package com.dairy.findview;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class BaseViewCreateFactory {
    protected List<ResBean> resBeans;
    protected PsiFile psiFile;

    public BaseViewCreateFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files) {
        this.resBeans = resIdBeans;
        this.psiFile = files;
    }

    protected int mAdapterType;

    protected boolean isAdapterType() {
        return mAdapterType >= AdapterType.ADAPTER_BASE.index && mAdapterType <= AdapterType.ADAPTER_RECYCLER.index;
    }

    protected boolean isRecyclerAdapter() {
        return mAdapterType == AdapterType.ADAPTER_RECYCLER.index;
    }


    public abstract void execute();

    public void execute(Runnable runnable) {
        if (psiFile != null) {
            WriteCommandAction.runWriteCommandAction(psiFile.getProject(), runnable);
        }
    }

    protected void formatCode() {

    }

    protected void performFunction() {

    }

    protected void generateFindViewById() {

    }

    protected void generateFields() {
    }

    protected void generateAdapter() {

    }
}
