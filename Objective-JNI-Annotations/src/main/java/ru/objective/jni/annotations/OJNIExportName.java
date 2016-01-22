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

package ru.objective.jni.annotations;

import java.lang.annotation.*;

/**
 * Created by ashitikov on 29.11.15.
 */
@Retention(RetentionPolicy.CLASS)
@Target(value={ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD})
@Inherited
public @interface OJNIExportName {
    public String name();
}
