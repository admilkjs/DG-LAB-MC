-dontshrink
-dontoptimize
-useuniqueclassmembernames
-adaptclassstrings
-allowaccessmodification

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod
-renamesourcefileattribute Source

# Forge / Mod entrypoints
-keep class cn.admilk.dglabweb.DgLabWebMod { *; }
-keep @net.minecraftforge.fml.common.Mod class * { *; }
-keep @net.minecraftforge.fml.common.Mod$EventBusSubscriber class * { *; }
-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

# Gson / ZIP config schema
-keep class cn.admilk.dglabweb.config.AppConfig { *; }
-keep class cn.admilk.dglabweb.config.AppConfig$* { *; }
-keep class cn.admilk.dglabweb.config.ConfigArchiveService$* { *; }
-keep class cn.admilk.dglabweb.device.DeviceMessage { *; }
-keep class cn.admilk.dglabweb.rule.RuleDefinition { *; }
-keep class cn.admilk.dglabweb.wave.WaveformDefinition { *; }

# Enum names are part of the config payload and command/UI state.
-keep enum cn.admilk.dglabweb.rule.ChannelTarget { *; }
-keep enum cn.admilk.dglabweb.rule.IntensityMode { *; }
-keep enum cn.admilk.dglabweb.rule.RuleProcessingMode { *; }
-keep enum cn.admilk.dglabweb.rule.StrengthAction { *; }
-keep enum cn.admilk.dglabweb.device.DeviceChannel { *; }

# Keep the generator entrypoint usable from Gradle.
-keep class cn.admilk.dglabweb.tool.PvpPunishConfigGenerator {
    public static void main(java.lang.String[]);
}
