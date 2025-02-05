package com.dairy.findview;

import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2023/7/26.
 */
public class JavaViewMergeFactory extends JavaViewCreateFactory {

    private final Map<String, String> mFieldMap = new HashMap<>();
    private final PsiFile layoutFile;

    public JavaViewMergeFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, @NotNull PsiFile layoutFile, @NotNull PsiClass psiClass) {
        super(resIdBeans, files, psiClass);
        this.layoutFile = layoutFile;
    }

    @Override
    protected void executeBefore() {
        for (ResBean bean : resBeans) {
            mFieldMap.put(bean.getFieldName(), bean.getFieldName(2));
        }
    }

    @Override
    protected void generateAdapter() {
        try {
            boolean recycler = isRecyclerViewAdapter();
            PsiClass holderClass = getAdapterHolder(recycler);
            String binding = Utils.getViewBinding(layoutFile);
            StringBuilder holderMethod = new StringBuilder();
            StringBuilder holderField = new StringBuilder();
            holderField.append(CodeConstant.getAdapterJavaFiled(binding, "mBinding"));
            PsiMethod findMethod = null;
            PsiCodeBlock methodBody = null;

            if (holderClass != null && holderClass.getConstructors().length != 0) {
                findMethod = holderClass.getConstructors()[0];
            }
            if (holderClass != null) {
                for (PsiField field : holderClass.getAllFields()) {
                    if (mFieldMap.get(field.getName()) != null) {
                        field.delete();
                    }
                }
            }
            holderMethod.append("public ")
                    .append(holderClass == null ? "ViewHolder" : holderClass.getName())
                    .append("(")
                    .append(binding)
                    .append(" binding) {");

            if (findMethod != null) {
                methodBody = findMethod.getBody();
            }
            if (recycler && (methodBody == null || !methodBody.getText().contains("super(binding"))) {
                holderMethod.append("super(binding.getRoot());\n");
            }
            String bindingS = "this.mBinding = binding;";
            if (methodBody == null || !methodBody.getText().contains(bindingS)) {
                holderMethod.append(bindingS);
            }
            if (methodBody != null) {
                for (PsiStatement statement : methodBody.getStatements()) {
                    if (!(statement.getText().contains("findViewById") && isAdapterField(statement))
                            && !statement.getText().contains("super")
                            && !statement.getText().contains("ButterKnife.")) {
                        holderMethod.append(replacePropertyName(statement.getText())).append("\n");
                    }
                }
            }
            if (findMethod != null) {
                findMethod.delete();
            }

            holderMethod.append("}");
            holderField.append(holderMethod);

            if (holderClass == null) {
                if (recycler) {
                    holderField.insert(0, "class ViewHolder extends RecyclerView.ViewHolder {");
                    holderField.append("}");
                    holderClass = factory.createClassFromText(holderField.toString(), psiClass);
                    holderClass = holderClass.getInnerClasses()[0];
                } else {
                    holderClass = factory.createClassFromText(holderField.toString(), psiClass);
                    holderClass.setName("ViewHolder");
                    changeHolderModifier(holderClass);
                }
                psiClass.add(holderClass);
            } else {
                changeHolderModifier(holderClass);
                PsiElementFactory holderFactory = JavaPsiFacade.getElementFactory(holderClass.getProject());
                if (holderClass.findFieldByName("mBinding", false) == null) {
                    holderClass.add(holderFactory.createFieldFromText(CodeConstant.getAdapterJavaFiled(binding, "mBinding"), holderClass));
                }
                holderClass.add(holderFactory.createMethodFromText(holderMethod.toString(), holderClass));
            }

            if (recycler) {
                PsiMethod[] onCreateViewHolder = psiClass.findMethodsByName("onCreateViewHolder", false);
                String bindingString = binding + " binding = " + binding + ".inflate(LayoutInflater.from(parent.getContext()),parent,false);";
                if (onCreateViewHolder.length > 0) {
                    PsiMethod onCreate = onCreateViewHolder[0];
                    PsiCodeBlock body = onCreate.getBody();
                    if (body != null) {
                        if (!body.isEmpty()) {
                            PsiStatement[] statements = body.getStatements();
                            PsiStatement last = statements[statements.length - 1];
                            //Utils.showNotification(psiFile.getProject(), MessageType.ERROR, last.getText());
                            if (last.getText().contains(Utils.getXmlPath(layoutFile))) {
                                body.addBefore(factory.createStatementFromText(bindingString, psiClass), last);
                            } else {
                                for (PsiStatement statement : statements) {
                                    if (statement.getText().contains(Utils.getXmlPath(layoutFile))) {
                                        statement.replace(factory.createStatementFromText(bindingString, psiClass));
                                    }
                                }
                            }
                            if (last.getText().contains("return")) {
                                last.replace(factory.createStatementFromText("return new ViewHolder(binding);", psiClass));
                            }
                        } else {
                            body.addAfter(factory.createStatementFromText(bindingString, psiClass), body.getLBrace());
                            body.addBefore(factory.createStatementFromText("return new ViewHolder(binding);", psiClass), body.getRBrace());
                        }
                    } else {
                        onCreate.add(factory.createCodeBlockFromText("{\n" + bindingString + "\n" + "return new ViewHolder(binding);\n}", psiClass));
                    }
                } else {
                    String createMethod = "@NonNull" + "\n" +
                            "@Override" + "\n" +
                            "public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {" + "\n" +
                            bindingString + "\n" +
                            "return new ViewHolder(binding);" + "\n" +
                            "}";
                    psiClass.add(factory.createMethodFromText(createMethod, psiClass));
                }
                PsiMethod[] onBindViewHolder = psiClass.findMethodsByName("onBindViewHolder", false);
                if (onBindViewHolder.length > 0) {
                    PsiCodeBlock onBindBody = onBindViewHolder[0].getBody();
                    if (onBindBody != null) {
                        PsiStatement[] statements = onBindBody.getStatements();
                        for (PsiStatement statement : statements) {
                            if (statement.getText().contains("holder.")) {
                                statement.replace(factory.createStatementFromText(replaceAdapterPropertyName(statement.getText()), psiClass));
                            }
                        }
                    }
                } else {
                    String createMethod = "@Override" + "\n" +
                            "public void onBindViewHolder(@NonNull ViewHolder holder, int position) {" + "\n" +
                            "}";
                    psiClass.add(factory.createMethodFromText(createMethod, psiClass));
                }
            } else {
                PsiMethod[] getView = psiClass.findMethodsByName("getView", false);
                if (getView.length > 0) {
                    PsiCodeBlock getViewBody = getView[0].getBody();
                    if (getViewBody != null) {
                        PsiStatement[] statements = getViewBody.getStatements();
                        for (PsiStatement statement : statements) {
                            mergeBaseAdapterPropertyName(statement, getViewBody, binding);
                        }
                    }
                } else {
                    psiClass.add(factory.createMethodFromText("@Override\n" +
                                    "    public View getView(int position, View convertView, ViewGroup parent) {}"
                            , psiClass));
                }

            }

        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    private boolean isAdapterField(PsiStatement statement) {
        for (String filed : mFieldMap.keySet()) {
            if (statement.getText().contains(filed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void generateFields() {
        try {
            boolean bindingExist = false;
            String bindingType = Utils.getViewBinding(layoutFile);
            for (PsiField field : psiClass.getFields()) {
                if (mFieldMap.get(field.getName()) != null
                        || "mRootView".equals(field.getName())
                        || "Unbinder".equals(field.getType().getCanonicalText())) {
                    field.delete();
                } else {
                    if (!bindingExist) {
                        bindingExist = field.getText().contains(bindingType);
                    }
                }
            }
            if (!bindingExist) {
                PsiField field = factory.createFieldFromText("private " + bindingType + " mBinding;", psiClass);
                psiClass.add(field);
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    @Override
    protected void generateFindViewById() {
        try {
            String bindingType = Utils.getViewBinding(layoutFile);
            String bindingName = "mBinding";
            String bindingString;
            if (mIsActivity) {
                bindingString = bindingType + ".inflate(LayoutInflater.from(this));";
                PsiMethod[] methods = psiClass.findMethodsByName("onCreate", false);
                if (methods.length > 0) {
                    PsiMethod onCreate = methods[0];
                    PsiStatement setContentView = null;
                    boolean bindingExist = false;
                    if (onCreate.getBody() != null) {
                        for (PsiStatement statement : onCreate.getBody().getStatements()) {
                            if (statement.getText().contains("setContentView")) {
                                setContentView = statement;
                                break;
                            } else {
                                bindingExist = statement.getText().contains(bindingString);
                            }
                        }
                        if (setContentView != null && !setContentView.getText().contains("mBinding.getRoot()")) {
                            if (!bindingExist) {
                                onCreate.getBody().addBefore(factory.createStatementFromText(bindingName + " = " + bindingString, psiClass), setContentView);
                            }
                            setContentView.replace(factory.createStatementFromText("setContentView(mBinding.getRoot());", psiClass));
                        }
                    }
                }
            } else {
                PsiMethod[] methods = psiClass.findMethodsByName("onCreateView", false);
                bindingString = bindingType + ".inflate(inflater, container, false);";
                if (methods.length > 0) {
                    PsiMethod onCreateView = methods[0];
                    PsiStatement returnView;
                    if (onCreateView.getBody() != null) {
                        PsiStatement[] statements = onCreateView.getBody().getStatements();
                        int count = statements.length;
                        returnView = statements[count - 1];
                        if (returnView.getText().contains(Utils.getXmlPath(layoutFile))) {
                            if (!onCreateView.getBody().getText().contains(bindingString)) {
                                onCreateView.getBody().addBefore(factory.createStatementFromText(bindingName + " = " + bindingString, psiClass), returnView);
                            }
                        } else {
                            for (PsiStatement statement : statements) {
                                if (statement.getText().contains(Utils.getXmlPath(layoutFile))) {
                                    statement.replace(factory.createStatementFromText(bindingName + " = " + bindingString, psiClass));
                                } else if (statement.getText().contains("initViews")) {
                                    statement.replace(factory.createStatementFromText("initViews(mBinding.getRoot());", psiClass));
                                }
                            }
                        }
                        returnView.replace(factory.createStatementFromText("return mBinding.getRoot();", psiClass));
                    }
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    @Override
    protected void performFunction() {
        try {
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                PsiCodeBlock body = method.getBody();
                if (body != null) {
                    if (!body.getText().contains("R.layout")) {
                        PsiStatement[] statements = body.getStatements();
                        for (PsiStatement statement : statements) {
                            mergePropertyName(statement);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    private void mergePropertyName(PsiStatement statement) {
        if (statement instanceof PsiIfStatement) {
//            PsiIfStatement ifStatement = (PsiIfStatement) statement;
//            replacePropertyName(ifStatement.getThenBranch());
//            replacePropertyName(ifStatement.getElseBranch());
            replacePropertyName(statement);
        } else if (statement instanceof PsiSwitchStatement) {
            replacePropertyName(statement);
//        } else if (statement instanceof PsiLoopStatement) {
//
        } else if (statement.getText().contains("=findViewById")
                || statement.getText().contains("= findViewById")) {
            //删除 findViewById
            statement.delete();
        } else if (statement.getText().contains("@BindView")
                || statement.getText().contains("ButterKnife.")
                || statement.getText().contains("UnBinder.")) {
            //删除 ButterKnife
            statement.delete();
        } else {
            replacePropertyName(statement);
        }
    }

    private void replacePropertyName(PsiStatement statement) {
        if (statement == null) {
            return;
        }
        String text = statement.getText();
        String replace = replacePropertyName(text);
        if (!text.equals(replace)) {
            statement.replace(factory.createStatementFromText(replace, psiClass));
        }
    }

    private String replacePropertyName(String text) {
        String replace = text;
        int dot = -1;
        int _dot = -1;
        boolean findId = text.contains("findViewById");
        for (Map.Entry<String, String> entry : mFieldMap.entrySet()) {
            dot = text.indexOf(entry.getKey());
            while (entry.getValue() != null) {
                int length = dot + entry.getKey().length();
                if (dot != -1 && length < text.length() && text.charAt(length) == '.') {
                    replace = replace.replace(entry.getKey(), "mBinding." + entry.getValue());
                    _dot = text.substring(length).indexOf(entry.getKey());
                    if (_dot != -1) {
                        dot = length + _dot;
                    } else {
                        dot = _dot;
                    }
                } else {
                    break;
                }
            }
        }
        return replace;
    }

    private String replaceAdapterPropertyName(String text) {
        String replace = text;
        if (text.contains("holder.")) {
            List<Integer> dot1 = new ArrayList<>();
            List<Integer> dot2 = new ArrayList<>();
            int dot = text.indexOf("holder.");
            int _dot = 0;
            while (true) {
                if (dot != -1) {
                    dot += _dot + 7;
                    dot1.add(dot);
                    _dot = dot + text.substring(dot).indexOf(".");
                    dot2.add(_dot);
                    dot = text.substring(_dot).indexOf("holder.");
                } else {
                    break;
                }
            }
            for (int i = 0; i < dot1.size(); i++) {
                String name = text.substring(dot1.get(i), dot2.get(i));
                String filed = mFieldMap.get(name);
                if (filed != null) {
                    replace = replace.replace(name, "mBinding." + filed);
                }
            }
        }
        return replace;
    }

    private void mergeBaseAdapterPropertyName(PsiStatement statement, PsiCodeBlock body, String binding) {
        if (statement.getText().contains(Utils.getXmlPath(layoutFile))) {
            if (statement instanceof PsiIfStatement) {
                PsiStatement then = ((PsiIfStatement) statement).getThenBranch();
                if (then != null && then.getText().contains(Utils.getXmlPath(layoutFile))) {
                    if (then instanceof PsiBlockStatement) {
                        for (PsiStatement s : ((PsiBlockStatement) then).getCodeBlock().getStatements()) {
                            mergeBaseAdapterPropertyName(s, body, binding);
                        }
                    }
                }
            } else {
                body.addBefore(factory.createStatementFromText(binding + " binding = " + binding + ".inflate(LayoutInflater.from(parent.getContext()), parent, false);", psiClass), statement);
                statement.replace(factory.createStatementFromText("convertView = binding.getRoot();", psiClass));
            }
        } else if (statement.getText().contains("new ViewHolder") && !statement.getText().contains("ViewHolder(binding)")) {
            statement.replace(factory.createStatementFromText("holder = new ViewHolder(binding);", psiClass));
        } else if (statement.getText().contains("findViewById")) {
            statement.delete();
        } else if (statement.getText().contains("holder.")) {
            statement.replace(factory.createStatementFromText(replaceAdapterPropertyName(statement.getText()), psiClass));
        }
    }
}
