package com.dairy.findview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResBean {
    private static final Pattern sIdPattern = Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE);

    private boolean isChecked = true;
    private String name;
    private String fullName;
    private boolean isSystem;
    private String id;
    private int nameType = 3;//aa_bb=1,aaBb=2,mAaBb=3

    public ResBean(String name, String id) {
        final Matcher matcher = sIdPattern.matcher(id);
        if (matcher.find() && matcher.groupCount() > 1) {
            this.id = matcher.group(2);
            String group = matcher.group(1);
            isSystem = !(group == null || group.length() == 0);
        }
        if (this.id == null) {
            throw new IllegalArgumentException("Invalid format of view id");
        }
        String[] packages = name.split("\\.");
        if (packages.length > 1) {
            this.fullName = name;
            this.name = packages[packages.length - 1];
        } else {
            this.fullName = null;
            this.name = name;
        }
    }

    public void setNameType(int nameType) {
        this.nameType = nameType;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getName() {
        return name;
    }

    public String getFieldName() {
        String fieldName = id;
        String[] names = id.split("_");
        if (nameType == 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                if (i == 0) {
                    sb.append("m");
                }
                String word = names[i];
                sb.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase());
            }
            fieldName = sb.toString();
        } else if (nameType == 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length; i++) {
                String word = names[i];
                if (i == 0) {
                    sb.append(word);
                } else {
                    sb.append(word.substring(0, 1).toUpperCase())
                            .append(word.substring(1).toLowerCase());
                }
            }
            fieldName = sb.toString();
        }
        return fieldName;
    }

    public String getId() {
        return id;
    }

    public String getFullId() {
        if (isSystem) {
            return "android.R.id." + getId();
        }
        return "R.id." + getId();
    }
}
