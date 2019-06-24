package com.dairy.findview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ViewCreateFactory extends WriteCommandAction.Simple {

    private List<ResBean> resBeans;
    private PsiElementFactory factory;
    private PsiClass psiClass;
    private PsiFile psiFile;

    public ViewCreateFactory(List<ResBean> resIdBeans, PsiClass psiClass, PsiFile files) {
        this(files.getProject(), files);
        this.resBeans = resIdBeans;
        this.psiClass = psiClass;
        this.psiFile = files;
        factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
    }

    private ViewCreateFactory(@Nullable Project project, PsiFile... files) {
        super(project, files);
    }

    @Override
    protected void run() throws Throwable {
        //生成成员变量
        generateFields();
        //生成方法
        generateFindViewById();
        //调用方法
        performFunction();
        //格式化
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        styleManager.optimizeImports(psiFile);
        styleManager.shortenClassReferences(psiClass);
        new ReformatCodeProcessor(psiClass.getProject(), psiClass.getContainingFile(), null, false).runWithoutProgress();
    }


    private void generateFields() {
        try {
            for (ResBean resBean : resBeans) {
                if (psiClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                    PsiField field = factory.createFieldFromText("private " + resBean.getName() + " " + resBean.getFieldName() + ";", psiClass);
                    psiClass.add(field);
                }
            }
        } catch (Throwable ignored) {

        }

    }

    private void generateFindViewById() {
        try {
            StringBuilder createMethod = new StringBuilder();
            boolean activity = isActivity();
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
        } catch (Throwable ignored) {

        }
    }

    private void performFunction() {
        try {
            boolean activity = isActivity();
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
        } catch (Throwable ignored) {

        }
    }

    private boolean isActivity() {
        GlobalSearchScope scope = GlobalSearchScope.allScope(psiFile.getProject());
        PsiClass activityClass = JavaPsiFacade.getInstance(psiFile.getProject()).findClass(
                "android.app.Activity", scope);
        PsiClass appActivityClass = JavaPsiFacade.getInstance(psiFile.getProject()).findClass(
                "android.support.v7.app.AppCompatActivity", scope);
        return (activityClass != null && psiClass.isInheritor(activityClass, false))
                || (appActivityClass != null && psiClass.isInheritor(appActivityClass, false))
                || (psiClass.getName() != null && psiClass.getName().contains("Activity"));
    }
}
