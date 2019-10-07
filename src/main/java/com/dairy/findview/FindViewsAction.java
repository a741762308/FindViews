package com.dairy.findview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.internal.Location;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

public class FindViewsAction extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            PsiFile psiFile = e.getData(DataKeys.PSI_FILE);
            Editor editor = e.getData(DataKeys.EDITOR);
            List<ResBean> resBeans = Utils.getResBeanFromFile(psiFile, editor);

            ShowDialog dialog = new ShowDialog(resBeans);
            dialog.setClickListener((boolean kotlin) -> {
                BaseViewCreateFactory factory;
                if (kotlin) {
                    KtClass ktClass = getPsiClassFromEvent(editor);
                    factory = new KtViewCreateFactory(resBeans, psiFile, ktClass);
                } else {
                    PsiClass psiClass = Utils.getJavaClass(psiFile);
                    factory = new JavaViewCreateFactory(resBeans, psiFile, psiClass);
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

    private KtClass getPsiClassFromEvent(Editor editor) {
        assert editor != null;

        Project project = editor.getProject();
        if (project == null) return null;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (!(psiFile instanceof KtFile)) {
            return null;
        }
        Location location = Location.fromEditor(editor, project);
        PsiElement psiElement = psiFile.findElementAt(location.getStartOffset());
        if (psiElement == null) return null;

        return Utils.getKotlinClass(psiElement);
    }
}
