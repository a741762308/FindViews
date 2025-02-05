package com.dairy.findview

import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.*

/**
 * Created by admin on 2023/7/26.
 */
class KtViewMergeFactory(
    @NotNull resIdBeans: MutableList<ResBean>,
    @NotNull files: PsiFile,
    @NotNull private val layoutFile: PsiFile,
    @NotNull ktClass: KtClass
) : KtViewCreateFactory(resIdBeans, files, ktClass) {

    private val propertyMap = HashMap<String, String>()
    override fun executeBefore() {
        resBeans.forEach {
            propertyMap[it.fieldName] = it.getFieldName(2)
        }
    }

    override fun generateAdapter() {
        try {
            val recycler = isRecyclerViewAdapter()
            var holderClass = getAdapterHolder(recycler)
            val binding = Utils.getViewBinding(layoutFile)
            if (holderClass == null) {
                val holdClass: String = if (recycler) {
                    "inner class ViewHolder(val binding: $binding) : RecyclerView.ViewHolder(binding.root) {}"
                } else {
                    "class ViewHolder(val binding: $binding) {}"
                }
                holderClass = ktFactory.createClass(holdClass)
                ktBody.addBefore(holderClass, ktBody.rBrace)
            } else {
                //删除成员属性
                holderClass.getProperties().forEach {
                    if (propertyMap[it.name] != null) {
                        it.delete()
                    }
                }

                var findMethods =
                    Utils.allConstructors(holderClass)
                        .filter { it.getValueParameterList()?.parameters?.isEmpty() == false }
                if (recycler) {
                    findMethods = findMethods.filter { it is KtPrimaryConstructor || it.getText().contains("super(") }
                }
                val findMethod = findMethods.firstOrNull()
                val methodBody: KtBlockExpression? = findMethod?.getBodyExpression()
                val body = holderClass.getOrCreateBody()
                if (findMethod == null) {
                    if (recycler) {
                        holderClass.addBefore(
                            ktFactory.createPrimaryConstructor("constructor(val binding: $binding) : super(binding.root)"),
                            body.rBrace
                        )
                    } else {
                        holderClass.addBefore(
                            ktFactory.createSecondaryConstructor("constructor(val binding: $binding)"),
                            body.rBrace
                        )
                    }
                } else {
                    //init 代码块
                    holderClass.getAnonymousInitializers().forEach {
                        //Utils.showNotification(ktClass.project, MessageType.ERROR, it.body?.text ?: "init{}")
                        (it.body as? KtBlockExpression)?.statements?.forEach {
                            it.mergePropertyName(isAdapter = true)
                        }
                    }
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
                                holdClass.replace(start - 1, end + 1, "(binding.root)").toString().let {
                                    //Utils.showNotification(ktClass.project, MessageType.ERROR, it)
                                    holderClass.replace(ktFactory.createClass(it))
                                }

                            }
                        } else {
                            //删除find语句保留其他语句
                            val functionBody = StringBuilder()
                            methodBody?.statements?.forEach {
                                if (!it.text.contains("ButterKnife.")) {
                                    if (!it.text.contains("findViewById")) {
                                        functionBody.append(it.text).append("\n")
                                    } else if (it.text.contains("=")) {
                                        val name = it.text.substring(0, it.text.indexOf("="))
                                        if (!name.contains("val") && !name.contains("var")) {
                                            functionBody.append(it.replacePropertyName())
                                                .append("\n")
                                        }
                                    }
                                }
                            }
                            val funString = functionBody.toString()
                            if (funString.isNotEmpty()) {
                                //Utils.showNotification(ktClass.project, MessageType.ERROR, funString.toString())
                                findMethod.replace(ktFactory.createSecondaryConstructor("constructor(val binding: $binding):super(binding.root){$funString}"))
                            } else {
                                findMethod.replace(ktFactory.createSecondaryConstructor("constructor(val binding: $binding):super(binding.root)"))
                            }
                        }
                    }
                }
            }
            if (recycler) {
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
                val body = onCreate.bodyExpression as? KtBlockExpression
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
                                ?: find { it.text.contains(createBinding.text) } ?: body.addBefore(
                                    createBinding,
                                    body.addBefore(ktFactory.createNewLine(), returnView)
                                )
                        }
                        returnView.replace(ktFactory.createExpression("return ViewHolder(binding)"))
                    }

                }
                val onBind = Utils.findFunctionByName(ktClass, "onBindViewHolder")
                if (onBind == null) {
                    ktClass.addBefore(
                        ktFactory.createFunction("override fun onBindViewHolder(holder: ViewHolder, position: Int) {}"),
                        ktBody.rBrace
                    )
                } else {
                    //替换属性为binding
                    (onBind.bodyExpression as? KtBlockExpression)?.statements?.replaceAdapterPropertyName()
                }
            } else {
                val getView = Utils.findFunctionByName(ktClass, "getView")
                if (getView == null) {
                    ktClass.addBefore(
                        ktFactory.createFunction("override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {}"),
                        ktBody.rBrace
                    )
                } else {
                    //替换属性为binding
                    val body = getView.bodyExpression as? KtBlockExpression
                    body?.statements?.forEach {
                        it.mergeBaseAdapterPropertyName(body, binding)
                    }
                }
            }

            //Utils.showNotification(ktClass.project, MessageType.ERROR, replaceProperty.toString())

        } catch (t: Throwable) {
            t.printStackTrace()
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    override fun generateFields() {
        try {
            //添加binding
            val binding = Utils.getViewBinding(layoutFile)
            val bindingString = "$binding.inflate(LayoutInflater.from(this))"
            var bindingExist = false
            //删掉属性
            ktClass.getProperties().forEach {
                if (propertyMap[it.name] != null || it.name == "mRootView" || it.typeReference?.text == "Unbinder") {
                    it.delete()
                } else {
                    if (!bindingExist) {
                        bindingExist = if (mIsActivity) {
                            it.text.contains(bindingString)
                        } else {
                            it.text.contains(binding)
                        }
                    }
                }
            }
            if (!bindingExist) {
                if (mIsActivity) {
                    ktBody.addAfter(
                        ktFactory.createProperty("private val binding by lazy { $bindingString }"),
                        ktBody.lBrace
                    )
                } else {
                    ktBody.addAfter(
                        ktFactory.createProperty("private lateinit var binding: $binding"),
                        ktBody.lBrace
                    )
                }
            }

        } catch (t: Throwable) {
            t.printStackTrace()
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }

    }

    override fun generateFindViewById() {
        try {
            //view替换为binding
            if (mIsActivity) {
                val function = Utils.findFunctionByName(ktClass, "onCreate")
                if (function != null) {
                    val body: KtBlockExpression? = function.bodyExpression as? KtBlockExpression
                    body?.statements?.find {
                        it.text.contains("setContentView") &&
                                !it.text.contains("binding.root")
                    }
                        ?.replace(ktFactory.createExpression("setContentView(binding.root)"))
                }
            } else {
                val function = Utils.findFunctionByName(ktClass, "onCreateView")
                if (function != null) {
                    val binding = Utils.getViewBinding(layoutFile)
                    val bindingString = "$binding.inflate(inflater, container, false)"
                    val body = function.bodyExpression as? KtBlockExpression ?: return
                    body.statements.let {
                        val returnView = it.last()
                        if (returnView.text.contains(Utils.getXmlPath(layoutFile))) {
                            val e = returnView.replace(ktFactory.createExpression("return binding.root"))
                            it.find { s -> s.text.contains(bindingString) } ?: kotlin.run {
                                body.addBefore(ktFactory.createNewLine(), e)
                                body.addAfter(
                                    ktFactory.createExpression("binding = $bindingString"),
                                    body.lBrace
                                )
                            }
                            it.find { s -> s.text.contains("initViews") }?.run {
                                replace(ktFactory.createExpression("initViews(binding.root)"))
                            }
                        } else {
                            it.forEach { s ->
                                if (s.text.contains(Utils.getXmlPath(layoutFile))) {
                                    s.replace(ktFactory.createExpression("binding = $bindingString"))
                                } else if (s.text.contains("initViews")) {
                                    s.replace(ktFactory.createExpression("initViews(binding.root)"))
                                }
                            }
                            returnView.replace(ktFactory.createExpression("return binding.root"))
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }

    }

    override fun performFunction() {
        try {
            //替换属性为binding
            ktClass.declarations.filterIsInstance<KtFunction>()
                //去掉自定义布局
                .filter { it.bodyExpression?.text?.contains("R.layout") == false }
                .forEach { fuc ->
                    (fuc.bodyExpression as? KtBlockExpression)?.statements?.forEach {
                        it.mergePropertyName()
                    }
                }
        } catch (t: Throwable) {
            t.printStackTrace()
            Utils.showNotification(ktClass.project, MessageType.ERROR, t.message)
        }
    }

    private fun List<KtExpression>?.replaceAdapterPropertyName() {
        this?.forEach {
            it.replaceAdapterPropertyName()
        }
    }

    private fun KtExpression.mergeBaseAdapterPropertyName(body: KtExpression, binding: String) {
        if (text.contains(Utils.getXmlPath(layoutFile))) {
            if (this is KtIfExpression) {
                val ifBlock = then ?: return
                if (ifBlock.text.contains(Utils.getXmlPath(layoutFile))) {
                    if (ifBlock is KtBlockExpression) {
                        ifBlock.statements.forEach {
                            it.mergeBaseAdapterPropertyName(body, binding)
                        }
                        //Utils.showNotification(ktClass.project, MessageType.ERROR, ifBlock.text)
                    }
                }
            } else {
                val bindingString = "$binding.inflate(LayoutInflater.from(parent.context), parent, false)"
                val createBinding = ktFactory.createProperty("val binding = $bindingString")
                body.addBefore(createBinding, body.addBefore(ktFactory.createNewLine(), this))
                replace(ktFactory.createExpression("view = binding.root"))
                //Utils.showNotification(ktClass.project, MessageType.ERROR, text)
            }
        } else if (text.contains("ViewHolder(")) {
            //previousStatement()?.replace(ktFactory.createExpression("view = binding.root"))
            replace(ktFactory.createExpression("holder = ViewHolder(binding)"))
        } else if (text.contains("findViewById")) {
            delete()
        } else {
            replaceAdapterPropertyName()
        }
    }

    private fun KtExpression.mergePropertyName(isAdapter: Boolean = false) {
        if (this is KtIfExpression) {
            condition?.replacePropertyName()
            then?.replacePropertyName()
            `else`?.replacePropertyName()
        } else if (this is KtWhenExpression) {
            //条件
            subjectExpression?.replacePropertyName()
            //case
            entries.forEach {
                it.conditions.forEach { c ->
                    (c as? KtWhenConditionWithExpression)?.expression?.replacePropertyName()
                }
                it.expression?.replacePropertyName()
            }
            //default
            elseExpression?.replacePropertyName()
//        } else if (this is KtLoopExpression) {
//            body?.replacePropertyName()
//        } else if (this is KtCallExpression) {
//            Utils.showNotification(ktClass.project, MessageType.ERROR,  calleeExpression?.text?:"")
//            valueArguments.forEach {
//                it.getArgumentExpression()?.replacePropertyName()
//            }
//            typeArguments.forEach {
//            }
//            lambdaArguments.forEach {
//                Utils.showNotification(ktClass.project, MessageType.ERROR,  it.getLambdaExpression()?.text?:"")
//            }
        } else if (text.contains("findViewById")) {
            //删除 findViewById
            if (isAdapter) {
                if (isAdapterField()) {
                    delete()
                } else {
                    replaceFindById()
                }
            } else if (text.contains("=")) {
                if (text.indexOf("=") <= text.indexOf("findViewById")) {
                    delete()
                } else {
                    replaceFindById()
                }
            }
        } else if (text.contains("@BindView")
            || text.contains("ButterKnife.")
            || text.contains("UnBinder.")
        ) {
            //删除 ButterKnife
            delete()
        } else {
            replacePropertyName()
        }
    }

    private fun KtExpression.isAdapterField(): Boolean {
        for (filed in propertyMap.keys) {
            if (text.contains(filed)) {
                return true
            }
        }
        return false
    }

    private fun KtExpression.replaceFindById() {
        if (text.contains("findViewById")) {
            resBeans.forEach {
                if (text.contains(it.fullId) && it.isChecked) {
                    var string = "findViewById<${it.name}>(${it.fullId})"
                    if (!text.contains(string)) {
                        string = "findViewById(${it.fullId})"
                    }
                    if (text.contains(".findViewById")) {
                        if (text.contains("view.")) {
                            string = "view.$string"
                        } else if (text.contains("itemView.")) {
                            string = "itemView.$string"
                        }
                    }
                    if (text.contains(string)) {
                        val statement = text.replace(string, "binding.${it.getFieldName(2)}")
                        replace(ktFactory.createExpression(statement))
                    } else {
                        //Utils.showNotification(ktClass.project, MessageType.INFO, text)
                    }
                    return@replaceFindById
                }
            }
        }
    }

    private fun KtExpression.replacePropertyName() {
        //Utils.showNotification(ktClass.project, MessageType.ERROR, this.toString())
        if (this is KtBlockExpression) {
            statements.forEach {
                it.replacePropertyName()
            }
        } else {
            var replace = text
            var dot: Int
            propertyMap.keys.forEach {
                dot = text.indexOf(it)
                val name = propertyMap[it]
                while (name != null) {
                    if (dot != -1 && (text.getOrNull(dot + it.length) == '.'
                                || text.getOrNull(dot + it.length) == ')')
                    ) {
                        replace = replace.replace(it, "binding.$name", false)
                        val _dot = text.substring(dot + it.length).indexOf(it)
                        if (_dot != -1) {
                            dot += it.length + _dot
                        } else {
                            dot = _dot
                        }
                    } else {
                        break
                    }
                }
            }
            replace(ktFactory.createExpression(replace))
        }
    }

    private fun KtExpression.replaceAdapterPropertyName() {
        if (text.contains("holder.")) {
            val dot1 = mutableListOf<Int>()
            val dot2 = mutableListOf<Int>()
            var dot = text.indexOf("holder.")
            var _dot = 0
            while (true) {
                if (dot != -1) {
                    dot += _dot + 7
                    dot1.add(dot)
                    _dot = dot + text.substring(dot).indexOf(".")
                    dot2.add(_dot)
                    dot = text.substring(_dot).indexOf("holder.")
                } else {
                    break
                }
            }
            var replace = text
            for (index in 0 until dot1.size) {
                val name = text.substring(dot1[index], dot2[index])
                //Utils.showNotification(ktClass.project, MessageType.ERROR, name)
                propertyMap[name]?.let { id ->
                    //Utils.showNotification(ktClass.project, MessageType.ERROR, id)
                    replace = replace.replace(name, "binding.$id", false)
                }
            }
            //Utils.showNotification(ktClass.project, MessageType.ERROR, replace)
            replace(ktFactory.createExpression(replace))
        }
    }
}