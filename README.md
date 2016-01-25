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

## Wrapper generation
Each java class from classpath (--classpath option) or concrete one that you specified explicitly in --class option will be generated Objective-C wrapper, except private classes. Classes and interface inheritance will be saved. 
Type convertion:

| Java          | Objective-C   |
| ------------- |:-------------:|
| Class         | @interface    |
| Interface     | @protocol     |
| int           | int           |
| long          | long          |
| short         | short         |
| byte          | **char**      |
| boolean       | bool          |
| float         | float         |
| double        | double        |
| void          | void          |


#### Property generation
To avoid overload problems and name conflicts, all properties will be generated with prefix *property_*. 
**In case of final property, setter will not be generated.**
Example. Java property:
```java
public String testString;
```
Generated Objective-C getter and setter:
```objectivec
- (String *)property_testString;
- (void)setProperty_testString:(String *)value;
```
Private properties will be ignored.
You can use @OJNIExportName annotation to change name and @OJNIExclude to exclude it from wrapper.

#### Method generation
Example. Java methods:
```java
public String getTestString() { 
   return testString;
}

public void setTestString(String value) {
   testString = value;
}
```
Generated Objective-C wrapper:
```objectivec
- (String *)getTestString;
- (void)setTestString:(String *)value;
```
Please, note, that *(String *)* is not the same as *(NSString *)*. In that context, *(String *)* is Objective-C wrapper for java class *String*. For more comfort, there is a special helper methods right for String:
```objectivec
- (instancetype)initWithNSString:(NSString *)string;
+ (instancetype)stringWithNSString:(NSString *)string;
- (NSString *)toNSString;
```
So you can easily create java string right from Objective-C code:
```objectivec
SomeClass *javaClass = [[SomeClass alloc] init]; // call empty constructor
String *str = [String stringWithNSString:@"Hello, World!"];
[javaClass setTestString:str];

```
Or easily convert it back:
```objectivec
NSString *nsstr = [[javaClass getTestString] toNSString];
NSLog(@"Got test string from java: %@", nsstr);
```

There is a big problem: Objective-C doesn't support method overloading. Thats why this java code:
```java
public void overload(int a) {}
public void overload(int a, int b) {}
public void overload(String str) {}
```
will be translated to Objective-C wrapper:
```objectivec
- (void)overloadWithAint:(int)a ;
- (void)overload:(int)a b:(int)b ;
- (void)overloadWithStrString:(ASString *)str ;
```

###### Constructors
Example. Java class:
```java
public class SomeClass {
   // with overloading
   public SomeClass() { }
   public SomeClass(int a) { }
   public SomeClass(String a) { }
}
```
Objective-C translated:
```objectivec
@interface SomeClass : Object
- (instancetype)init;
- (instancetype)initWithaInt:(int)a;
- (instancetype)initWithaString:(String *)a;
@end
```
Private methods will be ignored.
You can use @OJNIExportName annotation to change name and @OJNIExclude to exclude it from wrapper.

## Arrays
There is two types of arrays:
1. Primitive (int[], float[] ...)
2. Object (String[], Object[] ...)

#### Primitive array
All primitive arrays like int[], float[] and etc. translates to OJNIPrimitive##Type##Array, where ##Type## is Int, Float and etc. 
Java example:
```java
public int[] getGivenArray(int[] array) {
   return array;
}
```
Translated Objective-C:
```objectivec
- (OJNIPrimitiveIntArray *)getGivenArray(OJNIPrimitiveIntArray *)array;
```
**OJNIPrimitiveArray classes uses standard C arrays, right like NSData * class.**

Multidimensional example:
```java
public int[][] getGivenArray(int[][] array) {
   return array;
}
```
Translated:
```objectivec
- (NSArray <OJNIPrimitiveIntArray *> *)getGivenArray(NSArray <OJNIPrimitiveIntArray *> *)array;
```
For the comfort, there is a helper methods to create OJNIPrimitiveArray without C arrays like:
```objectivec
- (instancetype)initWithNumberArray:(NSArray <NSNumber *> *)numberArray;
+ (instancetype)arrayWithNumberArray:(NSArray <NSNumber *> *)numberArray;

//using
OJNIPrimitiveIntArray * array = [OJNIPrimitiveIntArray arrayWithNumberArray:@[@(1), @(2), @(3)]];
NSArray <OJNIPrimitiveIntArray *> *result = [SomeClass getGivenArray:@[array]];
for (int i = 0; i < result.count; i++) {
   int stored = [result[i] intAtIndex:i];
   // do smth with stored.
}
```

#### Object array
All object arrays like String[], Object[] and etc. translates to NSArray <##ObjectType##> *, where ##ObjectType## is object class.
Java example:
```java
public String[] getGivenArray(String[] array) {
   return array;
}
```
Translated Objective-C:
```objectivec
- (NSArray <String *> *)getGivenArray(NSArray <String *> *)array;
```
Multidimensional example:
```java
public String[][] getGivenArray(String[][] array) {
   return array;
}
```
Translated:
```objectivec
- (NSArray <NSArray <String *> *> *)getGivenArray(NSArray <NSArray <String *> *> *)array;
```

# Objective-JNI-Annotations
You can find more here: https://github.com/ashitikov/Objective-JNI/tree/master/Objective-JNI-Annotations
