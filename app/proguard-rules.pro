# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.iccyuan.hush.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.iccyuan.hush.data.model.**$$serializer { *; }

# Keep the (de)serialized data model intact — these are persisted as JSON columns and
# round-tripped via polymorphic serializers, so member/name stripping must not touch them.
-keep class com.iccyuan.hush.data.model.** { *; }

# Room: keep entities and generated DAO/database implementations.
-keep class com.iccyuan.hush.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# TTS / notification reflection-free, but keep enum values used via valueOf in converters.
-keepclassmembers enum com.iccyuan.hush.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 高德地图 / 定位 / 地理围栏 SDK：大量依赖反射与 JNI，必须整体 keep，否则 release（R8）
# 构建会把这些类裁剪/混淆掉，导致打开地图/定位时崩溃。规则取自高德官方混淆配置。
-keep class com.amap.api.**{*;}
-keep class com.amap.api.maps.**{*;}
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.amap.api.services.**{*;}
-keep class com.amap.api.maps2d.**{*;}
-keep class com.amap.api.mapcore.**{*;}
-keep class com.amap.api.mapcore2d.**{*;}
-keep class com.amap.api.col.**{*;}
-keep class com.amap.apis.**{*;}
-keep class com.autonavi.**{*;}
-keep class com.loc.**{*;}
-dontwarn com.amap.api.**
-dontwarn com.amap.apis.**
-dontwarn com.autonavi.**
-dontwarn com.loc.**
