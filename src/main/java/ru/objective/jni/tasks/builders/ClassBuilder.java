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
import ru.objective.jni.utils.ResourceList;
import ru.objective.jni.utils.Utils;
import ru.objective.jni.constants.Constants;
import ru.objective.jni.exceptions.BadParsingException;
import ru.objective.jni.tasks.types.PrimitiveTypeConverter;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by ashitikov on 04.12.15.
 */
public class ClassBuilder extends AbstractBuilder {

    protected String header;
    protected String implementation;
    protected HashSet<String> dependencies;

    public ClassBuilder(JavaClass javaClass, String prefix, String[] excludes, String[] excludedPackages) throws Exception {
        super(javaClass, prefix, excludes, excludedPackages);
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
    protected void build(JavaClass javaClass) throws Exception {

        if (javaClass.isInterface())
            throw new BadParsingException("Cannot build class from interface " + javaClass.toString());

        if (Utils.isExportClass(javaClass, excludes, excludedPackages)) {

            StringBuilder declBuilder = new StringBuilder();
            StringBuilder implBuilder = new StringBuilder();

            Method[] methods = javaClass.getMethods();
            Field[] fields = javaClass.getFields();

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

            for (Field field : fields) {
                // skip field if excluded
                if (Utils.isClassNameExcluded(field.getType().toString(), excludes, excludedPackages))
                    continue;

                String fieldName = Utils.getFieldExportName(field);

                if (fieldName == null)
                    continue;

                Type basicFieldType = Utils.basicTypeFromArrayType(field.getType());

                if (!Utils.isPrimitive(basicFieldType))
                    methodDependencies.add(basicFieldType.toString());

                String declgetter = getHeaderDeclarationField(fieldName, field, false);
                String declsetter = getHeaderDeclarationField(fieldName, field, true);
                declBuilder.append(declgetter);
                declBuilder.append(System.lineSeparator());
                declBuilder.append(declsetter);
                declBuilder.append(System.lineSeparator());

                String implgetter = getFieldImplementation(field, declgetter, false);
                String implsetter = getFieldImplementation(field, declsetter, true);
                implBuilder.append(implgetter);
                implBuilder.append(System.lineSeparator());
                implBuilder.append(implsetter);
                implBuilder.append(System.lineSeparator());
            }

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
                        if (Utils.isClassNameExcluded(dependency, excludes, excludedPackages)) {
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

                String impl = getMethodImplementation(method, decl);
                implBuilder.append(impl);
                implBuilder.append(System.lineSeparator());
            }

            // add core string methods decls
            if (getJavaClass().getClassName().equals("java.lang.String")) {
                declBuilder.append("- (instancetype)initWithNSString:(NSString *)string;").append(System.lineSeparator()).
                        append("+ (instancetype)stringWithNSString:(NSString *)string;").append(System.lineSeparator()).
                        append("- (NSString *)toNSString;").append(System.lineSeparator());
            }

            if (methodDependencies.size() > 0) {
                dependencies = methodDependencies;
            }

            String packageName = javaClass.getPackageName();
            String shortClassName = Utils.getShortClassName(packageName, javaClass.getClassName());

            String superClassName = Constants.OBJC_SYSTEM_CLASS;
            String headerImportBlock = "";
            String interfacesBlock = (interfaces != null ? getInterfacesBlock(interfaces) : "");

            JavaClass superClass = getSuperClass();


            if (superClass != null && Utils.isExportClass(superClass, excludes, excludedPackages)) {
                superClassName = superClass.getClassName();
            }

            headerImportBlock = getHeaderImportBlock(superClassName, classInterfacesNames, dependencies, false);

            String implImportBlock = "";
            if (headerImportBlock != null && !headerImportBlock.equals(""))
                implImportBlock = getHeaderImportBlock(superClassName, classInterfacesNames, dependencies, true);

            implBuilder.append(getOJNIMethodsImplementations());

                generate(packageName, shortClassName, interfacesBlock,
                        Utils.getShortClassName(superClassName), headerImportBlock,
                        declBuilder.toString(), "", implBuilder.toString(), implImportBlock);
        }
    }

    private String getOJNIMethodsImplementations() {
        StringBuilder builder = new StringBuilder();

        String slashedClassName = Utils.getSlashedClassName(getJavaClass().getClassName());

        builder.append("+ (NSString *)OJNIClassName {").append(System.lineSeparator()).
                append("return @\"").append(slashedClassName).append("\";").append(System.lineSeparator()).
                append("}").append(System.lineSeparator());


        // special case for string
        if (getJavaClass().getClassName().equals("java.lang.String")) {
            builder.append("- (instancetype)initWithNSString:(NSString *)string {\n" +
                    "    return [self initWithJavaObject:[[OJNIEnv sharedEnv] newJavaStringFromString:string utf8Encoding:NO]];\n" +
                    "}\n" +
                    "\n" +
                    "+ (instancetype)stringWithNSString:(NSString *)string {\n" +
                    "    return [[self alloc] initWithNSString:string];\n" +
                    "}\n" +
                    "\n" +
                    "- (NSString *)toNSString {\n" +
                    "    return [[OJNIEnv sharedEnv] newStringFromJavaString:[self javaObject] utf8Encoding:NO];\n" +
                    "}");
        }

        return builder.toString();
    }

    private String getFieldImplementation(Field field, String declaration, boolean setter) {
        StringBuilder builder = new StringBuilder();

        if (setter && field.isFinal())
            return "";

        builder.append(declaration).append(" {").append(System.lineSeparator());

        builder.append("jfieldID fid = [[OJNIMidManager sharedManager] fieldIDFor");
        if (field.isStatic())
            builder.append("Static");
        builder.append("Method:@\""+field.getName()+"\" ");
        builder.append("signature:@\""+field.getSignature()+"\" inClass:self.class];");
        builder.append(System.lineSeparator());

        Type returnType = field.getType();
        String lowerCaseReturnType = (Utils.isPrimitive(returnType) && !Utils.isArrayType(returnType) ?
                returnType.toString() : "object");
        String capitalized = StringUtils.capitalize(lowerCaseReturnType);

        String staticIdentifier = "";
        String selfIdentitifer = "[self javaObject]";

        if (field.isStatic()) {
            staticIdentifier = "Static";
            selfIdentitifer = "[self.class OJNIClass]";
        }

        if (setter) { // setter
            builder.append("[[OJNIEnv sharedEnv] set").append(staticIdentifier).append(capitalized).
                    append("Field:").append(selfIdentitifer).append(" field:fid value:");

            String var_name = "property_" + field.getName();

            if (Utils.isArrayType(returnType)) {
                ArrayType arrayType = (ArrayType)returnType;
                int dimensions = arrayType.getDimensions();
                String typeString = arrayType.getBasicType().toString();
                String capitalizedType = StringUtils.capitalize(typeString);

                // fix Boolean = Bool conflicts
                if (capitalizedType.equals("Bool"))
                    capitalizedType = "Boolean";

                if (arrayType.getDimensions() == 1 && Utils.isPrimitive(arrayType)) {
                    builder.append("[[OJNIEnv sharedEnv] newJava").append(capitalizedType).append("ArrayFromArray:").append(var_name).append("]");
                } else {
                    if (Utils.isPrimitive(arrayType)) {
                        builder.append("[[OJNIEnv sharedEnv] newJavaObjectArrayFromArray:").
                                append(var_name).append(" baseClass:[OJNIPrimitive").append(capitalizedType).
                                append("Array class]").
                                append(" dimensions:").append(dimensions).append("]");

                    } else {
                        JavaClass argTypeJavaClass = OJNIClassLoader.getInstance().loadClass(typeString);

                        String resultClassString = "";
                        if (argTypeJavaClass != null && argTypeJavaClass.isInterface())
                            resultClassString = "@\"" + Utils.getSlashedClassName(typeString) + "\"";
                        else
                            resultClassString = "[" + getPrefix() + Utils.getShortClassName(typeString) + " class]";

                        builder.append("[[OJNIEnv sharedEnv] newJavaObjectArrayFromArray:").
                                append(var_name).append(" baseClass:").
                                append(resultClassString).
                                append(" dimensions:").append(dimensions).append("]");
                    }
                }
            } else {
                if (Utils.isPrimitive(returnType)) {
                    builder.append(var_name);
                } else {
                    builder.append("[").append(var_name).append(" javaObject]");
                }
            }
            builder.append("];");
        } else { // getter
            builder.append("j").append(lowerCaseReturnType).append(" __obj = ").
                    append("[[OJNIEnv sharedEnv] get").append(staticIdentifier).append(capitalized).
                    append("Field:").append(selfIdentitifer).append(" field:fid];").append(System.lineSeparator());

            builder.append(generateReturnObject(field.getType()));
        }

        builder.append(System.lineSeparator()).append("}");

        return builder.toString();
    }

    private String getMethodImplementation(Method method, String declaration) {
        StringBuilder builder = new StringBuilder();

        String vars = generateArgumentString(method);

        builder.append(declaration).append(" {").append(System.lineSeparator());

        builder.append("jmethodID mid = [[OJNIMidManager sharedManager] methodIDFor");
        if (method.isStatic())
            builder.append("Static");
        builder.append("Method:@\""+method.getName()+"\" ");
        builder.append("signature:@\""+method.getSignature()+"\" inClass:self.class];");
        builder.append(System.lineSeparator());

        if (method.getReturnType().equals(Type.VOID)) {
            if (Utils.isConstructor(method)) {
                builder.append("jobject __obj = [[OJNIEnv sharedEnv] newObject:[self.class OJNIClass] method:mid");
                builder.append(vars).append("];").append(System.lineSeparator());
                builder.append("return [super initWithJavaObject:__obj];");

            } else {
                if (method.isStatic()) {
                    builder.append("[[OJNIEnv sharedEnv] callStaticVoidMethodOnClass:[self.class OJNIClass] method:mid");
                } else {
                    builder.append("[[OJNIEnv sharedEnv] callVoidMethodOnObject:[self javaObject] method:mid");
                }

                builder.append(vars).append("];");
            }
        } else {
            builder.append(generateCallMethod(method, vars));
            builder.append(generateReturnObject(method.getReturnType()));
        }

        builder.append(System.lineSeparator()).append("}");

        return builder.toString();
    }

    public String generateCallMethod(Method method, String vars) {
        StringBuilder builder = new StringBuilder();

        Type returnType = method.getReturnType();

        if (Utils.isArrayType(returnType) || !Utils.isPrimitive(returnType)) {
            if (method.isStatic())
                builder.append("jobject __obj = [[OJNIEnv sharedEnv] callStaticObject");
            else
                builder.append("jobject __obj = [[OJNIEnv sharedEnv] callObject");
        } else {
            builder.append("j").append(returnType.toString()).append(" __obj = [[OJNIEnv sharedEnv] call");
            if (method.isStatic())
                builder.append("Static");
            builder.append(StringUtils.capitalize(returnType.toString()));

        }

        if (method.isStatic())
            builder.append("MethodOnClass:[self.class OJNIClass] method:mid");
        else
            builder.append("MethodOnObject:[self javaObject] method:mid");

        builder.append(vars).append("];").append(System.lineSeparator());

        return builder.toString();
    }

    public String generateArgumentString(Method method) {
        StringBuilder builder = new StringBuilder();

        Type[] types = method.getArgumentTypes();
        LocalVariableTable localVariableTable = method.getLocalVariableTable();

        for (int i = 0, var_index = (method.isStatic() ? 0 : 1); i < types.length; i++, var_index++) {
//            if (localVariableTable != null && localVariableTable.getLocalVariable(var_index, 0) == null)
//                System.gc();
            Type type = types[i];
            String var_name = "";
            if (localVariableTable == null) {
                var_name = "arg" +var_index;
            } else {
                LocalVariable lv = localVariableTable.getLocalVariable(var_index, 0);
                if (lv != null) {
                    var_name = lv.getName();
                }
            }

            if (type.equals(Type.LONG) || type.equals(Type.DOUBLE))
                var_index++;

            if (Utils.isOccupiedWord(var_name)){
                var_name = "_" + var_name;
            }

            builder.append(", ");

            if (Utils.isArrayType(type)) {
                ArrayType arrayType = (ArrayType)type;
                int dimensions = arrayType.getDimensions();
                String typeString = arrayType.getBasicType().toString();
                String capitalizedType = StringUtils.capitalize(typeString);

                // fix Boolean = Bool conflicts
                if (capitalizedType.equals("Bool"))
                    capitalizedType = "Boolean";

                if (arrayType.getDimensions() == 1 && Utils.isPrimitive(arrayType)) {
                    //builder.append("[").append(var_name).append(" rawArray]");
                    builder.append("[[OJNIEnv sharedEnv] newJava").append(capitalizedType).append("ArrayFromArray:").append(var_name).append("]");
                } else {
                    if (Utils.isPrimitive(arrayType)) {
                        builder.append("[[OJNIEnv sharedEnv] newJavaObjectArrayFromArray:").
                                append(var_name).append(" baseClass:[OJNIPrimitive").append(capitalizedType).
                                append("Array class]").
                                append(" dimensions:").append(dimensions).append("]");

                    } else {
                        JavaClass argTypeJavaClass = OJNIClassLoader.getInstance().loadClass(typeString);

                        String resultClassString = "";
                        if (argTypeJavaClass != null && argTypeJavaClass.isInterface())
                            resultClassString = "@\"" + Utils.getSlashedClassName(typeString) + "\"";
                        else
                            resultClassString = "[" + getPrefix() + Utils.getShortClassName(typeString) + " class]";

                        builder.append("[[OJNIEnv sharedEnv] newJavaObjectArrayFromArray:").
                                append(var_name).append(" baseClass:").
                                append(resultClassString).
                                append(" dimensions:").append(dimensions).append("]");
                    }
                }
            } else {
                if (Utils.isPrimitive(type)) {
                    builder.append(var_name);
                } else {
                    builder.append("[").append(var_name).append(" javaObject]");
                }
            }
        }

        return builder.toString();
    }

    public String generateReturnObject(Type returnType) {
        StringBuilder builder = new StringBuilder();

        builder.append("return ");

        if (Utils.isArrayType(returnType)) {
            ArrayType arrReturnType = (ArrayType) returnType;
            int dimensions = arrReturnType.getDimensions();
            String capitalizedType = StringUtils.capitalize(arrReturnType.getBasicType().toString());

            // fix Boolean = Bool conflicts
            if (capitalizedType.equals("Bool"))
                capitalizedType = "Boolean";

            if (Utils.isPrimitive(arrReturnType)) {
                if (dimensions == 1) {
                    builder.append("[[OJNIEnv sharedEnv] primitive").
                            append(capitalizedType).
                            append("ArrayFromJavaArray:__obj];");
                } else {
                    builder.append("[[OJNIEnv sharedEnv] newArrayFromJavaObjectArray:__obj baseClass:[OJNIPrimitive").
                            append(capitalizedType).
                            append("Array class] classPrefix:@\"").append(getPrefix()).
                            append("\" dimensions:").append(dimensions).append("];");
                }
            } else {
                builder.append("[[OJNIEnv sharedEnv] newArrayFromJavaObjectArray:__obj baseClass:[OJNIJavaObject class] classPrefix:@\"").
                        append(getPrefix()).append("\" dimensions:").append(dimensions).append("];");
            }
        } else {
            if (Utils.isPrimitive(returnType)) {
                builder.append("__obj;");
            } else {
                builder.append("[OJNIJavaObject retrieveFromJavaObject:__obj classPrefix:@\"").
                        append(getPrefix()).
                        append("\"];");
            }
        }

        return builder.toString();
    }

    public void generate(String packageName, String className,
                         String interfacesBlock, String superClassName,
                         String importBlock, String declarationBlock,
                         String deallocBlock, String implementationBlock,
                         String implementationImportBlock) throws Exception {
        String headerTemplate = ResourceList.getStringContentFromResource(Constants.TEMPLATE_HEADER_FILENAME);

        String superClassNameResult = (Utils.isOBJCSystemClass(superClassName) ? superClassName : getPrefix() + superClassName);

        headerTemplate = headerTemplate.replace(Constants.CLASS_NAME, getPrefix()+className);
        headerTemplate = headerTemplate.replace(Constants.SUPERCLASS_NAME, superClassNameResult);
        headerTemplate = headerTemplate.replace(Constants.IMPORT_BLOCK, importBlock);
        headerTemplate = headerTemplate.replace(Constants.DECLARATION_BLOCK, declarationBlock);
        headerTemplate = headerTemplate.replace(Constants.INTERFACES_IMPLEMENTS, interfacesBlock);

        String implementationTemplate = ResourceList.getStringContentFromResource(Constants.TEMPLATE_IMPLEMENTATION_FILENAME);

        implementationTemplate = implementationTemplate.replace(Constants.CLASS_NAME, getPrefix()+className);
        implementationTemplate = implementationTemplate.replace(Constants.DEALLOC_BLOCK, deallocBlock);
        implementationTemplate = implementationTemplate.replace(Constants.IMPLEMENTATION_BLOCK, implementationBlock);
        implementationTemplate = implementationTemplate.replace(Constants.IMPORT_BLOCK, implementationImportBlock);


        header = headerTemplate;
        implementation = implementationTemplate;
    }

    @Override
    public HashSet<String> getDependencies() {
        return  dependencies;
    }
}
