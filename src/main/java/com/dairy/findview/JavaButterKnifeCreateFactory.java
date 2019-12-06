package com.dairy.findview;

import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JavaButterKnifeCreateFactory extends JavaViewCreateFactory {

    public JavaButterKnifeCreateFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, @NotNull PsiClass psiClass) {
        super(resIdBeans, files, psiClass);
    }


    @Override
    protected void generateFields() {
        try {
            for (ResBean resBean : resBeans) {
                if (psiClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                    PsiField field = factory.createFieldFromText(resBean.getJavaButterKnifeFiled(), psiClass);
                    psiClass.add(field);
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }

    @Override
    protected void generateFindViewById() {

    }

    @Override
    protected void generateAdapter() {
        try {
            boolean recycler = isRecyclerViewAdapter();
            PsiClass holderClass = getAdapterHolder(recycler);
            StringBuilder holderField = new StringBuilder();
            for (ResBean resBean : resBeans) {
                if (resBean.isChecked()) {
                    holderField.append(resBean.getAdapterJavaButterKnifeFiled())
                            .append("\n");
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
                    methodBody.delete();
                }
            }
            if (recycler && holderMethod.indexOf("super") == -1) {
                holderMethod.append("super(itemView);\n");
            }
            if (holderMethod.indexOf("ButterKnife.bind") == -1) {
                holderMethod.append("ButterKnife.bind(this,itemView);\n");
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
                    holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                    holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
                }
                PsiElementFactory holderFactory = JavaPsiFacade.getElementFactory(holderClass.getProject());
                for (ResBean resBean : resBeans) {
                    if (holderClass.findFieldByName(resBean.getFieldName(), false) == null && resBean.isChecked()) {
                        PsiField field = holderFactory.createFieldFromText(resBean.getAdapterJavaButterKnifeFiled(), holderClass);
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

    @Override
    protected void performFunction() {
        try {
            if (Config.get().isButterKnifeBind()) {
                if (mIsActivity) {
                    PsiMethod[] methods = psiClass.findMethodsByName("onCreate", false);
                    if (methods.length > 0) {
                        PsiMethod onCreate = methods[0];
                        PsiStatement setContentView = null;
                        if (onCreate.getBody() != null) {
                            for (PsiStatement statement : onCreate.getBody().getStatements()) {
                                if (statement.getText().contains("ButterKnife.bind(this)")) {
                                    return;
                                } else if (statement.getText().contains("setContentView")) {
                                    setContentView = statement;
                                }
                            }
                            if (setContentView != null) {
                                String bind;
                                if (Config.get().isButterKnifeUnBind()) {
                                    if (psiClass.findFieldByName("mUnBinder", false) == null) {
                                        psiClass.add(factory.createFieldFromText("private Unbinder mUnBinder;", psiClass));
                                    }
                                    bind = "mUnBinder = ButterKnife.bind(this);";
                                } else {
                                    bind = "ButterKnife.bind(this);";
                                }
                                onCreate.getBody().addAfter(factory.createStatementFromText(bind, psiClass), setContentView);

                                if (Config.get().isButterKnifeUnBind()) {
                                    addUnbindStatement();
                                }
                            }
                        }
                    }
                } else {
                    PsiMethod[] methods = psiClass.findMethodsByName("onCreateView", false);
                    if (methods.length > 0) {
                        PsiMethod onCreateView = methods[0];
                        if (onCreateView.getBody() != null) {
                            PsiStatement[] statements = onCreateView.getBody().getStatements();
                            for (PsiStatement statement : statements) {
                                if (statement.getText().contains("ButterKnife.bind")) {
                                    return;
                                }
                            }
                            PsiStatement returnView = statements[statements.length - 1];
                            int index = returnView.getText().indexOf("inflater.inflate(");
                            if (index != -1) {
                                onCreateView.getBody().addBefore(factory.createStatementFromText("View view=" + returnView.getText().substring(index), psiClass), returnView);
                            }
                            String bind;
                            if (Config.get().isButterKnifeUnBind()) {
                                if (psiClass.findFieldByName("mUnBinder", false) == null) {
                                    psiClass.add(factory.createFieldFromText("private Unbinder mUnBinder;", psiClass));
                                }

                                bind = "mUnBinder = ButterKnife.bind(this, view);";
                            } else {
                                bind = "ButterKnife.bind(this, view);";
                            }
                            onCreateView.getBody().addBefore(factory.createStatementFromText(bind, psiClass), returnView);

                            if (index != -1) {
                                onCreateView.getBody().addAfter(factory.createStatementFromText("return view;", psiClass), returnView);
                                returnView.delete();
                            }
                        }
                        if (Config.get().isButterKnifeUnBind()) {
                            addUnbindStatement();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }
    }


    private void addUnbindStatement() {
        PsiMethod[] methods;
        if (mIsActivity) {
            methods = psiClass.findMethodsByName("onDestroy", false);
        } else {
            methods = psiClass.findMethodsByName("onDestroyView", false);
        }
        if (methods.length > 0) {
            PsiMethod destroy = methods[0];
            if (destroy.getBody() != null) {
                for (PsiStatement statement : destroy.getBody().getStatements()) {
                    if (statement.getText().contains(".unbind()")) {
                        return;
                    }
                }
                PsiStatement[] statements = destroy.getBody().getStatements();
                PsiStatement last = statements[statements.length - 1];
                destroy.getBody().addAfter(factory.createStatementFromText("mUnBinder.unbind();", psiClass), last);
            }
        } else {
            StringBuilder methodString = new StringBuilder("@Override\n");
            if (mIsActivity) {
                methodString.append("protected void onDestroy() {\n");
                methodString.append("super.onDestroy();\n");
            } else {
                methodString.append("public void onDestroyView() {\n");
                methodString.append("super.onDestroyView();\n");
            }
            methodString.append("mUnBinder.unbind();\n");
            methodString.append("}\n");
            psiClass.add(factory.createMethodFromText(methodString.toString(), psiClass));

        }
    }
}
