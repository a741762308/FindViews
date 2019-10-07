package com.dairy.findview;

/**
 * Created by admin on 2019/8/9.
 */
public class CodeConstant {
    public static final String JAVA_FILED = "%s %s %s;";
    public static final String JAVA_VARIABLE_STATEMENT = "%s = %sfindViewById(%s);";

    public static final String KOTLIN_PROPERTY = "%s lateinit var %s: %s";
    public static final String KOTLIN_PROPERTY_LAZY = "%s val %s: %s by lazy { %sfindViewById<%s>(%s) }";
    public static final String KOTLIN_PROPERTY_ADAPTER = "val %s: %s";
    public static final String KOTLIN_PROPERTY_ADAPTER_LAZY = "val %s: %s = %sfindViewById(%s)";
    public static final String KOTLIN_VARIABLE_EXPRESSION = "%s = %sfindViewById(%s)";


    public static String getJavaFiled(String name, String fieldName) {
        return String.format(JAVA_FILED, Config.get().getJavaModifier(), name, fieldName);
    }

    public static String getJavaStatement(String fieldName, String view, String fullId) {
        return String.format(JAVA_VARIABLE_STATEMENT, fieldName, view == null ? "" : (view.length() != 0 ? view + "." : view), fullId);
    }

    public static String getKotlinProperty(String name, String fieldName) {
        return String.format(KOTLIN_PROPERTY, Config.get().getKotlinModifier(), fieldName, name);
    }

    public static String getKotlinAdapterProperty(String name, String fieldName) {
        return String.format(KOTLIN_PROPERTY_ADAPTER, fieldName, name);
    }

    public static String getKotlinAdapterProperty(String fieldName, String name, String view, String fullId) {
        return String.format(KOTLIN_PROPERTY_ADAPTER_LAZY, fieldName, name, view == null ? "" : (view.length() != 0 ? view + "." : view), fullId);
    }

    public static String getKotlinLazyProperty(String fieldName, String name, String view, String fullId) {
        return String.format(KOTLIN_PROPERTY_LAZY, Config.get().getKotlinModifier(), fieldName, name, view == null ? "" : (view.length() != 0 ? view + "." : view), name, fullId);
    }

    public static String getKotlinExpression(String fieldName, String view, String fullId) {
        return String.format(KOTLIN_VARIABLE_EXPRESSION, fieldName, view == null ? "" : (view.length() != 0 ? view + "." : view), fullId);
    }
}
