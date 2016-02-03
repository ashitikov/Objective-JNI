/*
 * Copyright 2016 Alexander Shitikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.objective.jni.utils;

import org.apache.commons.bcel6.classfile.*;
import org.apache.commons.bcel6.generic.ArrayType;
import org.apache.commons.bcel6.generic.BasicType;
import org.apache.commons.bcel6.generic.Type;
import org.apache.commons.bcel6.util.ClassPath;
import ru.objective.jni.annotations.OJNIExclude;
import ru.objective.jni.annotations.OJNIExportName;
import ru.objective.jni.constants.Constants;
import ru.objective.jni.exceptions.BadParsingException;
import sun.reflect.annotation.AnnotationType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Created by ashitikov on 03.12.15.
 */
public class Utils {

    public static String getDottedClassName(String className) {
        String result = className.replace(Constants.CLASS_SUFFIX, "");

        result = result.replace('/', '.');
        result = result.replace('\\', '.');

        return result;
    }

    public static String getSlashedClassName(String className) {
        String result = className.replace('.', '/');

        return result;
    }

    public static String getShortClassName(String packageName, String className) {
        String result = className.replace(packageName, "");

        return result.replace(".", "");
    }

    public static String getShortClassName(String className){
        String[] splitted = className.split("\\.");

        if (splitted == null || splitted.length == 0)
            return null;

        return splitted[splitted.length - 1];
    }

    public static String getBasicType(String className) {
        return className.replaceAll("\\[|\\]", "");
    }

    public static HashSet<Method> getOverloadedMethods(Method[] methods) {
        HashSet<Method> result = new HashSet<>(methods.length);

        for (Method method : methods) {

            if (Utils.getMethodExportInfo(method) == null)
                continue;

            for (Method foundMethod : methods) {

                if (Utils.getMethodExportInfo(foundMethod) == null)
                    continue;

                if (method.equals(foundMethod))
                    continue;

                if (method.getName().equals(foundMethod.getName()) &&
                        method.getArgumentTypes().length == foundMethod.getArgumentTypes().length) {
                    result.add(method);
                    break;
                }
            }
        }

        return result;
    }

    public static boolean isClassNameExcluded(String className, String[] excludes, String[] packages) {

        String basicClassName = getBasicType(className);

        if (excludes != null) {
            for (String excluded : excludes) {
                if (basicClassName.equals(excluded))
                    return true;
            }
        }

        if (packages != null) {
            for (String excluded : packages) {
                if (basicClassName.startsWith(excluded + "."))
                    return true;
            }
        }

        return false;
    }

    public static boolean isExportClass(JavaClass javaClass, String[] excludes, String[] excludedPackages) throws ClassNotFoundException {
        if (javaClass.isAnonymous() || javaClass.isAnnotation() || javaClass.isSynthetic())
            return false;

        if (isClassNameExcluded(javaClass.getClassName(), excludes, excludedPackages))
            return false;

        try {
            JavaClass[] interfaces = javaClass.getAllInterfaces();
            JavaClass[] supers = javaClass.getSuperClasses();

            for (JavaClass superClass : supers) {
                if (isClassNameExcluded(superClass.getClassName(), excludes, excludedPackages))
                    return false;
            }

            for (JavaClass superInterface : interfaces) {
                if (isClassNameExcluded(superInterface.getClassName(), excludes, excludedPackages))
                    return false;
            }
        } catch (ClassNotFoundException cnf) {
            System.out.println();
            System.out.println("WARNING! One of superclass or interface of class " + javaClass.getClassName() +
                    " does not included in classpath and will skip. Reason: " + cnf.getLocalizedMessage());

            return false; // ignore classes that does not included in classpath
        }

        return true;
    }

    public static String[] getContainedExportClasses(JarFile jarFile, String[] excludes, String[] excludedPackages) throws Exception {
        Enumeration<JarEntry> entries = jarFile.entries();

        ArrayList<String> containedClasses = new ArrayList<>();

        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String entryName = entry.getName();

            if (!entry.isDirectory() && entryName.endsWith(Constants.CLASS_SUFFIX)) {
                // check this is export class
                String className = getClassNameFromClassFileName(entryName);
                JavaClass parsed = OJNIClassLoader.getInstance().loadClass(className);

                if (!isExportClass(parsed, excludes, excludedPackages))
                    continue;

                containedClasses.add(entryName);
            }
        }

        if (containedClasses.size() == 0)
            throw new BadParsingException("Error parsing specified jar file" + jarFile.getName());

        String[] result = new String[containedClasses.size()];

        containedClasses.toArray(result);

