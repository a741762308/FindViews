package com.dairy.findview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;

import java.util.List;

public class MergeViewAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            PsiFile layoutFile=Utils.getFileFromCaret(psiFile,editor);
            List<ResBean> resBeans = Utils.getResBeanFromFile(psiFile, editor);
            MergeDialog dialog = new MergeDialog(resBeans);
            dialog.setClickListener(new OnMergeClickListener() {
                @Override
                public void onOk(boolean kotlin) {
                    KtClass ktClass = Utils.getPsiClassFromEvent(editor);
                    KtViewMergeFactory factory = new KtViewMergeFactory(resBeans, psiFile, layoutFile, ktClass);
                    if (resBeans.isEmpty()) {
                        Utils.showNotification(psiFile.getProject(), MessageType.WARNING, "No layout found or No IDs found in layout");
                    } else {
                        factory.execute();
                    }
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
