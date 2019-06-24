package com.dairy.findview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;

import java.util.List;

public class FindViewsAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(DataKeys.PSI_FILE);
            Editor editor = e.getData(DataKeys.EDITOR);
            PsiClass psiClass = Utils.getTargetClass(psiFile);
            List<ResBean> resBeans = Utils.getResBeanFromFile(psiFile, editor);
            ViewCreateFactory factory = new ViewCreateFactory(resBeans, psiClass, psiFile);

            ShowDailog dialog = new ShowDailog(resBeans);
            dialog.setClickListener(() -> {
                if (resBeans.isEmpty()) {
                    Utils.showNotification(psiFile.getProject(), MessageType.INFO, "No layout found or No IDs found in layout");
                } else {
                    factory.execute();
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(null);//居中
            dialog.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