        return result;
    }

    public static String getClassNameFromClassFileName(String classFileName) {
        return getDottedClassName(classFileName).replace(".class", "");
    }

    public static boolean isOccupiedWord(String word) {
        for (String occupied : Constants.OCCUPIED_OBJC_WORDS) {
            if (word.equals(occupied)) {
                return true;
            }
        }

        return false;
    }

    public static String getFieldExportName(Field field) {
        if (!field.isPublic() || field.isSynthetic())
            return null;

        AnnotationEntry[] entries = field.getAnnotationEntries();

        for (AnnotationEntry annotationEntry : entries) {
            String translated = Signature.translate(annotationEntry.getAnnotationType());

            if (translated.equals(OJNIExclude.class.getName()))
                return null;

            if (translated.equals(OJNIExportName.class.getName())) {

                ElementValuePair[] elementValuePairs = annotationEntry.getElementValuePairs();

                AnnotationType annotationType = AnnotationType.getInstance(OJNIExportName.class);

                for (ElementValuePair elementValuePair : elementValuePairs) {

                    if (annotationType.memberTypes().get(elementValuePair.getNameString()) != null) {
                        return elementValuePair.getValue().stringifyValue();
                    }
                }
            }
        }

        return field.getName();
    }

    public static MethodExportInfo getMethodExportInfo(Method method) {
        MethodExportInfo result = new MethodExportInfo();

        if (!method.isPublic() || method.isAnnotation() || method.isSynthetic())
            return result;

        String name = method.getName();

        AnnotationEntry[] entries = method.getAnnotationEntries();

        for (AnnotationEntry annotationEntry : entries) {
            String translated = Signature.translate(annotationEntry.getAnnotationType());

            if (translated.equals(OJNIExclude.class.getName()))
                return result;

            if (translated.equals(OJNIExportName.class.getName())) {
                result.isCustom = true;

                ElementValuePair[] elementValuePairs = annotationEntry.getElementValuePairs();

                AnnotationType annotationType = AnnotationType.getInstance(OJNIExportName.class);

                for (ElementValuePair elementValuePair : elementValuePairs) {

                    if (annotationType.memberTypes().get(elementValuePair.getNameString()) != null) {
                        name = elementValuePair.getValue().stringifyValue();
                    }
                }
            }
        }

        result.name = name;

        return result;
    }

    public static <T> T[] mergeUniqueArray(T[] arr1, T[] arr2) {
        HashSet<T> result = new HashSet<>();

        if (arr1 == null) {
            return arr2;
        } else if (arr2 == null)
            return arr1;

        for (T obj : arr1) {
            result.add(obj);
        }

        for (T obj : arr2) {
            result.add(obj);
        }

        return result.toArray((T[])Array.newInstance(arr1.getClass().getComponentType(), result.size()));
    }


    public static ArrayList<String> getMethodNonPrimitiveDependencies(Method method) {
        Type returnType = basicTypeFromArrayType(method.getReturnType());
        Type[] argumentTypes = method.getArgumentTypes();

        ArrayList<String> dependencies = new ArrayList<>();

        for (Type argumentType : argumentTypes) {
            argumentType = basicTypeFromArrayType(argumentType);
            if (!isPrimitive(argumentType)) {
                dependencies.add(argumentType.toString());
            }
        }

        if (!isPrimitive(returnType))
            dependencies.add(returnType.toString());

        if (dependencies.size() == 0)
            return null;

        return dependencies;
    }

    public static Type basicTypeFromArrayType(Type type) {
        if (isArrayType(type)) {
            return ((ArrayType)type).getBasicType();
        }

        return type;
    }

    public static boolean isPrimitive(Type type) {
        try {
            BasicType.getType(basicTypeFromArrayType(type).getType());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ClassPath[] classPathsFromStrings(String[] classPaths) {
        ClassPath[] result = new ClassPath[classPaths.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = new ClassPath(classPaths[i]);
        }

        return result;
    }

    public static boolean isArrayType(Type type) {
        return type.getClass() == ArrayType.class;
    }

    public static boolean isConstructor(Method method) {
        return (method.getName().equals("<init>"));
    }

    public static boolean isOBJCSystemClass(String className) {
        return className.equals(Constants.OBJC_SYSTEM_CLASS);
    }

    public static boolean isJavaSystemClass(String className) {
        return className.equals(Constants.JAVA_SYSTEM_CLASS);
    }

    public static String getImportTemplate(String importName) {
        return Constants.TEMPLATE_IMPORT.replace(Constants.TEMPLATE_IMPORT_KEY, importName) + System.lineSeparator();
    }

    public static String getForwardDeclarationTemplate(String importName) {
        return Constants.TEMPLATE_FORWARD_DECLARATION.replace(Constants.TEMPLATE_FORWARD_DECLARATION_KEY, importName) + System.lineSeparator();
    }

    public static String getForwardInterfaceDeclarationTemplate(String importName) {
        return Constants.TEMPLATE_FORWARD_INTERFACE_DECLARATION.replace(Constants.TEMPLATE_FORWARD_INTERFACE_DECLARATION_KEY, importName) + System.lineSeparator();
    }
}
