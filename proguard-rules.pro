-dontshrink
-dontoptimize
-useuniqueclassmembernames
-adaptclassstrings
-allowaccessmodification

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod
-renamesourcefileattribute Source

# Forge / Mod entrypoints
-keep class dglabmc.DgLabMcMod { *; }
-keep @net.minecraftforge.fml.common.Mod class * { *; }
-keep @net.minecraftforge.fml.common.Mod$EventBusSubscriber class * { *; }
-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

# Gson / ZIP config schema
-keep class dglabmc.config.AppConfig { *; }
-keep class dglabmc.config.AppConfig$* { *; }
-keep class dglabmc.config.ConfigArchiveService$* { *; }
-keep class dglabmc.device.DeviceMessage { *; }
-keep class dglabmc.rule.RuleDefinition { *; }
-keep class dglabmc.wave.WaveformDefinition { *; }

# Enum names are part of the config payload and command/UI state.
-keep enum dglabmc.rule.ChannelTarget { *; }
-keep enum dglabmc.rule.IntensityMode { *; }
-keep enum dglabmc.rule.RuleProcessingMode { *; }
-keep enum dglabmc.rule.StrengthAction { *; }
-keep enum dglabmc.device.DeviceChannel { *; }

# Keep the generator entrypoint usable from Gradle.
-keep class dglabmc.tool.PvpPunishConfigGenerator {
    public static void main(java.lang.String[]);
}
