package com.dairy.findview;

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
    }

    public FileType getFileType() {
        if (mFileType == null) {
            mFileType = FileType.JAVA;
//            mFileType = FileType.valueOf(PropertiesComponent.getInstance().getValue(Key.KEY_FILE_TYPE, FileType.JAVA.name()));
        }
        return mFileType;
    }

    public void setFileType(FileType type) {
        this.mFileType = type;
//        PropertiesComponent.getInstance().setValue(Key.KEY_FILE_TYPE, type.name());
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
