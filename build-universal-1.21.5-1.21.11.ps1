$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    & .\gradlew.bat remapJar --no-daemon `
        "-Pminecraft_version=1.21.11" `
        "-Pyarn_mappings=1.21.11+build.6" `
        "-Pfabric_api_version=0.141.6+1.21.11" `
        "-Pukulib_version=1.10.2+1.21.11" `
        "-Pukulib_dependency_version=1.10.2" `
        "-Puniversal_build=true"

    if ($LASTEXITCODE -ne 0) {
        throw "Universal JAR build failed (exit code $LASTEXITCODE)."
    }
} finally {
    Pop-Location
}

$jarPath = Join-Path $PSScriptRoot "build\libs\R7XTiertagger-2.1.4+universal.jar"
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "Universal JAR was not created: $jarPath"
}

Write-Host "Universal R7XTiertagger JAR created: $jarPath"
