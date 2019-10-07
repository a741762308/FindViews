package com.dairy.findview;

import com.intellij.ide.util.PropertiesComponent;

import static com.dairy.findview.Config.Key.KEY_KOTLIN_LAZY;
import static com.dairy.findview.Config.Key.KEY_MODIFIER_TYPE;

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