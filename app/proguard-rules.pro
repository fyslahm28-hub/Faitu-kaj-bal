# Add project specific ProGuard rules here.
# Keep all Firestore domain model classes and their constructors/getters/setters
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    *;
}

# Keep Firebase Firestore & Auth annotations and classes
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.** { *; }
