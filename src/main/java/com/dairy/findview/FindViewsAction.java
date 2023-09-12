package com.dairy.findview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

public class FindViewsAction extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            List<ResBean> resBeans = Utils.getResBeanFromFile(psiFile, editor);

            ShowDialog dialog = new ShowDialog(resBeans);
            dialog.setClickListener((boolean kotlin) -> {
                BaseViewCreateFactory factory;

                if (Config.get().isButterKnife()) {
                    if (kotlin) {
                        KtClass ktClass = Utils.getPsiClassFromEvent(editor);
                        factory = new KtButterKnifeCreateFactory(resBeans, psiFile, ktClass);
                    } else {
                        PsiClass psiClass = Utils.getTargetClass(editor, psiFile);
                        factory = new JavaButterKnifeCreateFactory(resBeans, psiFile, psiClass);
                    }
                } else {
                    if (kotlin) {
                        KtClass ktClass = Utils.getPsiClassFromEvent(editor);
                        factory = new KtViewCreateFactory(resBeans, psiFile, ktClass);
                    } else {
                        PsiClass psiClass = Utils.getTargetClass(editor, psiFile);
                        factory = new JavaViewCreateFactory(resBeans, psiFile, psiClass);
                    }
                }
                if (resBeans.isEmpty()) {
                    Utils.showNotification(psiFile.getProject(), MessageType.WARNING, "No layout found or No IDs found in layout");
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
