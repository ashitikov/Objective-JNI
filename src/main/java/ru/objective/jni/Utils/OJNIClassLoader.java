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

package ru.objective.jni.Utils;

import org.apache.commons.bcel6.classfile.JavaClass;
import org.apache.commons.bcel6.util.ClassPath;
import org.apache.commons.bcel6.util.SyntheticRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Created by ashitikov on 09.12.15.
 */
public class OJNIClassLoader {
    private static OJNIClassLoader instance = new OJNIClassLoader();

    private SyntheticRepository systemRepository;
    private SyntheticRepository[] cpRepositories;

    public static OJNIClassLoader getInstance() {
        return instance;
    }

    public void setClassPaths(ClassPath[] classPaths) {
        cpRepositories = new SyntheticRepository[classPaths.length];

        for (int i = 0; i < cpRepositories.length; i++) {
            cpRepositories[i] = SyntheticRepository.getInstance(classPaths[i]);
        }
    }

    private OJNIClassLoader() {
        systemRepository = SyntheticRepository.getInstance();
    }

    public JavaClass loadClass(String className) {
        className = Utils.getDottedClassName(Utils.getBasicType(className));

        JavaClass found = null;

        try {
            found = systemRepository.loadClass(className);
        } catch (Exception e) {}

        if (found == null) {
            for (SyntheticRepository repository : cpRepositories) {
                try {
                    found = repository.loadClass(className);

                    if (found != null) {
                        break;
                    }
                } catch (Exception e) {}
            }
        }

        return found;
    }

    public String[] getClassNamesFromPackage(String packageName) throws IOException {
        HashSet<String> result = getClassNamesSetFromPackage(packageName);

        return result.toArray(new String[result.size()]);
    }

    public HashSet<String> getClassNamesSetFromPackage(String packageName) throws IOException {
        HashSet<String> result = new HashSet<>();

        Pattern pattern = Pattern.compile(packageName + ".*.class");

        final Collection<String> systemList = ResourceList.getResources(systemRepository.getClassPath(), pattern);
        for(String name : systemList){
            result.add(Utils.getClassNameFromClassFileName(name));
        }

        for (SyntheticRepository repository : cpRepositories) {
            final Collection<String> cpList = ResourceList.getResources(repository.getClassPath(), pattern);
            for(String name : cpList){
                result.add(Utils.getClassNameFromClassFileName(name));
            }
        }

        return result;
    }
}
