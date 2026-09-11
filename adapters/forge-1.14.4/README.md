# DG-LAB MC Forge 1.14.4 adapter

This adapter contains only Forge 1.14.4 integration.  Business logic is
compiled directly from the shared `../../core/src/main/java` source tree so
the version adapter never owns a second copy of Core.

The adapter includes the 1.14.4 client event bridge, command router, control
center screens, key binding and Netty WebSocket runtime.  Minecraft-specific
types stay in this directory; rules, configuration, security and waveform
processing stay in Core.

## Build

ForgeGradle 3 uses Gradle 4.9. Use a JDK 8 or 11 installation (JDK 11 is the
recommended local fallback when a full JDK 8 is unavailable):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
.\gradlew.bat compileJava --no-daemon
.\gradlew.bat test --no-daemon
.\gradlew.bat buildRelease --no-daemon
```

The release jar is written to `dist/release/` after re-obfuscation.
