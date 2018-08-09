package br.com.staroski.multicaster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MulticasterGenerator {

    private static final String TAG_CLASS_PACKAGE = "${class.package}";
    private static final String TAG_CLASS_IMPORTS = "${class.imports}";
    private static final String TAG_CLASS_NAME = "${class.name}";
    private static final String TAG_LISTENER_NAME = "${listener.name}";
    private static final String TAG_LISTENER_METHODS = "${listener.methods}";

    public String generate(String fullClassName, String fullListenerName) throws Exception {
        Class<?> listenerType = Class.forName(fullListenerName);
        String javaCode = loadTemplate();
        javaCode = javaCode.replace(TAG_CLASS_PACKAGE, getPackageName(fullClassName));
        javaCode = javaCode.replace(TAG_CLASS_IMPORTS, generateImports(listenerType));
        javaCode = javaCode.replace(TAG_CLASS_NAME, generateClassName(fullClassName));
        javaCode = javaCode.replace(TAG_LISTENER_NAME, listenerType.getSimpleName());
        javaCode = javaCode.replace(TAG_LISTENER_METHODS, generateListenerMethods(listenerType));
        return javaCode;
    }

    private String generateClassName(String fullClassName) {
        int index = fullClassName.lastIndexOf('.');
        if (index > 0) {
            return fullClassName.substring(index + 1);
        }
        return fullClassName;
    }

    private String generateImports(Class<?> listenerType) {
        StringBuilder imports = new StringBuilder();
        imports.append("import ").append(listenerType.getName()).append(";");
        Set<String> parameterImports = new TreeSet<>();
        for (Method method : getMethods(listenerType)) {
            for (Class<?> type : method.getParameterTypes()) {
                String name = type.getName();
                if (parameterImports.add(name)) {
                    imports.append("\nimport ").append(name).append(";");
                }
            }
        }
        return imports.toString();
    }

    private Method[] getMethods(Class<?> type) {
        List<Method> allMethods = new ArrayList<>();
        Class<?> supertype = type;
        while (supertype != null && !supertype.equals(Object.class)) {
            allMethods.addAll(Arrays.asList(supertype.getDeclaredMethods()));
            for (Class<?> itf : supertype.getInterfaces()) {
                allMethods.addAll(Arrays.asList(getMethods(itf)));
            }
            supertype = supertype.getSuperclass();
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }

    private String generateListenerMethods(Class<?> listenerType) {
        StringBuilder methodDeclarations = new StringBuilder();
        Method[] methods = getMethods(listenerType);
        for (int m = 0; m < methods.length; m++) {
            Method method = methods[m];
            String methodName = method.getName();
            if (m > 0) {
                methodDeclarations.append("\n");
            }
            methodDeclarations.append("    public void ").append(methodName).append("(");
            methodDeclarations.append(getParameterTypesAndNames(method));
            methodDeclarations.append(") {\n");

            // codigo
            for (char object = 'a'; object <= 'b'; object++) {
                methodDeclarations.append("        ").append(object).append(".").append(methodName).append("(");
                methodDeclarations.append(getParameterNames(method));
                methodDeclarations.append(");\n");
            }
            methodDeclarations.append("    }\n");
        }
        return methodDeclarations.toString();
    }

    private String getPackageName(String fullClassName) {
        StringBuilder packageDeclaration = new StringBuilder();
        int index = fullClassName.lastIndexOf('.');
        if (index > 0) {
            packageDeclaration.append("package ").append(fullClassName.substring(0, index)).append(";");
        }
        return packageDeclaration.toString();
    }

    private String getParameterNames(Method method) {
        StringBuilder text = new StringBuilder();
        Class<?>[] params = method.getParameterTypes();
        for (int p = 0; p < params.length; p++) {
            Class<?> type = params[p];
            if (p > 0) {
                text.append(", ");
            }
            String name = type.getSimpleName();
            text.append(name.toLowerCase());
            if (params.length > 1) {
                text.append(p);
            }
        }
        return text.toString();
    }

    private String getParameterTypesAndNames(Method method) {
        StringBuilder text = new StringBuilder();
        Class<?>[] params = method.getParameterTypes();
        for (int p = 0; p < params.length; p++) {
            Class<?> type = params[p];
            if (p > 0) {
                text.append(", ");
            }
            String name = type.getSimpleName();
            text.append(name).append(" ").append(name.toLowerCase());
            if (params.length > 1) {
                text.append(p);
            }
        }
        return text.toString();
    }

    private String loadTemplate() throws IOException {
        Class<? extends MulticasterGenerator> type = getClass();
        String file = "/" + type.getPackage().getName().replace('.', '/') + "/multicaster.template";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = type.getResourceAsStream(file);
        byte[] buffer = new byte[4096];
        for (int read = -1; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) {}
        return new String(out.toByteArray());
    }
}
