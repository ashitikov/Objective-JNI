# Objective-JNI-Annotations
This project contains annotations, that can be used inside jar, to generate wrapper from it.

Usage:
```java
@OJNIExportName(name="objectWithA:andB:")
public void getObject(int a, String b) {

}

@OJNIExclude
public void someExcludedMethod() {

}
```

The result is generated obj-c wrapper:
```objectivec
- (void)objectWithA:(int)a andB:(String *)b;

// Method someExcludedMethod will not be generated, because of exclude annotation
```
