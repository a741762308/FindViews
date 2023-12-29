package com.dairy.findview;

import java.util.List;

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


    public static final String BUTTER_KNIFE_ANNOTATION = "@BindView(%s) %s";

    public static String getJavaButterKnifeFiled(String name, String fieldName, String fullId) {
        return String.format(BUTTER_KNIFE_ANNOTATION, fullId, getJavaFiled(name, fieldName));
    }

    public static String getAdapterJavaButterKnifeFiled(String name, String fieldName, String fullId) {
        return String.format(BUTTER_KNIFE_ANNOTATION, fullId, getAdapterJavaFiled(name, fieldName));
    }

    public static String getKotlinButterKnifeProperty(String name, String fieldName, String fullId) {
        return String.format(BUTTER_KNIFE_ANNOTATION, fullId, getKotlinProperty(name, fieldName));
    }

    public static String getAdapterKotlinButterKnifeProperty(String name, String fieldName, String fullId) {
        return String.format(BUTTER_KNIFE_ANNOTATION, fullId, getAdapterKotlinProperty(name, fieldName));
    }

    public static String getJavaFiled(String name, String fieldName) {
        return String.format(JAVA_FILED, Config.get().getJavaModifier(), name, fieldName);
    }

    public static String getAdapterJavaFiled(String name, String fieldName) {
        return String.format(JAVA_FILED, "", name, fieldName).substring(1);
    }

    public static String getJavaStatement(String fieldName, String view, String fullId) {
        return String.format(JAVA_VARIABLE_STATEMENT, fieldName, view == null ? "" : (view.length() != 0 ? view + "." : view), fullId);
    }

    public static String getKotlinProperty(String name, String fieldName) {
        return String.format(KOTLIN_PROPERTY, Config.get().getKotlinModifier(), fieldName, name);
    }

    public static String getAdapterKotlinProperty(String name, String fieldName) {
        return String.format(KOTLIN_PROPERTY, "", fieldName, name);
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

    public static String getGenerateAdapterCode(List<ResBean> resBeans, boolean isKotlin, String className, String xmlPath) {
        if (isKotlin) {
            return generateKotlinCode(resBeans, className, xmlPath);
        } else {
            return generateJavaCode(resBeans, className, xmlPath);
        }
    }

    public String getGenerateAdapterBindingCode(List<ResBean> resBeans, boolean isKotlin, String className, String xmlPath) {
        if (isKotlin) {
            return generateKotlinBindingCode(resBeans, className, xmlPath);
        } else {
            return generateJavaBindingCode(resBeans, className, xmlPath);
        }
    }

    private static String generateKotlinCode(List<ResBean> resBeans, String className, String xmlPath) {
        final StringBuilder sb = new StringBuilder();
        sb.append("class ").append(className).append(": RecyclerView.Adapter<").append(className).append(".ViewHolder>() {\n");
        sb.append("\n");

        sb.append("    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {\n");
        sb.append("        val view= LayoutInflater.from(viewGroup.context).inflate(").append(xmlPath).append(", viewGroup, false)\n");
        sb.append("        return ViewHolder(view)\n");
        sb.append("    }\n");
        sb.append("\n");

        sb.append("    override fun onBindViewHolder(holder: ViewHolder, position: Int) {\n");
        sb.append("        \n");
        sb.append("    }\n");
        sb.append("\n");

        sb.append("    override fun getItemCount(): Int {\n");
        sb.append("        return 0\n");
        sb.append("    }\n");
        sb.append("\n");

        sb.append("    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){\n");
        if (Config.get().isButterKnife()) {
            for (ResBean bean : resBeans) {
                if (bean.isChecked()) {
                    sb.append("        ").append(Config.get().isButterKnifeBind() ? bean.getAdapterKotlinButterKnifeProperty() : bean.getAdapterKotlinProperty()).append("\n");
                }
            }
            sb.append("        init {\n" +
                    "            ButterKnife.bind(this,itemView)\n" +
                    "        }\n");
        } else {
            for (ResBean bean : resBeans) {
                if (bean.isChecked()) {
                    sb.append("        ").append(bean.getKotlinAdapterProperty("itemView")).append("\n");
                }
            }
        }
        sb.append("    }\n");
        sb.append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String generateJavaCode(List<ResBean> resBeans, String className, String xmlPath) {
        final StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className);
        sb.append(" extends RecyclerView.Adapter<").append(className).append(".ViewHolder> {\n");
        sb.append("\n");

        sb.append("    @NonNull\n");
        sb.append("    @Override\n");
        sb.append("    public ").append(className).append(".ViewHolder").append(" onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {\n");
        sb.append("        View view = LayoutInflater.from(viewGroup.getContext()).inflate(").append(xmlPath).append(", viewGroup, false);\n ");
        sb.append("        return new ViewHolder(view);\n");
        sb.append("    }\n");
        sb.append("\n");

        sb.append("    @Override\n");
        sb.append("    public void onBindViewHolder(@NonNull ").append(className).append(".ViewHolder holder, int position) {\n");
        sb.append("\n");
        sb.append("    }\n");
        sb.append("\n");

        sb.append("    public int getItemCount() {\n");
        sb.append("        return 0;\n");
        sb.append("    }\n");
        sb.append("\n\n");

        sb.append("    class ViewHolder extends RecyclerView.ViewHolder {\n");
        for (ResBean bean : resBeans) {
            if (bean.isChecked()) {
                sb.append("        ").append(Config.get().isButterKnife() ? bean.getAdapterJavaButterKnifeFiled() : bean.getAdapterJavaFiled()).append("\n");
            }
        }
        sb.append("\n");
        sb.append("        public ViewHolder(View itemView) {\n");
        sb.append("            super(itemView);\n");
        if (Config.get().isButterKnife()) {
            sb.append("            ButterKnife.bind(this, itemView);\n");
        } else {
            for (ResBean bean : resBeans) {
                if (bean.isChecked()) {
                    sb.append("            ").append("this.").append(bean.getJavaStatement("itemView")).append("\n");
                }
            }
        }
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}");
        return sb.toString();
    }

    private static String generateKotlinBindingCode(List<ResBean> resBeans, String className, String xmlPath) {
        return "";
    }

    private static String generateJavaBindingCode(List<ResBean> resBeans, String className, String xmlPath) {
        return "";
    }
}
