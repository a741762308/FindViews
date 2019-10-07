package com.dairy.findview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiFile;

import java.util.List;

public class GenerateViewsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                List<ResBean> resBeans = Utils.getResBeanFromFile(psiFile);
                XMLDialog dialog = new XMLDialog(resBeans, Utils.getXmlPath(psiFile));
                dialog.setClickListener((boolean kotlin) -> {
                    BaseViewCreateFactory factory = new GenerateViewFactory(resBeans, psiFile, kotlin, dialog.getAdapterName());
                    if (resBeans.isEmpty()) {
                        Utils.showNotification(psiFile.getProject(), MessageType.WARNING, "No layout found or No IDs found in layout");
                    } else {
                        factory.execute();
                    }
                });
                dialog.pack();
                dialog.setLocationRelativeTo(null);//居中
                dialog.setVisible(true);
            }
        } catch (Exception ignore) {

        }
    }
}
