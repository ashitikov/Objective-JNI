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

import org.apache.commons.bcel6.classfile.*;
import org.apache.commons.bcel6.generic.ArrayType;
import org.apache.commons.bcel6.generic.Type;
import org.apache.commons.lang3.StringUtils;
import ru.objective.jni.utils.MethodExportInfo;
import ru.objective.jni.utils.OJNIClassLoader;
import ru.objective.jni.utils.Utils;
import ru.objective.jni.constants.Constants;
import ru.objective.jni.tasks.types.PrimitiveTypeConverter;

import java.util.HashSet;

/**
 * Created by ashitikov on 04.12.15.
 */
public abstract class AbstractBuilder {
    protected JavaClass javaClass;
    protected String prefix;
    protected String[] excludes;
    protected String[] excludedPackages;

    protected abstract void build(JavaClass javaClass) throws Exception;

    protected JavaClass getJavaClass() {
        return javaClass;
    }

    protected String getPrefix() {
        return prefix;
    }

    public AbstractBuilder(JavaClass javaClass, String prefix, String[] excludes, String[] excludedPackages) throws Exception {
        this.javaClass = javaClass;
        this.prefix = prefix;
        this.excludes = excludes;
        this.excludedPackages = excludedPackages;

        build(javaClass);

    }

    public JavaClass[] getInterfaces() {
        try {
            return javaClass.getInterfaces();
        }
        catch (Exception e) {
            return null;
        }
    }

    public JavaClass getSuperClass() {
        try {
            return javaClass.getSuperClass();
        }
        catch (Exception e) {
            return null;
        }
    }

    protected String getHeaderDeclarationField(String name, Field field, boolean setter) {
        StringBuilder builder = new StringBuilder();

        String returnType = Utility.signatureToString(field.getSignature());

        returnType = PrimitiveTypeConverter.convertToOBJCType(Utils.getBasicType(returnType));

        if (!Utils.isPrimitive(field.getType())) {
            JavaClass typeJavaClass = OJNIClassLoader.getInstance().loadClass(field.getType().toString());

            if (typeJavaClass != null && typeJavaClass.isInterface())
                returnType = "id <" + getPrefix() + Utils.getShortClassName(returnType) + ">";
            else
                returnType = getPrefix() + Utils.getShortClassName(returnType) + " *";
        }

        if (Utils.isArrayType(field.getType()))
            returnType = getStringArrayType(returnType, (ArrayType)field.getType());

        String declSign = (field.isStatic() ? "+" : "-")+" ";

        String resultType = PrimitiveTypeConverter.convertToOBJCType(returnType);

        if (setter) {
            if (!field.isFinal()) {
                //generate set
                builder.append(declSign).append("(void)setProperty_").append(field.getName()).
                        append(":(").append(resultType).
                        append(")property_").append(field.getName()).append(";");
            }
        } else {
            //generate get
            builder.append(declSign);
            builder.append("(").append(resultType).append(")property_").
                    append(field.getName()).append(";");
        }

        return builder.toString();
    }

