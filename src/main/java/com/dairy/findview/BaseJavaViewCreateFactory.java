package com.dairy.findview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class BaseJavaViewCreateFactory extends BaseViewCreateFactory {
    protected PsiElementFactory factory;
    protected PsiClass psiClass;
    protected boolean mIsActivity;

    public BaseJavaViewCreateFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, @NotNull PsiClass psiClass) {
        super(resIdBeans, files);
        this.psiClass = psiClass;
        mIsActivity = Utils.isJavaActivity(psiFile, psiClass);
        factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
    }

    protected boolean isRecyclerViewAdapter() {
        return isRecyclerAdapter() || Utils.isJavaRecyclerAdapter(psiFile, psiClass) || psiClass.findMethodsByName("onCreateViewHolder", false).length != 0
                && psiClass.findMethodsByName("onBindViewHolder", false).length != 0;
    }

    protected PsiClass getAdapterHolder(boolean recycler) {
        if (recycler) {
            for (PsiClass inner : psiClass.getAllInnerClasses()) {
                if (Utils.isJavaRecyclerHolder(psiFile, inner)) {
                    return inner;
                }
            }
        } else {
            for (PsiClass inner : psiClass.getAllInnerClasses()) {
                if (inner.getName() != null && inner.getName().contains("ViewHolder")) {
                    return inner;
                }
            }
        }
        return null;
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

    protected void changeHolderModifier(PsiClass holderClass) {
        if (holderClass.getModifierList() != null) {
            if (!holderClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                holderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
            }
            if (!holderClass.hasModifierProperty(PsiModifier.STATIC)) {
                holderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
            }
        }
    }
}
