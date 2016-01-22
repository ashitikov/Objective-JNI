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

package ru.objective.jni.argparser;

import org.apache.commons.cli.Options;

/**
 * Created by ashitikov on 18.01.16.
 */
public class ArgumentOptions {
    public final static Options ARGUMENT_OPTIONS = new Options();

    static {
        constructOptions();
    }

    private static void constructOptions() {
        ARGUMENT_OPTIONS.addOption("help", "help", false, "Print this message");
        ARGUMENT_OPTIONS.addOption("version", "version", false, "Print Objective-JNI version");

        ARGUMENT_OPTIONS.addOption("exclude", "exclude", true, "Explicitely exclude java class. " +
                "Objective-C wrapper will not be generated for that.");

        ARGUMENT_OPTIONS.addOption("excludepackage", "excludepackage", true, "Explicitly excludes java package. " +
                "Objective-C wrapper will not be generated for all types inside this package.");

        ARGUMENT_OPTIONS.addOption("classpath", "classpath", true, "Specify .jar classpath file " +
                "to generate Objective-C wrapper.");

        ARGUMENT_OPTIONS.addOption("class", "class", true, "Specify java class to generate Objective-C wrapper");

        ARGUMENT_OPTIONS.addOption("package", "package", true, "Specify java package to " +
                "generate Objective-C wrappers for classes inside.");

        ARGUMENT_OPTIONS.addOption("output", "output", true, "Specify output dir to put all generated wrappers to.");

        ARGUMENT_OPTIONS.addOption("prefix", "prefix", true, "Specify class name prefix " +
                "for each generated Objective-C wrapper.");
    }
}
