param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipClean
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repoRoot 'gradlew.bat'
$releaseDir = Join-Path $repoRoot 'dist\release'

if (-not (Test-Path $gradleWrapper)) {
    throw "gradlew.bat not found: $gradleWrapper"
}

if ($JavaHome) {
    $resolvedJavaHome = (Resolve-Path $JavaHome).Path
    $env:JAVA_HOME = $resolvedJavaHome
    $env:PATH = (Join-Path $resolvedJavaHome 'bin') + ';' + $env:PATH
}

$tasks = New-Object System.Collections.Generic.List[string]
if (-not $SkipClean) {
    [void]$tasks.Add('clean')
}
[void]$tasks.Add('buildRelease')

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "Running tasks: $($tasks -join ' ')"

Push-Location $repoRoot
try {
    & $gradleWrapper @tasks '--no-daemon'
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}

if (-not (Test-Path $releaseDir)) {
    throw "Build finished but release directory was not created: $releaseDir"
}

Write-Host ''
Write-Host 'Artifacts:'
Get-ChildItem -Path $releaseDir | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
