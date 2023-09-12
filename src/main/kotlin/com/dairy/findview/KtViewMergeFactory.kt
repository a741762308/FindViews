package com.dairy.findview

import android.view.View
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.idea.quickfix.expectactual.getTypeDescription
import org.jetbrains.kotlin.idea.refactoring.pullUp.getSuperTypeEntryByDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

/**
 * Created by admin on 2023/7/26.
 */
class KtViewMergeFactory(
    @NotNull resIdBeans: MutableList<ResBean>,
    @NotNull files: PsiFile,
    @NotNull private val layoutFile: PsiFile,
    @NotNull ktClass: KtClass
) : KtViewCreateFactory(resIdBeans, files, ktClass) {

    override fun generateAdapter() {
        try {
            val recycler = isRecyclerViewAdapter()
            var holderClass: KtClass? = null
            if (recycler) {
                for (inner in ktClass.declarations.filter { it is KtClass }) {
                    if (Utils.isKotlinRecyclerHolder(
                            psiFile,
                            inner as KtClass
                        )
                    ) {
                        holderClass = inner
                        break
                    }
                }
            } else {
                for (inner in ktClass.declarations.filter { it is KtClass }) {
                    if (inner.name != null && inner.name!!.contains("ViewHolder")) {
                        holderClass = inner as KtClass
                        break
                    }
                }
            }
            val binding = Utils.getViewBinding(layoutFile)
            if (holderClass == null) {
                val holdClass: String = if (recycler) {
                    "inner class ViewHolder(binding: $binding) : RecyclerView.ViewHolder(binding.root) {}"
                } else {
                    " inner class ViewHolder(binding: $binding) {}"
                }
                holderClass = ktFactory.createClass(holdClass)
                ktBody.addBefore(holderClass, ktBody.rBrace)
            } else {
                var findMethods =
                    Utils.allConstructors(holderClass)
                        .filter { it.getValueParameterList()?.parameters?.isEmpty() == false }
                if (recycler) {
                    findMethods = findMethods.filter { it is KtPrimaryConstructor || it.getText().contains("super(") }
                }
                val findMethod = findMethods.firstOrNull()
                val methodBody: KtBlockExpression? = findMethod?.getBodyExpression()
                val body = holderClass.getBody()!!
                if (findMethod == null) {
                    if (recycler) {
                        holderClass.addBefore(
                            ktFactory.createSecondaryConstructor("constructor(binding: $binding) : super(binding.root)"),
                            body.rBrace
                        )
                    } else {
                        holderClass.addBefore(
                            ktFactory.createSecondaryConstructor("constructor(binding: $binding)"),
                            body.rBrace
                        )
                    }
                } else {
                    val params = findMethod.getValueParameterList()?.parameters?.firstOrNull()
                    if (params != null && (params.text.contains(":View") || params.text.contains(": View"))) {
                        if (findMethod is KtPrimaryConstructor) {
                            params.replace(ktFactory.createProperty("val binding: $binding"))
                            val holdClass = StringBuilder(holderClass.text)
                            val ss = "RecyclerView.ViewHolder("
                            var start = holdClass.indexOf(ss)
                            if (start > 0) {
                                start += ss.length
                                var end = holdClass.length
                                for (i in start until holdClass.length) {
                                    if (holdClass[i] == ')') {
                                        end = i
                                        break
                                    }
                                }
                                holderClass.replace(
                                    ktFactory.createClass(
                                        holdClass.replace(start - 1, end + 1, "(binding.root)").toString()
                                    )
                                )
                            }
                        } else {
                            val functionBody = StringBuilder()
                            findMethod.getBodyExpression()?.statements?.forEach {
                                if (!it.text.contains("findViewById")) {
                                    functionBody.append(it.text)
                                    functionBody.append("\n")
                                }
                            }
                            findMethod.replace(ktFactory.createSecondaryConstructor("constructor(binding: $binding):super(binding.root)"))
                            val funString = functionBody.toString()
                            Utils.showNotification(ktClass.project, MessageType.ERROR, funString)
                            if (funString.isNotEmpty()) {
//                                findMethod.getBodyExpression()?.add(ktFactory.createExpression(funString))
                            }
                        }
                    }

                    if (methodBody != null && !methodBody.text.isEmpty()) {

                    }
                }
            }

            var onCreate = Utils.findFunctionByName(ktClass, "onCreateViewHolder")
            val bindingString = "$binding.inflate(LayoutInflater.from(parent.context), parent, false)"
            val createBinding = ktFactory.createProperty("val binding = $bindingString")
            if (onCreate == null) {
                ktClass.addAfter(
                    ktFactory.createFunction("override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {}"),
                    ktBody.lBrace
                )
            }
            onCreate = Utils.findFunctionByName(ktClass, "onCreateViewHolder")
            val body = onCreate.bodyBlockExpression
            body?.statements?.run {
                if (isEmpty()) {
                    body.addAfter(createBinding, body.addAfter(ktFactory.createNewLine(), body.lBrace))
                    body.addBefore(ktFactory.createExpression("return ViewHolder(binding)"), body.rBrace)
                } else {
                    val returnView = last()
                    if (returnView.text.contains(Utils.getXmlPath(layoutFile))) {
                        // val binding = $bindingString
                        body.addBefore(createBinding, body.addBefore(ktFactory.createNewLine(), returnView))
                    } else {
                        find { it.text.contains(Utils.getXmlPath(layoutFile)) }?.replace(createBinding)
                        find { it.text.contains(createBinding.text) } ?: body.addBefore(
                            createBinding,
                            body.addBefore(ktFactory.createNewLine(), returnView)
                        )
                    }
                    returnView.replace(ktFactory.createExpression("return ViewHolder(binding)"))
                }

            }

            holderClass.getProperties().forEach {
                if (it.text.contains("findViewById")) {
                    it.delete()
                }
            }

        } catch (t: Throwable) {
            t.printStackTrace()
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    override fun generateFields() {

    }

    override fun generateFindViewById() {

    }

    override fun performFunction() {

    }
}