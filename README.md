# Objective-JNI
This is Objective-C code wrapper generator over the java library (.jar) using JNI calls.
Environment is available here: https://github.com/ashitikov/Objective-JNI-Environment

Usage:
```
java -jar Objective-JNI-1.0-SNAPSHOT.jar
```

Options:
```
 -class,--class <arg>                     Specify java class to generate
                                          Objective-C wrapper
 -classpath,--classpath <arg>             Specify .jar classpath file to
                                          generate Objective-C wrapper.
 -exclude,--exclude <arg>                 Explicitely exclude java class.
                                          Objective-C wrapper will not be
                                          generated for that.
 -excludepackage,--excludepackage <arg>   Explicitly excludes java
                                          package. Objective-C wrapper
                                          will not be generated for all
                                          types inside this package.
 -help,--help                             Print this message
 -output,--output <arg>                   Specify output dir to put all
                                          generated wrappers to.
 -package,--package <arg>                 Specify java package to generate
                                          Objective-C wrappers for classes
                                          inside.
 -prefix,--prefix <arg>                   Specify class name prefix for
                                          each generated Objective-C
                                          wrapper.
 -version,--version                       Print Objective-JNI version
```

Example:
```
java -jar Objective-JNI-1.0-SNAPSHOT.jar --output ./generated --prefix AS --classpath ./some-java-code.jar --class java.lang.Integer
```

It's recommended to use OpenJDK 1.7.
