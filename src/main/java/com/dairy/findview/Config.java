package com.dairy.findview;

import com.intellij.ide.util.PropertiesComponent;

import static com.dairy.findview.Config.Key.*;

public class Config {
    private volatile static Config sInstance;
    private FileType mFileType;
    private ClassType mClassType;

    public static Config get() {
        if (sInstance == null) {
            synchronized (Config.class) {
                if (sInstance == null) {
                    sInstance = new Config();
                }
            }
        }
        return sInstance;
    }

    static final class Key {
        public static final String KEY_FILE_TYPE = "com.dairy.file_type";
        public static final String KEY_MODIFIER_TYPE = "com.dairy.modifier_type";
        public static final String KEY_KOTLIN_LAZY = "com.dairy.kotlin_lazy";
        public static final String KEY_BUTTER_KNIFE = "com.dairy.butter_knife";
        public static final String KEY_BUTTER_TYPE = "com.dairy.butter_type";
        public static final String KEY_BUTTER_KNIFE_BIND = "com.dairy.butter_knife_bind";
        public static final String KEY_BUTTER_KNIFE_UNBIND = "com.dairy.butter_knife_unbind";
        public static final String KEY_BUTTER_KNIFE_LIBRARY = "com.dairy.butter_knife_library";
    }

    public boolean isButterKnife() {
        return PropertiesComponent.getInstance().getBoolean(KEY_BUTTER_KNIFE);
    }

    public void saveButterKnife(boolean butterKnife) {
        PropertiesComponent.getInstance().setValue(KEY_BUTTER_KNIFE, butterKnife);
    }

    public boolean isButterKnifeBind() {
        return PropertiesComponent.getInstance().getBoolean(KEY_BUTTER_KNIFE_BIND, true);
    }

    public void saveButterKnifeUnBind(boolean butterKnife) {
        PropertiesComponent.getInstance().setValue(KEY_BUTTER_KNIFE_UNBIND, butterKnife);
    }

    public boolean isButterKnifeUnBind() {
        return PropertiesComponent.getInstance().getBoolean(KEY_BUTTER_KNIFE_UNBIND);
    }

    public void saveButterKnifeBind(boolean butterKnife) {
        PropertiesComponent.getInstance().setValue(KEY_BUTTER_KNIFE_BIND, butterKnife);
    }

    public boolean isButterKnifeR2() {
        return PropertiesComponent.getInstance().getBoolean(KEY_BUTTER_KNIFE_LIBRARY);
    }

    public void saveButterKnifeLibrary(boolean isR2) {
        PropertiesComponent.getInstance().setValue(KEY_BUTTER_KNIFE_LIBRARY, isR2);
    }

    public void saveKotlinLazy(boolean lazy) {
        PropertiesComponent.getInstance().setValue(KEY_KOTLIN_LAZY, lazy);
    }

    public boolean isKotlinLazy() {
        return PropertiesComponent.getInstance().getBoolean(KEY_KOTLIN_LAZY);
    }

    public void saveModifierType(ModifierType type) {
        PropertiesComponent.getInstance().setValue(KEY_MODIFIER_TYPE, type.name());
    }

    public String getModifierType() {
        return PropertiesComponent.getInstance().getValue(KEY_MODIFIER_TYPE, ModifierType.PRIVATE.name());
    }

    public String getJavaModifier() {
        return getModifier(FileType.JAVA);
    }

    public String getKotlinModifier() {
        return getModifier(FileType.KOTLIN);
    }

    private String getModifier(FileType fileType) {
        if (fileType == null) {
            fileType = getFileType();
        }
        ModifierType type = ModifierType.valueOf(getModifierType());
        if (type == ModifierType.PUBLIC) {
            return "public";
        } else if (type == ModifierType.PROTECT) {
            return "protect";
        } else if (type == ModifierType.DEFAULT) {
            if (fileType == FileType.JAVA) {
                return "";
            } else {
                return "internal";
            }
        } else if (type == ModifierType.PRIVATE) {
            return "private";
        }
        return "private";
    }

    public FileType getFileType() {
        if (mFileType == null) {
            mFileType = FileType.JAVA;
        }
        return mFileType;
    }

    public void setFileType(FileType type) {
        this.mFileType = type;
    }

    public ClassType getClassType() {
        if (mClassType == null) {
            mClassType = ClassType.ACTIVITY;
        }
        return mClassType;
    }

    public void setClassType(ClassType classType) {
        mClassType = classType;
    }
}
