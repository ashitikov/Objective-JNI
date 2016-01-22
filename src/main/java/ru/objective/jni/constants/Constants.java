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

package ru.objective.jni.constants;

/**
 * Created by ashitikov on 30.11.15.
 */
public final class Constants {

    public static final String CLASS_SUFFIX = ".class";
    public static final String OBJC_SYSTEM_CLASS = "OJNIJavaObject";
    public static final String JAVA_SYSTEM_CLASS = "java.lang.Object";

    public static final String INTERFACE_NAME = "${INTERFACE_NAME}";
    public static final String INTERFACES_IMPLEMENTS = "${INTERFACES_IMPLEMENTS}";

    public static final String DEALLOC_BLOCK = "${DEALLOC_BLOCK}";
    public static final String IMPORT_BLOCK = "${IMPORT_BLOCK}";
    public static final String CLASS_NAME = "${CLASS_NAME}";
    public static final String SUPERCLASS_NAME = "${SUPERCLASS_NAME}";
    public static final String DECLARATION_BLOCK = "${DECLARATION_BLOCK}";
    public static final String IMPLEMENTATION_BLOCK = "${IMPLEMENTATION_BLOCK}";

    public static final String TEMPLATE_HEADER_FILENAME = "OJNITemplate.h";
    public static final String TEMPLATE_IMPLEMENTATION_FILENAME = "OJNITemplate.m";
    public static final String TEMPLATE_INTERFACE_FILENAME = "OJNITemplateProtocol.h";
    public static final String TEMPLATE_LICENSE_FILENAME = "LICENSE";

    public static final String TEMPLATE_IMPORT_KEY = "${TEMPLATE_IMPORT_KEY}";
    public static final String TEMPLATE_IMPORT = "#import \""+TEMPLATE_IMPORT_KEY+".h\"";

    public static final String TEMPLATE_FORWARD_DECLARATION_KEY = "${TEMPLATE_IMPORT_KEY}";
    public static final String TEMPLATE_FORWARD_DECLARATION = "@class "+TEMPLATE_FORWARD_DECLARATION_KEY+";";

    public static final String TEMPLATE_FORWARD_INTERFACE_DECLARATION_KEY = "${TEMPLATE_IMPORT_KEY}";
    public static final String TEMPLATE_FORWARD_INTERFACE_DECLARATION = "@protocol "+TEMPLATE_FORWARD_INTERFACE_DECLARATION_KEY+";";

    public static final String[] OCCUPIED_OBJC_WORDS = new String[] {
            "release", "dealloc", "retainCount", "alloc", "init"
    };
}
