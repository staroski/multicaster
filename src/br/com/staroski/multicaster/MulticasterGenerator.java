package br.com.staroski.multicaster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MulticasterGenerator {

    private static final String TAG_CLASS_PACKAGE = "${class.package}";
    private static final String TAG_CLASS_IMPORTS = "${class.imports}";
    private static final String TAG_CLASS_NAME = "${class.name}";
    private static final String TAG_INTERFACES_NAMES = "${interfaces.names}";
    private static final String TAG_MULTICASTER_METHODS = "${multicaster.methods}";
    private static final String TAG_INTERFACES_METHODS = "${interfaces.methods}";

    public String generate(String fullClassName, String fullInterfaceName) throws Exception {
        return generate(fullClassName, new String[] { fullInterfaceName });
    }

    public String generate(String fullClassName, String... fullInterfacesNames) throws Exception {
        if (fullClassName == null || fullInterfacesNames == null) {
            throw new IllegalArgumentException("null");
        }
        int count = fullInterfacesNames.length;
        if (count < 1) {
            throw new IllegalArgumentException("At least one interface must be specified!");
        }
        Class<?>[] interfacesTypes = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            interfacesTypes[i] = Class.forName(fullInterfacesNames[i].trim());
            if (!interfacesTypes[i].isInterface()) {
                throw new IllegalArgumentException(interfacesTypes[i].getName() + " is not a interface!");
            }
        }
        String javaCode = loadTemplate();
        javaCode = javaCode.replace(TAG_CLASS_PACKAGE, generatePackage(fullClassName));
        javaCode = javaCode.replace(TAG_CLASS_IMPORTS, generateImports(interfacesTypes));
        javaCode = javaCode.replace(TAG_CLASS_NAME, generateClassName(fullClassName));
        javaCode = javaCode.replace(TAG_INTERFACES_NAMES, generateImplements(interfacesTypes));
        javaCode = javaCode.replace(TAG_MULTICASTER_METHODS, generateMulticasterMethods(interfacesTypes));
        javaCode = javaCode.replace(TAG_INTERFACES_METHODS, generateInterfaceMethods(interfacesTypes));
        return javaCode.trim();
    }

    private String generateClassName(String fullClassName) {
        int index = fullClassName.lastIndexOf('.');
        if (index > 0) {
            return fullClassName.substring(index + 1);
        }
        return fullClassName;
    }

    private String generateImplements(Class<?>[] interfacesTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> implemented = new TreeSet<>();
        for (int i = 0; i < interfacesTypes.length; i++) {
            String name = interfacesTypes[i].getSimpleName();
            if (implemented.add(name)) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(name);
            }
        }
        return text.toString();
    }

    private String generateImports(Class<?>[] interfacesTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> imported = new TreeSet<>();
        for (int i = 0; i < interfacesTypes.length; i++) {
            Class<?> interfaceType = interfacesTypes[i];
            String name = interfaceType.getName();
            if (imported.add(name)) {
                if (i > 0) {
                    text.append("\n");
                }
                text.append("import ").append(name).append(";");
            }
            for (Method method : getMethods(interfaceType)) {
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

    private String generateInterfaceMethod(String typeName, Method method) {
        StringBuilder text = new StringBuilder();
        String methodName = method.getName();
        String returnType = method.getReturnType().getSimpleName();

        text.append("    @Override\n");
        text.append("    public ").append(returnType).append(" ").append(methodName).append("(");
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

    private String generateInterfaceMethods(Class<?>[] interfacesTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> declared = new TreeSet<>();
        for (int i = 0; i < interfacesTypes.length; i++) {
            Class<?> interfaceType = interfacesTypes[i];
            String typeName = interfaceType.getSimpleName();
            Method[] methods = getMethods(interfaceType);
            for (int m = 0; m < methods.length; m++) {
                String method = generateInterfaceMethod(typeName, methods[m]);
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

    private String generateMulticasterMethodAdd(Class<?> interfaceType) {
        String name = interfaceType.getSimpleName();
        StringBuilder text = new StringBuilder();
        text.append("    public static ").append(name).append(" add")
            .append("(").append(name).append(" existing").append(name).append(", ").append(name).append(" ").append(toVariableName(name)).append("ToAdd) {\n");
        text.append("        return (").append(name).append(") addInternal(existing").append(name).append(", ").append(toVariableName(name)).append("ToAdd);\n");
        text.append("    }");
        return text.toString();
    }

    private String generateMulticasterMethodRemove(Class<?> interfaceType) {
        String name = interfaceType.getSimpleName();
        StringBuilder text = new StringBuilder();
        text.append("    public static ").append(name).append(" remove")
            .append("(").append(name).append(" existing").append(name).append(", ").append(name).append(" ").append(toVariableName(name)).append("ToRemove) {\n");
        text.append("        return (").append(name).append(") removeInternal(existing").append(name).append(", ").append(toVariableName(name)).append("ToRemove);\n");
        text.append("    }");
        return text.toString();
    }

    private CharSequence generateMulticasterMethods(Class<?>[] interfacesTypes) {
        StringBuilder text = new StringBuilder();
        Set<String> declared = new TreeSet<>();
        for (int i = 0; i < interfacesTypes.length; i++) {
            Class<?> interfaceType = interfacesTypes[i];
            String method = generateMulticasterMethodAdd(interfaceType);
            if (declared.add(method)) {
                if (declared.size() > 1) {
                    text.append("\n\n");
                }
                text.append(method);
                text.append("\n");
            }
            method = generateMulticasterMethodRemove(interfaceType);
            if (declared.add(method)) {
                if (declared.size() > 1) {
                    text.append("\n");
                }
                text.append(method);
            }
        }
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
            for (Method method : supertype.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    allMethods.add(method);
                }
            }
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
            text.append(toVariableName(typeName));
            if (params.length > 1) {
                text.append(p + 1);
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
            String paramName = toVariableName(typeName);
            text.append(typeName).append(" ").append(paramName);
            if (params.length > 1) {
                text.append(p + 1);
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

    private String toVariableName(String typeName) {
        String paramName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        return paramName;
    }
}
