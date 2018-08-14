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
    private static final String TAG_MULTICASTER_METHODS = "${multicaster.methods}";
    private static final String TAG_LISTENER_METHODS = "${listener.methods}";

    public String generate(String fullClassName, String fullListenerName) throws Exception {
        return generate(fullClassName, new String[] { fullListenerName });
    }

    public String generate(String fullClassName, String... fullListenerNames) throws Exception {
        if (fullClassName == null || fullListenerNames == null) {
            throw new IllegalArgumentException("null");
        }
        int count = fullListenerNames.length;
        if (count < 1) {
            throw new IllegalArgumentException("At least one listener interface must be specified!");
        }
        Class<?>[] listenerTypes = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            listenerTypes[i] = Class.forName(fullListenerNames[i].trim());
            if (!listenerTypes[i].isInterface()) {
                throw new IllegalArgumentException(listenerTypes[i].getName() + " is not a interface!");
            }
        }
        String javaCode = loadTemplate();
        javaCode = javaCode.replace(TAG_CLASS_PACKAGE, generatePackage(fullClassName));
        javaCode = javaCode.replace(TAG_CLASS_IMPORTS, generateImports(listenerTypes));
        javaCode = javaCode.replace(TAG_CLASS_NAME, generateClassName(fullClassName));
        javaCode = javaCode.replace(TAG_LISTENER_NAME, generateImplements(listenerTypes));
        javaCode = javaCode.replace(TAG_MULTICASTER_METHODS, generateMulticasterMethods(listenerTypes));
        javaCode = javaCode.replace(TAG_LISTENER_METHODS, generateListenerMethods(listenerTypes));
        return javaCode.trim();
    }

    private String generateClassName(String fullClassName) {
        int index = fullClassName.lastIndexOf('.');
        if (index > 0) {
            return fullClassName.substring(index + 1);
        }
        return fullClassName;
    }

    private String generateImplementedListenerMethod(String typeName, Method method) {
        StringBuilder text = new StringBuilder();
        String methodName = method.getName();

        text.append("    @Override\n");
        text.append("    public void ").append(methodName).append("(");
        text.append(getParameterTypesAndNames(method));
        text.append(") {\n");

        for (char variable = 'a'; variable <= 'b'; variable++) {
            text.append("        ((").append(typeName).append(") ").append(variable).append(").").append(methodName).append("(");
            text.append(getParameterNames(method));
            text.append(");\n");
        }
        text.append("    }");
        return text.toString();
    }

    private String generateImplements(Class<?>[] listenerTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> implemented = new TreeSet<>();
        for (int i = 0; i < listenerTypes.length; i++) {
            String name = listenerTypes[i].getSimpleName();
            if (implemented.add(name)) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(name);
            }
        }
        return text.toString();
    }

    private String generateImports(Class<?>[] listenerTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> imported = new TreeSet<>();
        for (int i = 0; i < listenerTypes.length; i++) {
            Class<?> listenerType = listenerTypes[i];
            String name = listenerType.getName();
            if (imported.add(name)) {
                if (i > 0) {
                    text.append("\n");
                }
                text.append("import ").append(name).append(";");
            }
            for (Method method : getMethods(listenerType)) {
                for (Class<?> type : method.getParameterTypes()) {
                    name = type.getName();
                    if (imported.add(name)) {
                        text.append("\nimport ").append(name).append(";");
                    }
                }
            }
        }
        return text.toString();
    }

    private String generateListenerMethods(Class<?>[] listenerTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> declared = new TreeSet<>();
        for (int i = 0; i < listenerTypes.length; i++) {
            Class<?> listenerType = listenerTypes[i];
            String typeName = listenerType.getSimpleName();
            Method[] methods = getMethods(listenerType);
            for (int m = 0; m < methods.length; m++) {
                String method = generateImplementedListenerMethod(typeName, methods[m]);
                if (declared.add(method)) {
                    if (declared.size() > 1) {
                        text.append("\n\n");
                    }
                    text.append(method);
                }
            }
        }
        return text.toString();
    }

    private String generateMulticasterAddMethod(String name) {
        StringBuilder text = new StringBuilder();
        text.append("    public static ").append(name).append(" add")
            .append("(").append(name).append(" existingListener, ").append(name).append(" listenerToAdd) {\n");
        text.append("        return (").append(name).append(") addInternal(existingListener, listenerToAdd);\n");
        text.append("    }");
        return text.toString();
    }

    private CharSequence generateMulticasterMethods(Class<?>[] listenerTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> declared = new TreeSet<>();
        for (int i = 0; i < listenerTypes.length; i++) {
            Class<?> listenerType = listenerTypes[i];
            String name = listenerType.getSimpleName();
            String method = generateMulticasterAddMethod(name);
            if (declared.add(method)) {
                if (declared.size() > 1) {
                    text.append("\n\n");
                }
                text.append(method);
                text.append("\n");
            }
            method = generateMulticasterRemoveMethod(name);
            if (declared.add(method)) {
                if (declared.size() > 1) {
                    text.append("\n");
                }
                text.append(method);
            }
        }
        return text.toString();
    }

    private String generateMulticasterRemoveMethod(String name) {
        StringBuilder text = new StringBuilder();
        text.append("    public static ").append(name).append(" remove")
            .append("(").append(name).append(" existingListener, ").append(name).append(" listenerToRemove) {\n");
        text.append("        return (").append(name).append(") removeInternal(existingListener, listenerToRemove);\n");
        text.append("    }");
        return text.toString();
    }

    private String generatePackage(String fullClassName) {
        StringBuilder packageDeclaration = new StringBuilder();
        int index = fullClassName.lastIndexOf('.');
        if (index > 0) {
            packageDeclaration.append("package ").append(fullClassName.substring(0, index)).append(";");
        }
        return packageDeclaration.toString();
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

    private String getParameterNames(Method method) {
        StringBuilder text = new StringBuilder();
        Class<?>[] params = method.getParameterTypes();
        for (int p = 0; p < params.length; p++) {
            Class<?> type = params[p];
            if (p > 0) {
                text.append(", ");
            }
            String typeName = type.getSimpleName();
            String paramName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
            text.append(paramName);
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
            String typeName = type.getSimpleName();
            String paramName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
            text.append(typeName).append(" ").append(paramName);
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
