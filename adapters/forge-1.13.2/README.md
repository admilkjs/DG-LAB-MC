# DG-LAB MC Forge 1.13.2 adapter

This directory is the Forge 1.13.2 compatibility layer.  Rules, configuration,
waveforms, security and the device protocol are provided by the shared `core`
module; this adapter only owns Forge lifecycle, client events, screens and the
Netty WebSocket composition.

Forge 1.13.2 uses the legacy ForgeGradle 3 toolchain.  Build it with Java 11
and the checked-in Gradle 4.9 wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
.\gradlew.bat compileJava --no-daemon
.\gradlew.bat test --no-daemon
.\gradlew.bat buildRelease --no-daemon
```

The nested build consumes `../../core/build/libs/dglabmc-core-1.0.1.jar`; the root
build must build `:core` before invoking this adapter.
