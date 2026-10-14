# libGDX
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild

-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
   <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}

# gdx-controllers
-keep class com.badlogic.gdx.controllers.android.AndroidControllers { <init>(); }

# Игра: ECS заполняет поля систем через рефлексию по их generic-типу (ComponentMapper<T>),
# сохранения и настройки ищут enum по имени — код игры не переименовываем и не вырезаем.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class com.zaig100.dargon.** { *; }
