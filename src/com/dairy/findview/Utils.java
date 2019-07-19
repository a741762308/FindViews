package com.dairy.findview;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static String[] sActivityClass = new String[]{
            "android.app.Activity",
            "android.support.v7.app.AppCompatActivity"
    };
    private static String[] sAdapterClass = new String[]{
            "android.widget.BaseAdapter",
            "android.widget.SimpleAdapter",
            "android.widget.ArrayAdapter",
            "android.widget.SimpleCursorAdapter",
            "android.widget.SimpleExpandableListAdapter",
            "android.widget.HeaderViewListAdapter",
            "android.support.v7.widget.RecyclerView.Adapter"
    };


    public static List<ResBean> getResBeanFromFile(PsiFile psiFile, Editor editor) {
        final List<ResBean> resBeans = new ArrayList<>();
        PsiFile file = getFileFromCaret(psiFile, editor);
        if (file != null) {
            return getResBeanFromFile(file, resBeans);
        }
        return resBeans;
    }

    private static List<ResBean> getResBeanFromFile(PsiFile file, List<ResBean> resBeans) {
        file.accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                super.visitXmlTag(tag);
                if (tag.getName().endsWith("include")) {
                    XmlAttribute layout = tag.getAttribute("layout", null);
                    if (layout == null) {
                        return;
                    }
                    String value = layout.getValue();
                    if (value == null) {
                        return;
                    }
                    if (value.startsWith("@layout/")) {
                        String[] split = value.split("/");
                        String name = split[1];
                        PsiFile include = getFileByName(file, name);
                        if (include == null) {
                            return;
                        }
                        getResBeanFromFile(include, resBeans);
                    }
                } else {
                    XmlAttribute id = tag.getAttribute("android:id");
                    if (id == null) {
                        return;
                    }
                    String idValue = id.getValue();
                    if (idValue == null) {
                        return;
                    }
                    XmlAttribute clazz = tag.getAttribute("class", null);
                    String name = tag.getName();
                    if (clazz != null) {
                        name = clazz.getName();
                    }
                    ResBean bean = new ResBean(name, idValue);
                    resBeans.add(bean);
                }
            }
        });
        return resBeans;
    }

    public static PsiFile getFileFromCaret(PsiFile psiFile, Editor editor) {
        if (psiFile != null && editor != null) {
            CaretModel caret = editor.getCaretModel();
            int offset = caret.getOffset();
            PsiElement elementA = psiFile.findElementAt(offset);
            PsiElement elementB = psiFile.findElementAt(offset - 1);
            PsiFile layout = getFileFromElement(elementA);
            if (layout != null) {
                return layout;
            }
            return getFileFromElement(elementB);
        }
        return null;
    }

    public static PsiFile getFileFromElement(PsiElement element) {
        if (element != null) {
            //java
            PsiFile file = getFileParent(element, element.getParent());
            if (file != null) {
                Config.get().setFileType(FileType.JAVA);
                return file;
            }
            //kotlin
            file = getFileParent(element, element.getParent().getParent());
            if (file != null) {
                Config.get().setFileType(FileType.KOTLIN);
                return file;
            }
        }
        return null;
    }

    private static PsiFile getFileParent(PsiElement element, PsiElement parent) {
        if (element != null && parent != null) {
            String path = parent.getText();
            if (path.startsWith("R.layout.") || path.startsWith("android.R.layout")) {
                String name = String.format("%s.xml", element.getText());
                return getFileByName(element, element.getProject(), name);
            }
        }
        return null;
    }

    public static PsiFile getFileByName(PsiFile psiFile, String fileName) {
        String name = String.format("%s.xml", fileName);
        return getFileByName(psiFile, psiFile.getProject(), name);
    }

    public static PsiFile getFileByName(PsiElement psiElement, Project project, String name) {
        Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(psiElement);
        if (moduleForPsiElement != null) {
            GlobalSearchScope searchScope = GlobalSearchScope.moduleScope(moduleForPsiElement);
            PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, name, searchScope);
            if (psiFiles.length != 0) {
                return psiFiles[0];
            }
        }
        return null;
    }

    public static PsiClass getTargetClass(PsiFile classFile) {
        GlobalSearchScope globalSearchScope = GlobalSearchScope.fileScope(classFile);
        String fullName = classFile.getName();
        String className = fullName.split("\\.")[0];
        return PsiShortNamesCache.getInstance(classFile.getProject()).getClassesByName(className, globalSearchScope)[0];
    }

    public static boolean isActivity(PsiFile psiFile, PsiClass psiClass) {
        return isFitClass(psiFile, psiClass, sActivityClass) ||
                (psiClass.getName() != null && psiClass.getName().contains("Activity"));
    }

    public static boolean isAdapter(PsiFile psiFile, PsiClass psiClass) {
        return (isFitClass(psiFile, psiClass, sAdapterClass)) ||
                (psiClass.getName() != null && psiClass.getName().contains("Adapter"));
    }

    public static boolean isFitClass(PsiFile psiFile, PsiClass psiClass, String... classArray) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(psiFile.getProject());
        for (String classString : classArray) {
            PsiClass activityClass = JavaPsiFacade.getInstance(psiFile.getProject()).findClass(
                    classString, scope);
            if (activityClass != null && psiClass.isInheritor(activityClass, false)) {
                return true;
            }
        }
        return false;
    }

    public static void showNotification(Project project, MessageType type, String text) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, type, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }
}
