package com.dairy.findview;

import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

public class GenerateViewFactory extends BaseViewCreateFactory {

    private final boolean isKotlin;
    private final String adapterName;
    private final String layoutPath;

    public GenerateViewFactory(@NotNull List<ResBean> resIdBeans, @NotNull PsiFile files, boolean isKotlin, String adapterName) {
        super(resIdBeans, files);
        this.isKotlin = isKotlin;
        this.adapterName = adapterName;
        layoutPath = Utils.getXmlPath(psiFile);
    }

    @Override
    public void execute() {
        generateCode();
    }

    private void generateCode() {
        try {
            final String code = getGenerateCode();
            //复制到剪贴板
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transferable = new StringSelection(code);
            clipboard.setContents(transferable, null);
            Utils.showNotification(psiFile.getProject(), MessageType.INFO, "Success copy adapter code to clipboard");
        } catch (Throwable t) {
            Utils.showNotification(psiFile.getProject(), MessageType.ERROR, t.getMessage());
        }

    }


    private String getGenerateCode() {
        return CodeConstant.getGenerateAdapterCode(resBeans, isKotlin, adapterName, layoutPath);
    }
}
