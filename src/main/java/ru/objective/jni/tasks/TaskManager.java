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

import org.apache.commons.cli.CommandLine;
import ru.objective.jni.utils.OJNIClassLoader;
import ru.objective.jni.utils.Utils;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by ashitikov on 28.11.15.
 */
public class TaskManager {

    public TaskManager() {
    }

    public void run(CommandLine cmd) throws Exception {
        String[] classPaths = cmd.getOptionValues("classpath");
        String[] excludes = cmd.getOptionValues("exclude");
        String[] excludesPackages = cmd.getOptionValues("excludepackage");
        String[] classes = cmd.getOptionValues("class");
        String[] packages = cmd.getOptionValues("package");

        String output = cmd.getOptionValue("output", ".");
        String prefix = cmd.getOptionValue("prefix", "");

        ITask task = null;

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            task = new HelpTask();
        } else if (cmd.hasOption("version")) {
            task = new VersionTask();
        } else {
            OJNIClassLoader.getInstance().setClassPaths(Utils.classPathsFromStrings(classPaths));

            task = new DefaultTask(classPaths, excludes, excludesPackages, classes, packages, output, prefix);
        }

        task.run();
    }
}
