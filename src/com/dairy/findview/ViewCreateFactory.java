package com.dairy.findview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.List;

public class ViewCreateFactory {

    private List<ResBean> resBeans;
    private PsiElementFactory factory;
    private PsiClass psiClass;
    private PsiFile psiFile;
    private boolean mIsKotlin = false;

    public ViewCreateFactory(List<ResBean> resIdBeans, PsiFile files) {
        this(resIdBeans, files, false);
    }

    public ViewCreateFactory(List<ResBean> resIdBeans, PsiFile files, boolean mIsKotlin) {
        this.resBeans = resIdBeans;
        this.psiFile = files;
        this.psiClass = Utils.getTargetClass(psiFile);
        factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        this.mIsKotlin = mIsKotlin;
    }


    public Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (Utils.isAdapter(psiFile, psiClass)) {
                generateAdapter();
            } else {
                //生成成员变量
                generateFields();
                //生成方法
                generateFindViewById();
                //调用方法
                performFunction();
            }
            //格式化
            JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
            styleManager.optimizeImports(psiFile);
            styleManager.shortenClassReferences(psiClass);
            new ReformatCodeProcessor(psiClass.getProject(), psiClass.getContainingFile(), null, false).runWithoutProgress();

        }
    };

    public void execute() {
        WriteCommandAction.runWriteCommandAction(psiFile.getProject(), mRunnable);
    }

    private void generateFields() {
        try {
            for (ResBean resBean : resBeans) {
                if (psiClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                    PsiField field = factory.createFieldFromText("private " + resBean.getName() + " " + resBean.getFieldName() + ";", psiClass);
                    psiClass.add(field);
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    private boolean isRecyclerViewAdapter() {
        return psiClass.findMethodsByName("onCreateViewHolder", false).length != 0
                && psiClass.findMethodsByName("onBindViewHolder", false).length != 0;
    }

    private void generateAdapter() {
        try {
            boolean recycler = isRecyclerViewAdapter();
            PsiClass holderClass = null;
            if (recycler) {
                for (PsiClass inner : psiClass.getAllInnerClasses()) {
                    if (Utils.isFitClass(psiFile, inner, "android.support.v7.widget.RecyclerView.ViewHolder")) {
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
                    StringBuilder holdClass = new StringBuilder();
                    holdClass.append("class ViewHolder extends RecyclerView.ViewHolder {");
                    holdClass.append(holderField);
                    holdClass.append("}");
                    holderClass = factory.createClassFromText(holdClass.toString(), psiClass);
                    holderClass = holderClass.getInnerClasses()[0];
                } else {
                    holderClass = factory.createClassFromText(holderField.toString(), psiClass);
                    holderClass.setName("ViewHolder");
                    holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                    holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
                }

                psiClass.add(holderClass);
            } else {
                if (!holderClass.hasModifier(JvmModifier.PUBLIC)) {
                    holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                }
                if (!holderClass.hasModifier(JvmModifier.STATIC)) {
                    holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
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

    private void generateFindViewById() {
        try {
            StringBuilder createMethod = new StringBuilder();
            boolean activity = Utils.isActivity(psiFile, psiClass);
            if (activity) {
                createMethod.append("private void initViews(){}");
            } else {
                createMethod.append("private void initViews(View view){}");
            }
            PsiMethod[] methods = psiClass.findMethodsByName("initViews", false);
            if (methods.length == 0) {
                psiClass.add(factory.createMethodFromText(createMethod.toString(), psiClass));
            }
            methods = psiClass.findMethodsByName("initViews", false);
            PsiMethod findViewsMethod = methods[0];
            StringBuilder block = new StringBuilder();
            PsiCodeBlock methodBody = findViewsMethod.getBody();
            for (ResBean resBean : resBeans) {
                String findId = "findViewById(" + resBean.getFullId() + ");";
                if (resBean.isChecked() && methodBody != null && !methodBody.getText().contains(findId)) {
                    block.append(resBean.getFieldName()).append(" = ");
                    if (!activity) {
                        block.append("view.");
                    }
                    block.append(findId);
//                methodBody.add(factory.createStatementFromText(block.toString(), findViewsMethod));
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

    private void performFunction() {
        try {
            boolean activity = Utils.isActivity(psiFile, psiClass);
            if (activity) {
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
                    }
                    if (setContentView != null) {
                        onCreate.getBody().addAfter(factory.createStatementFromText("initViews();", psiClass), setContentView);
                    }
                }
            } else {
                PsiMethod[] methods = psiClass.findMethodsByName("onCreateView", false);
                if (methods.length > 0) {
                    PsiMethod onCreateView = methods[0];
                    PsiStatement returnView;
                    if (onCreateView.getBody() != null) {
                        for (PsiStatement statement : onCreateView.getBody().getStatements()) {
                            if (statement.getText().contains("initViews")) {
                                return;
                            }
                        }
                        int count = onCreateView.getBody().getStatementCount();
                        returnView = onCreateView.getBody().getStatements()[count - 1];
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
