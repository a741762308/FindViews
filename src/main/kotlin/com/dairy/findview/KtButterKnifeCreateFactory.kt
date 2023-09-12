package com.dairy.findview

import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

class KtButterKnifeCreateFactory(@NotNull resIdBeans: MutableList<ResBean>, @NotNull files: PsiFile, @NotNull ktClass: KtClass) :
    KtViewCreateFactory(resIdBeans, files, ktClass) {


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
            val holderField = StringBuilder()
            for (resBean in resBeans) {
                if (resBean.isChecked) {
                    holderField.append(resBean.adapterKotlinButterKnifeProperty)
                    holderField.append("\n")

                }
            }
            if (holderClass == null) {
                holderField.append("\ninit {\nButterKnife.bind(this,view)\n}\n")
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
                        holderMethod.append("constructor(itemView: View) : super(itemView) {\nButterKnife.bind(this,view)\n")
                    } else {
                        holderMethod.append("constructor(itemView: View){\nButterKnife.bind(this,view)\n")
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
                        ktFactory.createProperty("lateinit var ${it.fieldName}: ${it.name}")
                    }
                    .forEach { body.addBefore(ktFactory.createNewLine(), body.addAfter(it, e)) }

                if (methodSize <= 0) {
                    holderClass.addBefore(
                        ktFactory.createExpression("init {\nButterKnife.bind(this,view)\n}\n"),
                        body.rBrace
                    )
                    return
                }
                holderMethod.append("ButterKnife.bind(this,${params ?: "view"})")
                methodBody?.delete()
                holderMethod.append("}")
                if (findMethod == null) {
                    holderClass.addBefore(
                        ktFactory.createSecondaryConstructor(holderMethod.toString()),
                        body.rBrace
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
                    val field = ktFactory.createProperty(resBean.kotlinButterKnifeProperty)
                    ktBody.addBefore(ktFactory.createNewLine(), ktBody.addAfter(field, e))
                }
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }


    override fun generateFindViewById() {

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
                            } else if (statement.text.contains("ButterKnife.bind(this)")) {
                                return
                            }
                        }

                        if (setContentView != null) {

                            if (Config.get().isButterKnifeUnBind && ktClass.findPropertyByName("mUnBinder") == null) {
                                val e = ktClass.getProperties().lastOrNull() ?: ktBody.lBrace
                                ktBody.addBefore(
                                    ktFactory.createNewLine(),
                                    ktBody.addAfter(
                                        ktFactory.createProperty("private lateinit var mUnBinder: Unbinder"),
                                        e
                                    )
                                )
                            }
                            val bind = if (Config.get().isButterKnifeUnBind) {
                                "mUnBinder = ButterKnife.bind(this)"
                            } else {
                                "ButterKnife.bind(this)"
                            }
                            val e = body.addAfter(ktFactory.createExpression(bind), setContentView)
                            body.addBefore(ktFactory.createNewLine(), e)

                            if (Config.get().isButterKnifeUnBind) {
                                addUnbindStatement()
                            }
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
                            if (statement.text.contains("ButterKnife.bind")) {
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

                            if (Config.get().isButterKnifeUnBind && ktClass.findPropertyByName("mUnBinder") == null) {
                                val p = ktClass.getProperties().lastOrNull() ?: ktBody.lBrace
                                ktBody.addBefore(
                                    ktFactory.createNewLine(),
                                    ktBody.addAfter(
                                        ktFactory.createProperty("private lateinit var mUnBinder: Unbinder"),
                                        p
                                    )
                                )
                            }
                            val bind = if (Config.get().isButterKnifeUnBind) {
                                "mUnBinder = ButterKnife.bind(this, mRootView)"
                            } else {
                                "ButterKnife.bind(this, mRootView)"
                            }

                            e = body.addBefore(ktFactory.createExpression(bind), e)
                            body.addBefore(ktFactory.createNewLine(), e)
                            body.addAfter(
                                ktFactory.createExpression(text.replace("return", "mRootView =")),
                                body.lBrace
                            )
                        } else {
                            body.addAfter(
                                ktFactory.createNewLine(),
                                body.addBefore(ktFactory.createExpression("ButterKnife.bind(this, view)"), returnView)
                            )
                        }
                        if (Config.get().isButterKnifeUnBind) {
                            addUnbindStatement()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    private fun addUnbindStatement() {
        val function =
            if (mIsActivity) {
                Utils.findFunctionByName(ktClass, "onDestroy")
            } else {
                Utils.findFunctionByName(ktClass, "onDestroyView")
            }
        if (function != null) {
            val body = function.bodyExpression as? KtBlockExpression
            if (body != null) {
                for (statement in body.statements) {
                    if (statement.text.contains(".unbind()")) {
                        return
                    }
                }
                val last = body.statements.last()
                body.addAfter(ktFactory.createExpression("mUnBinder.unbind()"), last)
            }
        } else {
            val methodString = StringBuilder()

            methodString.append("override fun onDestroy() {\n")
            methodString.append("super.onDestroy()\n")
            methodString.append("mUnBinder.unbind()\n")
            methodString.append("}")
            ktBody.addAfter(
                ktFactory.createNewLine(),
                ktClass.addBefore(ktFactory.createFunction(methodString.toString()), ktBody.rBrace)
            )
        }
    }
}