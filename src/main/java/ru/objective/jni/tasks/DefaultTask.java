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

package ru.objective.jni.tasks;

import org.apache.commons.bcel6.classfile.JavaClass;
import ru.objective.jni.Utils.OJNIClassLoader;
import ru.objective.jni.Utils.ResourceList;
import ru.objective.jni.Utils.Utils;
import ru.objective.jni.constants.Constants;
import ru.objective.jni.tasks.builders.AbstractBuilder;
import ru.objective.jni.tasks.builders.ClassBuilder;
import ru.objective.jni.tasks.builders.InterfaceBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.jar.JarFile;

/**
 * Created by ashitikov on 29.11.15.
 */
public class DefaultTask implements ITask {

    private String[] classPaths;
    private String[] excludes;
    private String[] classes;
    private String[] packages;

    private String output;
    private String prefix;

    private HashSet<String> globalDependencies = new HashSet<>();
    private ArrayList<String> generatedClasses = new ArrayList<>();

    public DefaultTask(String[] classPaths, String[] excludes, String[] classes, String[] packages, String output, String prefix) {
        this.classPaths = classPaths;
        this.excludes = excludes;
        this.classes = classes;
        this.packages = packages;
        this.output = output;
        this.prefix = prefix;
    }

    @Override
    public void run() throws Exception {
        parseClasses();
    }

    private void parseClass(JavaClass parsedClass) throws Exception {
        if (generatedClasses.indexOf(parsedClass.getClassName()) == -1) {
            generatedClasses.add(parsedClass.getClassName());
        } else {
            return;
        }

        AbstractBuilder builder = null;

        if (parsedClass.isInterface()) {
            builder = new InterfaceBuilder(parsedClass, prefix, excludes);
        }
        else {
            builder = new ClassBuilder(parsedClass, prefix, excludes);
        }

        JavaClass[] interfaces = builder.getInterfaces();
        JavaClass superClass = builder.getSuperClass();
        HashSet<String> dependencies = builder.getDependencies();

        if (interfaces != null) {
            for (JavaClass javaInterface : interfaces) {
                parseClass(javaInterface);
            }
        }

        if (dependencies != null) {
            for (String dependency : dependencies){
                parseClass(dependency);
            }
        }

        if (superClass != null) {
            parseClass(superClass);
        }

        String header = builder.getHeader();
        String implementation = builder.getImplementation();

        String packageName = parsedClass.getPackageName();

        generate(packageName, Utils.getShortClassName(packageName, parsedClass.getClassName()), header, implementation);
    }

    private void parseClass(String className) throws Exception {
        JavaClass parsedClass = getJavaClass(className);

        if (parsedClass == null)
            throw new NullPointerException("Could not find class " + className + " in specified or system classpaths");

        parseClass(parsedClass);
    }

    private void parseClasses() throws Exception {
        System.out.println("Parsing started...");

        parseClassPaths();
        parsePackages();

        // parse from classes specified in class options
        if (classes != null) {
            for (String cls : classes) {
                System.out.print("Parsing class " + cls);

                parseClass(cls);

                System.out.println(" -- DONE!");
            }
        }

        writeLicense();
        System.out.println("Successful!");
    }

    private void parseClassPaths() throws Exception {
        if (classPaths == null)
            return;

        for (String classPath : classPaths) {
            System.out.print("Parsing classpath " + classPath);

            JarFile jarFile = new JarFile(classPath);

            String[] containedClasses = Utils.getContainedExportClasses(jarFile, excludes);

            for (String entry : containedClasses) {
                parseClass(entry);
            }

            System.out.println(" -- DONE!");
        }
    }

    private void parsePackages() throws Exception {
        if (packages == null)
            return;

        // parse classes from specified package
        for (String pkg : packages) {
            System.out.print("Parsing package " + pkg);

            String[] names = OJNIClassLoader.getInstance().getClassNamesFromPackage(pkg);

            for (String name : names) {
                parseClass(name);
            }

            System.out.println(" -- DONE!");
        }
    }

    private void writeLicense() throws IOException {
        String license = ResourceList.getStringContentFromResource(Constants.TEMPLATE_LICENSE_FILENAME);
        Path path = Paths.get(output + "/" + Constants.TEMPLATE_LICENSE_FILENAME);

        Files.createDirectories(path.getParent());
        Files.write(path, license.getBytes(Charset.defaultCharset()));
    }

    private void generate(String packageName, String objName, String header, String implementation) throws Exception {
        if (header != null) {
            Path headerPath = getWritePath(packageName, prefix+objName, true);

            Files.createDirectories(headerPath.getParent());

            Files.write(headerPath, header.getBytes(Charset.defaultCharset()));
        }

        if (implementation != null) {
            Path implPath = getWritePath(packageName, prefix+objName, false);

            Files.createDirectories(implPath.getParent());

            Files.write(implPath, implementation.getBytes(Charset.defaultCharset()));
        }
    }

    private Path getWritePath(String packageName, String className, boolean isHeader) {

        String packagePath = packageName.replace(".", "/");

        return Paths.get(output + "/"
                + packagePath + "/" + className + (isHeader ? ".h" : ".m"));
    }

    private JavaClass getJavaClass(String className) {
        return OJNIClassLoader.getInstance().loadClass(className);
    }


}
