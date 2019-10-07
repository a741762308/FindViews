package com.dairy.findview;

import com.intellij.openapi.components.ServiceManager;
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
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport;
import org.jetbrains.kotlin.asJava.elements.KtLightElement;
import org.jetbrains.kotlin.psi.*;

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

    private static List<ResBean> getResBeanFromFile(@NotNull PsiFile file, List<ResBean> resBeans) {
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

    public static PsiFile getFileByName(@NotNull PsiFile psiFile, String fileName) {
        String name = String.format("%s.xml", fileName);
        return getFileByName(psiFile, psiFile.getProject(), name);
    }

    public static PsiFile getFileByName(@NotNull PsiElement psiElement, @NotNull Project project, String name) {
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

    public static KtClass getKotlinClass(@NotNull PsiElement psiElement) {
        if (psiElement instanceof KtLightElement) {
            PsiElement origin = ((KtLightElement) psiElement).getKotlinOrigin();
            if (origin != null) {
                return getKotlinClass(origin);
            } else {
                return null;
            }

        } else if (psiElement instanceof KtClass && !((KtClass) psiElement).isEnum() &&
                !((KtClass) psiElement).isInterface() &&
                !((KtClass) psiElement).isAnnotation() &&
                !((KtClass) psiElement).isSealed()) {
            return (KtClass) psiElement;

        } else {
            PsiElement parent = psiElement.getParent();
            if (parent == null) {
                return null;
            } else {
                return getKotlinClass(parent);
            }
        }
    }

    public static boolean isJavaActivity(@NotNull PsiFile psiFile, @NotNull PsiClass psiClass) {
        return isJavaFitClass(psiFile, psiClass, sActivityClass) || isActivity(psiClass.getName());
    }

    public static boolean isJavaAdapter(@NotNull PsiFile psiFile, @NotNull PsiClass psiClass) {
        return isJavaFitClass(psiFile, psiClass, sAdapterClass) || isAdapter(psiClass.getName());
    }

    public static boolean isJavaFitClass(@NotNull PsiFile psiFile, @NotNull PsiClass psiClass, String... classArray) {
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

    public static boolean isKotlinActivity(@NotNull PsiFile psiFile, @NotNull KtClass ktClass) {
        return isKotlinFitClass(psiFile, ktClass, sActivityClass) || isActivity(ktClass.getName());
    }

    public static boolean isKotlinAdapter(@NotNull PsiFile psiFile, @NotNull KtClass ktClass) {
        return isKotlinFitClass(psiFile, ktClass, sAdapterClass) || isAdapter(ktClass.getName());
    }

    public static boolean isKotlinFitClass(@NotNull PsiFile psiFile, @NotNull KtClass ktClass, String... classArray) {
        KotlinAsJavaSupport support = ServiceManager.getService(psiFile.getProject(), KotlinAsJavaSupport.class);
        PsiClass psiClass = support.getLightClass(ktClass);
        if (psiClass != null) {
            return isJavaFitClass(psiFile, psiClass, classArray);
        }
        return false;
    }

    public static List<KtConstructor> allConstructors(KtClass ktClass) {
        List<KtConstructor> constructors = new ArrayList<>();
        KtPrimaryConstructor primaryConstructor = ktClass.getPrimaryConstructor();
        List<KtSecondaryConstructor> secondaryConstructors = ktClass.getSecondaryConstructors();
        if (primaryConstructor != null) {
            constructors.add(primaryConstructor);
        }
        constructors.addAll(secondaryConstructors);
        return constructors;
    }

    public static KtFunction findFunctionByName(@NotNull KtClass ktClass, @NotNull String name) {
        List<KtDeclaration> declarations = ktClass.getDeclarations();
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtFunction && declaration.getName() != null && declaration.getName().equals(name))
                return (KtFunction) declaration;
        }
        return null;
    }

    private static boolean isActivity(String name) {
        return name != null && name.contains("Activity");
    }

    private static boolean isAdapter(String name) {
        return name != null && name.contains("Adapter");
    }

    public static void showNotification(@NotNull Project project, MessageType type, String text) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, type, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }
}