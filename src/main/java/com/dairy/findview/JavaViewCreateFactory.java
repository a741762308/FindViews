package com.dairy.findview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JavaViewCreateFactory extends BaseViewCreateFactory {

    private PsiElementFactory factory;
    private PsiClass psiClass;
    private boolean mIsActivity;

    public JavaViewCreateFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, @NotNull PsiClass psiClass, AdapterType type) {
        super(resIdBeans, files);
        this.psiClass = psiClass;
        mIsActivity = Utils.isJavaActivity(psiFile, psiClass);
        factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        mAdapterType = type.index;
    }

    public JavaViewCreateFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, @NotNull PsiClass psiClass) {
        this(resIdBeans, files, psiClass, AdapterType.ADAPTER_NONE);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (Utils.isJavaAdapter(psiFile, psiClass) || isAdapterType()) {
                    //适配器
                    generateAdapter();
                } else {
                    //生成成员变量
                    generateFields();
                    //生成方法
                    generateFindViewById();
                    //调用方法
                    performFunction();
                }
                formatCode();
            } catch (Throwable t) {
                Utils.showNotification(psiClass.getProject(), MessageType.ERROR, t.getMessage());
            }
        }
    };

    @Override
    public void execute() {
        if (psiFile != null) {
            WriteCommandAction.runWriteCommandAction(psiFile.getProject(), mRunnable);
        }
    }

    /**
     * 格式化
     */
    @Override
    protected void formatCode() {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        styleManager.optimizeImports(psiClass.getContainingFile());
        styleManager.shortenClassReferences(psiClass);
        new ReformatCodeProcessor(psiClass.getProject(), psiClass.getContainingFile(), null, false).runWithoutProgress();
    }

    /**
     * 变量
     */
    @Override
    protected void generateFields() {
        try {
            for (ResBean resBean : resBeans) {
                if (psiClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                    PsiField field = factory.createFieldFromText(resBean.getJavaFiled(), psiClass);
                    psiClass.add(field);
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    private boolean isRecyclerViewAdapter() {
        return isRecyclerAdapter() || psiClass.findMethodsByName("onCreateViewHolder", false).length != 0
                && psiClass.findMethodsByName("onBindViewHolder", false).length != 0;
    }

    /**
     * adapter
     */
    @Override
    protected void generateAdapter() {
        try {
            boolean recycler = isRecyclerViewAdapter();
            PsiClass holderClass = null;
            if (recycler) {
                for (PsiClass inner : psiClass.getAllInnerClasses()) {
                    if (Utils.isJavaFitClass(psiFile, inner, "android.support.v7.widget.RecyclerView.ViewHolder")) {
                        holderClass = inner;
                        break;
                    }
                }
            } else {
                for (PsiClass inner : psiClass.getAllInnerClasses()) {
                    if (inner.getName() != null && inner.getName().contains("ViewHolder")) {
                        holderClass = inner;
                        break;
                    }
                }
            }
            StringBuilder holderField = new StringBuilder();
            for (ResBean resBean : resBeans) {
                if (resBean.isChecked()) {
                    holderField.append("public ")
                            .append(resBean.getFullName())
                            .append(" ")
                            .append(resBean.getFieldName())
                            .append(";\n");
                }
            }
            StringBuilder holderMethod = new StringBuilder();

            PsiMethod findMethod = null;
            PsiCodeBlock methodBody = null;

            if (holderClass != null && holderClass.getConstructors().length != 0) {
                findMethod = holderClass.getConstructors()[0];
            }
            if (findMethod == null) {
                holderMethod.append("public ")
                        .append(holderClass == null ? "ViewHolder" : holderClass.getName())
                        .append(" (View itemView) {");
            } else {
                methodBody = findMethod.getBody();
                if (methodBody != null) {
                    holderMethod.append(methodBody.getText());
                    holderMethod.deleteCharAt(holderMethod.length() - 1);
                }
            }
            if (recycler && holderMethod.indexOf("super") == -1) {
                holderMethod.append("super(itemView);\n");
            }
            for (ResBean resBean : resBeans) {
                if (resBean.isChecked()) {
                    String findId = "findViewById(" + resBean.getFullId() + ");";
                    if (methodBody == null || !methodBody.getText().contains(findId)) {
                        holderMethod.append("this.")
                                .append(resBean.getFieldName())
                                .append(" = ")
                                .append("itemView.")
                                .append(findId)
                                .append("\n");
                    }
                }
            }
            if (methodBody != null) {
                methodBody.delete();
            }
            holderMethod.append("}");
            holderField.append(holderMethod);
            if (holderClass == null) {
                if (recycler) {
                    String holdClass = "class ViewHolder extends RecyclerView.ViewHolder {" +
                            holderField +
                            "}";
                    holderClass = factory.createClassFromText(holdClass, psiClass);
                    holderClass = holderClass.getInnerClasses()[0];
                } else {
                    holderClass = factory.createClassFromText(holderField.toString(), psiClass);
                    holderClass.setName("ViewHolder");
                    if (holderClass.getModifierList() != null) {
                        holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                        holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
                    }
                }

                psiClass.add(holderClass);
            } else {
                if (holderClass.getModifierList() != null) {
                    if (!holderClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                        holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                    }
                    if (!holderClass.hasModifierProperty(PsiModifier.STATIC)) {
                        holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
                    }
                }
                PsiElementFactory holderFactory = JavaPsiFacade.getElementFactory(holderClass.getProject());
                for (ResBean resBean : resBeans) {
                    if (holderClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                        PsiField field = holderFactory.createFieldFromText("public " + resBean.getFullName() + " " + resBean.getFieldName() + ";", holderClass);
                        holderClass.add(field);
                    }
                }
                if (findMethod == null) {
                    holderClass.add(holderFactory.createMethodFromText(holderMethod.toString(), holderClass));
                } else {
                    findMethod.add(factory.createCodeBlockFromText(holderMethod.toString(), findMethod));
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    /**
     * 方法
     */
    @Override
    protected void generateFindViewById() {
        try {
            StringBuilder createMethod = new StringBuilder();
            if (mIsActivity) {
                createMethod.append("private void initViews() {}");
            } else {
                createMethod.append("private void initViews(View view) {}");
            }
            PsiMethod[] methods = psiClass.findMethodsByName("initViews", false);
            if (methods.length == 0) {
                psiClass.add(factory.createMethodFromText(createMethod.toString(), psiClass));
            }
            methods = psiClass.findMethodsByName("initViews", false);
            PsiMethod findViewsMethod = methods[0];
            StringBuilder block = new StringBuilder();
            PsiCodeBlock methodBody = findViewsMethod.getBody();
            String view = mIsActivity ? "" : "view";
            for (ResBean resBean : resBeans) {
                String findId = "findViewById(" + resBean.getFullId() + ");";
                if (resBean.isChecked() && methodBody != null && !methodBody.getText().contains(findId)) {
                    block.append(resBean.getJavaStatement(view));
                }
            }
            if (block.length() > 0) {
                if (methodBody != null) {
                    StringBuilder codeBlock = new StringBuilder(methodBody.getText());
                    methodBody.delete();
                    codeBlock.insert(codeBlock.length() - 1, block.toString());
                    codeBlock.append("\n");
                    findViewsMethod.add(factory.createCodeBlockFromText(codeBlock.toString(), findViewsMethod));
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    /**
     * 调用方法
     */
    @Override
    protected void performFunction() {
        try {
            if (mIsActivity) {
                PsiMethod[] methods = psiClass.findMethodsByName("onCreate", false);
                if (methods.length > 0) {
                    PsiMethod onCreate = methods[0];
                    PsiStatement setContentView = null;
                    if (onCreate.getBody() != null) {
                        for (PsiStatement statement : onCreate.getBody().getStatements()) {
                            if (statement.getText().contains("initViews")) {
                                return;
                            } else if (statement.getText().contains("setContentView")) {
                                setContentView = statement;
                            }
                        }
                        if (setContentView != null) {
                            onCreate.getBody().addAfter(factory.createStatementFromText("initViews();", psiClass), setContentView);
                        }
                    }
                }
            } else {
                PsiMethod[] methods = psiClass.findMethodsByName("onCreateView", false);
                if (methods.length > 0) {
                    PsiMethod onCreateView = methods[0];
                    PsiStatement returnView;
                    if (onCreateView.getBody() != null) {
                        PsiStatement[] statements = onCreateView.getBody().getStatements();
                        for (PsiStatement statement : statements) {
                            if (statement.getText().contains("initViews")) {
                                return;
                            }
                        }
                        int count = statements.length;
                        returnView = statements[count - 1];
                        int index = returnView.getText().indexOf("inflater.inflate(");
                        if (index != -1) {
                            onCreateView.getBody().addBefore(factory.createStatementFromText("View view=" + returnView.getText().substring(index), psiClass), returnView);
                        }
                        onCreateView.getBody().addBefore(factory.createStatementFromText("initViews(view);", psiClass), returnView);
                        if (index != -1) {
                            onCreateView.getBody().addAfter(factory.createStatementFromText("return view;", psiClass), returnView);
                            returnView.delete();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

}
