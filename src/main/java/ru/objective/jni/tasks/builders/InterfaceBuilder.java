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

package ru.objective.jni.tasks.builders;

import org.apache.commons.bcel6.classfile.JavaClass;
import org.apache.commons.bcel6.classfile.Method;
import ru.objective.jni.Utils.MethodExportInfo;
import ru.objective.jni.Utils.ResourceList;
import ru.objective.jni.Utils.Utils;
import ru.objective.jni.constants.Constants;
import ru.objective.jni.exceptions.BadParsingException;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by ashitikov on 04.12.15.
 */
public class InterfaceBuilder extends AbstractBuilder {

    protected String header;
    protected String implementation;
    protected HashSet<String> dependencies;

    public InterfaceBuilder(JavaClass javaClass, String prefix, String[] excludes) throws Exception {
        super(javaClass, prefix, excludes);
    }

    @Override
    protected void build(JavaClass javaClass) throws Exception {

        if (!javaClass.isInterface())
            throw new BadParsingException("Cannot build interface from class " + javaClass.toString());

        if (Utils.isExportClass(javaClass, excludes)) {

            StringBuilder declBuilder = new StringBuilder();

            Method[] methods = javaClass.getMethods();

            JavaClass[] interfaces = getInterfaces();
            String[] classInterfacesNames = null;

            if (interfaces != null) {
                ArrayList<String> resultInterfaces = new ArrayList<>(interfaces.length);

                for (JavaClass javaInterface : interfaces) {
                    resultInterfaces.add(Utils.getShortClassName(javaInterface.getClassName()));
                }

                if (resultInterfaces.size() > 0) {
                    classInterfacesNames = new String[resultInterfaces.size()];

                    resultInterfaces.toArray(classInterfacesNames);
                }
            }

            HashSet<String> methodDependencies = new HashSet<>();

            HashSet<Method> overloadedMethods = Utils.getOverloadedMethods(methods);

            for (Method method : methods) {

                MethodExportInfo info = Utils.getMethodExportInfo(method);
                String name = info.name;

                if (name == null)
                    continue;

                ArrayList<String> deps = Utils.getMethodNonPrimitiveDependencies(method);

                if (deps != null) {
                    boolean found = false;
                    for (String dependency : deps) {
                        // skip method if excluded
                        if (Utils.isClassNameExcluded(dependency, excludes)) {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;

                    methodDependencies.addAll(deps);
                }

                String decl = getHeaderDeclarationMethod(info, method, overloadedMethods.contains(method));
                declBuilder.append(decl);
                declBuilder.append(System.lineSeparator());
            }

            if (methodDependencies.size() > 0) {
                dependencies = methodDependencies;
            }

            String packageName = javaClass.getPackageName();
            String shortClassName = Utils.getShortClassName(packageName, javaClass.getClassName());
            String interfacesBlock = getInterfacesBlock(interfaces);
            String headerImportBlock = getHeaderImportBlock(null, classInterfacesNames, dependencies, false);

            generate(packageName, shortClassName, headerImportBlock, interfacesBlock, declBuilder.toString());
        }
    }

    public void generate(String packageName, String interfaceName, String importBlock,
                                  String interfaceImplemensBlock, String declarationBlock) throws Exception {
        String headerTemplate = ResourceList.getStringContentFromResource(Constants.TEMPLATE_INTERFACE_FILENAME);


        headerTemplate = headerTemplate.replace(Constants.INTERFACE_NAME, getPrefix()+interfaceName);
        headerTemplate = headerTemplate.replace(Constants.INTERFACES_IMPLEMENTS, interfaceImplemensBlock);
        headerTemplate = headerTemplate.replace(Constants.DECLARATION_BLOCK, declarationBlock);
        headerTemplate = headerTemplate.replace(Constants.IMPORT_BLOCK, importBlock);


        header = headerTemplate;
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public String getImplementation() {
        return implementation;
    }

    @Override
    public HashSet<String> getDependencies() {
        return dependencies;
    }
}
