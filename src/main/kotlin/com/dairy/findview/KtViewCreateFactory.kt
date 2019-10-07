package com.dairy.findview

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.*

/**
 * Created by admin on 2019/7/26.
 */
class KtViewCreateFactory(@NotNull resIdBeans: MutableList<ResBean>, @NotNull files: PsiFile, @NotNull private val ktClass: KtClass) :
    BaseViewCreateFactory(resIdBeans, files) {

    private val ktFactory: KtPsiFactory = KtPsiFactory(files.project)
    private val ktBody: KtClassBody = ktClass.getBody() ?: throw RuntimeException("kotlin class body not found")
    private val mIsActivity: Boolean = Utils.isKotlinActivity(psiFile, ktClass)

    private val mRunnable = Runnable {
        try {
            if (Utils.isKotlinAdapter(psiFile, ktClass) || isAdapterType) {
                //适配器
                generateAdapter()
            } else {
                //生成成员变量
                generateFields()
                //生成方法
                generateFindViewById()
                //调用方法
                performFunction()
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    override fun execute() {
        WriteCommandAction.runWriteCommandAction(ktClass.project, mRunnable)
    }

    private fun isRecyclerViewAdapter(): Boolean {
        val declarations = ktClass.declarations
        return isRecyclerAdapter || declarations.find { it.name.equals("onCreateViewHolder") } != null
                && declarations.find { it.name.equals("onBindViewHolder") } != null
    }

    override fun generateAdapter() {
        try {
            val recycler = isRecyclerViewAdapter()
            var holderClass: KtClass? = null
            if (recycler) {
                for (inner in ktClass.declarations.filter { it is KtClass }) {
                    if (Utils.isKotlinFitClass(
                            psiFile,
                            inner as KtClass,
                            "android.support.v7.widget.RecyclerView.ViewHolder"
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
            val holderField = StringBuilder()
            for (resBean in resBeans) {
                if (resBean.isChecked) {
                    holderField.append(resBean.kotlinAdapterProperty)
                    if (holderClass == null) {
                        val findId = "view.findViewById(" + resBean.fullId + ")\n"
                        holderField.append(" = ")
                            .append(findId)
                    }


                }
            }
            if (holderClass == null) {
                val holdClass: String
                if (recycler) {
                    holdClass = "inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {$holderField}"
                } else {
                    holdClass = " inner class ViewHolder(view: View) {$holderField}"
                }
                holderClass = ktFactory.createClass(holdClass)
                ktBody.addBefore(holderClass, ktBody.rBrace)
            } else {
                val holderMethod = StringBuilder()
                var findMethods =
                    Utils.allConstructors(holderClass)
                        .filter { it.getValueParameterList()?.parameters?.isEmpty() == false }
                if (recycler) {
                    findMethods = findMethods.filter { it is KtPrimaryConstructor || it.getText().contains("super(") }
                }
                val findMethod = findMethods.firstOrNull()
                val methodBody: KtBlockExpression? = findMethod?.getBodyExpression()
                var params: String? = null
                if (findMethod == null) {
                    if (recycler) {
                        holderMethod.append("constructor(itemView: View) : super(itemView) {")
                    } else {
                        holderMethod.append("constructor(itemView: View){")
                    }
                } else {
                    params = findMethod.getValueParameterList()?.parameters?.get(0)?.name
                    if (methodBody != null && !methodBody.text.isEmpty()) {
                        holderMethod.append(methodBody.text)
                        holderMethod.deleteCharAt(holderMethod.length - 1)
                    }
                }
                val methodSize = holderMethod.length

                val body = holderClass.getBody()!!
                //最后一个成员变量
                val p = holderClass.getProperties().lastOrNull()
                //所有成员变量
                val oldFiled = holderClass.getProperties().map { it.name }
                //左括号
                val e = p ?: body.lBrace
                resBeans
                    .filter { it.isChecked && !oldFiled.contains(it.fieldName) }
                    .map {
                        if (methodSize > 0) {
                            ktFactory.createProperty("val ${it.fieldName}: ${it.name}")
                        } else {//有构造方法
                            ktFactory.createProperty(it.getKotlinAdapterProperty(params ?: "view")
                            )
                        }
                    }
                    .forEach { body.addBefore(ktFactory.createNewLine(), body.addAfter(it, e)) }

                if (methodSize <= 0) {
                    return
                }
                for (resBean in resBeans) {
                    if (resBean.isChecked) {
                        val findId = "findViewById(" + resBean.fullId + ")"
                        if (methodBody == null || !methodBody.text.contains(findId)) {
                            holderMethod.append(resBean.getKotlinExpression("itemView"))
                            holderMethod.append("\n")
                        }
                    }
                }
                methodBody?.delete()
                holderMethod.append("}")
                if (findMethod == null) {
                    holderClass.addBefore(
                        ktFactory.createSecondaryConstructor(holderMethod.toString()),
                        holderClass.getBody()?.rBrace
                    )
                } else {
                    findMethod.add(ktFactory.createExpression(holderMethod.toString()))
                }
            }

        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    override fun generateFields() {
        try {
            //最后一个成员变量
            val p = ktClass.getProperties().lastOrNull()
            //所有成员变量
            val oldFiled = ktClass.getProperties().map { it.name }
            //左括号
            val e = p ?: ktBody.lBrace
            for (resBean in resBeans) {
                if (resBean.isChecked && !oldFiled.contains(resBean.fieldName)) {
                    val field = if (Config.get().isKotlinLazy && mIsActivity) {
                        ktFactory.createProperty(resBean.getKotlinLazyProperty(""))
                    } else {
                        ktFactory.createProperty(resBean.kotlinProperty)
                    }
                    ktBody.addBefore(ktFactory.createNewLine(), ktBody.addAfter(field, e))
                }
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }

    }

    override fun generateFindViewById() {
        try {
            val createFunction = StringBuilder()
            if (mIsActivity) {
                if (Config.get().isKotlinLazy) {
                    return
                }
                createFunction.append("private fun initViews() {}")
            } else {
                createFunction.append("private fun initViews(view: View) {}")
            }
            val functions = Utils.findFunctionByName(ktClass, "initViews")
            if (functions == null) {
                val p = ktClass.getProperties().lastOrNull()
                val e = p ?: ktBody.rBrace
                if (p != null) {
                    ktBody.addBefore(
                        ktFactory.createNewLine(),
                        ktClass.addAfter(ktFactory.createFunction(createFunction.toString()), e)
                    )
                } else {
                    ktBody.addAfter(
                        ktFactory.createNewLine(),
                        ktClass.addBefore(ktFactory.createFunction(createFunction.toString()), e)
                    )
                }
            }
            val findViewsFunction = Utils.findFunctionByName(ktClass, "initViews")
            val functionBody = findViewsFunction.bodyExpression as KtBlockExpression
            for (resBean in resBeans) {
                val findId = "findViewById(" + resBean.fullId + ")"
                if (resBean.isChecked && !functionBody.text.contains(findId)) {
                    val block = StringBuilder()
                    val e = functionBody.rBrace;
                    block.append(resBean.fieldName).append(" = ")
                    if (!mIsActivity) {
                        block.append("view.")
                    }
                    block.append(findId)
                    functionBody.addAfter(
                        ktFactory.createNewLine(),
                        functionBody.addBefore(ktFactory.createExpression(block.toString()), e)
                    )
                }
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    override fun performFunction() {
        try {
            val function: KtNamedDeclaration?
            val body: KtBlockExpression?
            if (mIsActivity) {
                if (Config.get().isKotlinLazy) {
                    return
                }
                function = Utils.findFunctionByName(ktClass, "onCreate")
                if (function != null) {
                    var setContentView: KtExpression? = null
                    body = function.bodyExpression as? KtBlockExpression
                    if (body != null) {
                        for (statement in body.statements) {
                            if (statement.text.contains("setContentView")) {
                                setContentView = statement
                            } else if (statement.text.contains("initViews")) {
                                return
                            }
                        }
                        if (setContentView != null) {
                            val e = body.addAfter(ktFactory.createExpression("initViews()"), setContentView)
                            body.addBefore(ktFactory.createNewLine(), e)
                        }
                    }
                }
            } else {
                function = Utils.findFunctionByName(ktClass, "onCreateView")
                if (function != null) {
                    val returnView: KtExpression
                    body = function.bodyExpression as? KtBlockExpression
                    if (body != null) {
                        val statements = body.statements
                        for (statement in statements) {
                            if (statement.text.contains("initViews")) {
                                return
                            }
                        }
                        returnView = statements.last()
                        val index = returnView.text.indexOf("inflater.inflate(")
                        if (index != -1) {
                            //创建临时变量失败，暂时没有更好的方法，先使用全局变量
                            val p = ktClass.getProperties().last()
                            if (!p.text.contains("mRootView")) {
                                val filed = ktFactory.createProperty("private lateinit var mRootView: View")
                                ktBody.addBefore(ktFactory.createNewLine(), ktBody.addAfter(filed, p))
                            }
                            val text = returnView.text
                            var e = returnView.replace(ktFactory.createExpression("return mRootView"))
                            e = body.addBefore(ktFactory.createNewLine(), e)
                            e = body.addBefore(ktFactory.createExpression("initViews(mRootView)"), e)
                            body.addBefore(ktFactory.createNewLine(), e)
                            body.addAfter(
                                ktFactory.createExpression(text.replace("return", "mRootView =")),
                                body.lBrace
                            )
                        } else {
                            body.addAfter(
                                ktFactory.createNewLine(),
                                body.addBefore(ktFactory.createExpression("initViews(view)"), returnView)
                            )
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }
}