    protected String getHeaderDeclarationMethod(MethodExportInfo info, Method method, boolean overloaded) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] argumentTypes = Utility.methodSignatureArgumentTypes(method.getSignature());
        String name = info.name;


        String methodReturnType = null;

        if (Utils.isConstructor(method)) {
            methodReturnType = "instancetype";

            if (name.equals("<init>"))
                name = "init";
        }
        else {
            if (Utils.isOccupiedWord(name)){
                name = "_" + name;
            }

            methodReturnType = Utility.methodSignatureReturnType(method.getSignature()).toString();

            methodReturnType = PrimitiveTypeConverter.convertToOBJCType(Utils.getBasicType(methodReturnType));

            if (!Utils.isPrimitive(method.getReturnType())) {
                JavaClass typeJavaClass = OJNIClassLoader.getInstance().loadClass(method.getReturnType().toString());

                if (typeJavaClass != null && typeJavaClass.isInterface())
                    methodReturnType = "id <" + getPrefix() + Utils.getShortClassName(methodReturnType) + ">";
                else
                    methodReturnType = getPrefix() + Utils.getShortClassName(methodReturnType) + " *";
            }

            if (Utils.isArrayType(method.getReturnType()))
                methodReturnType = getStringArrayType(methodReturnType, (ArrayType)method.getReturnType());
        }
        stringBuilder.append((method.isStatic() ? "+" : "-")+" " + "("+PrimitiveTypeConverter.convertToOBJCType(methodReturnType)+")");

        String[] nameParameters = name.split(":");

        if (info.isCustom)
            overloaded = false;

        LocalVariableTable table = method.getLocalVariableTable();

        if (argumentTypes.length == 0)
            stringBuilder.append(name);

        for (int i = 0, var_index = (method.isStatic() ? 0 : 1); i < argumentTypes.length; i++, var_index++) {
            Type javaType = method.getArgumentTypes()[i];
            String type = argumentTypes[i];

            String variable_name = "";
            if (table == null) {
                variable_name = "arg" +var_index;
            } else {
                LocalVariable lv = table.getLocalVariable(var_index, 0);
                if (lv != null) {
                    variable_name = lv.getName();
                }
            }

            if (javaType.equals(Type.LONG) || javaType.equals(Type.DOUBLE))
                var_index++;

            String nameParameter = (i < nameParameters.length ? nameParameters[i] : variable_name);

            type = Utils.getBasicType(type);

            String overloadedParameter = "";

            if (overloaded) {
                overloadedParameter = "With" + (i == 0 ? StringUtils.capitalize(variable_name) : "")
                        + Utils.getShortClassName(type);/*+ StringUtils.capitalize(utils.getShortClassName(type));*/
            }

            type = PrimitiveTypeConverter.convertToOBJCType(type);

            if (!Utils.isPrimitive(javaType)) {
                JavaClass argTypeJavaClass = OJNIClassLoader.getInstance().loadClass(javaType.toString());

                if (argTypeJavaClass != null && argTypeJavaClass.isInterface())
                    type = "id <" + getPrefix() + Utils.getShortClassName(type) + ">";
                else
                    type = getPrefix() + Utils.getShortClassName(type) + " *";
            }

            if (Utils.isArrayType(javaType)) {
                if (overloaded) {
                    int dimensions = ((ArrayType) javaType).getDimensions();

                    if (dimensions > 1)
                        overloadedParameter += dimensions + "dimArray";
                    else
                        overloadedParameter += "Array";
                }
                type = getStringArrayType(type, (ArrayType) javaType);
            }

            stringBuilder.append(nameParameter + overloadedParameter + ":(" + type + ")" + variable_name + " ");
        }

        stringBuilder.append(";");

        return stringBuilder.toString();
    }

    protected String getStringArrayType(String type, ArrayType javaType) {

        Type iteratorType = javaType;

        int j = 1;

        if (Utils.isPrimitive(iteratorType)) {
            String capitalized = StringUtils.capitalize(type);

            // fix Boolean = Bool conflicts
            if (capitalized.equals("Bool"))
                capitalized = "Boolean";

            type = "OJNIPrimitive" + capitalized + "Array *";
            j++;
        }

        for (; Utils.isArrayType(iteratorType) && j <= ((ArrayType)javaType).getDimensions(); j++) {

            type = "NSArray <" + type + "> *";

            iteratorType = ((ArrayType) iteratorType).getElementType();
        }

        return type;
    }

    protected String getHeaderImportBlock(String superClassName, String[] interfaces, HashSet<String> dependencies, boolean implementation) {
        StringBuilder result = new StringBuilder();

        HashSet<String> importClassNames = new HashSet<>();
        HashSet<String> importInterfaceNames = new HashSet<>();

        if (interfaces != null) {
            for (String interfaceName : interfaces){
                importInterfaceNames.add(interfaceName);
            }
        }

        if (dependencies != null) {
            for (String dependency : dependencies){
                importClassNames.add(dependency);
            }
        }


        if (superClassName != null && !Utils.isOBJCSystemClass(superClassName)) {
            //result.append(utils.getImportTemplate(getPrefix() + utils.getShortClassName(superClassName)));
            importClassNames.add(superClassName);
        }

        for (String importName : importInterfaceNames) {
            result.append(Utils.getImportTemplate(getPrefix() + Utils.getShortClassName(importName)));
        }

        // add environment import
        if (getJavaClass().getClassName().equals("java.lang.Object")) {
            result.append("#import \"OJNIJavaObject.h\"").append(System.lineSeparator());
        }

        for (String importName : importClassNames) {
            JavaClass importedClass = OJNIClassLoader.getInstance().loadClass(importName);

            //if (importName.equals(superClassName) || getJavaClass().isInterface() || (importedClass != null && importedClass.isInterface()))
            if (implementation) {
                if (importedClass != null && importedClass.isInterface())
                    result.append(Utils.getImportTemplate(getPrefix() + Utils.getShortClassName(importName)));
                else
                    result.append(Utils.getImportTemplate(getPrefix() + Utils.getShortClassName(importName)));
            } else {
                if (importName.equals(superClassName))
                    result.append(Utils.getImportTemplate(getPrefix() + Utils.getShortClassName(importName)));
                else if (importedClass != null && importedClass.isInterface())
                    result.append(Utils.getForwardInterfaceDeclarationTemplate(getPrefix() + Utils.getShortClassName(importName)));
                else
                    result.append(Utils.getForwardDeclarationTemplate(getPrefix() + Utils.getShortClassName(importName)));
            }
        }

        if (getJavaClass().isInterface()) {
            result.append(Utils.getForwardInterfaceDeclarationTemplate("OJNIJavaObject"));
        }

        return result.toString();
    }

    protected String getInterfacesBlock(JavaClass[] interfaces) {

        if (interfaces == null || interfaces.length == 0)
            return (getJavaClass().isInterface() ? "<"+ Constants.OBJC_SYSTEM_CLASS+">" : "");

        StringBuilder result = new StringBuilder();

        result.append("<");
        result.append(Constants.OBJC_SYSTEM_CLASS);
        result.append(", ");

        for (int i = 0; i < interfaces.length; i++) {
            JavaClass javaInterface = interfaces[i];

            result.append(getPrefix() + Utils.getShortClassName(javaInterface.getPackageName(), javaInterface.getClassName()));

            if (i + 1 != interfaces.length)
                result.append(", ");
        }

        result.append(">");

        return result.toString();
    }


    public abstract String getHeader();
    public abstract String getImplementation();
    public abstract HashSet<String> getDependencies();
}
