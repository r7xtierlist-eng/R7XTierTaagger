param([string]$OnlyVersion)

$ErrorActionPreference = "Stop"

$targets = @(
    @{ Minecraft = "1.21.5";  Yarn = "1.21.5+build.1";  FabricApi = "0.128.2+1.21.5";  UkuLib = "1.7.1+1.21.5";  UkuLibDependency = "1.7.1" },
    @{ Minecraft = "1.21.6";  Yarn = "1.21.6+build.1";  FabricApi = "0.128.2+1.21.6";  UkuLib = "1.8.2+1.21.6";  UkuLibDependency = "1.8.2" },
    @{ Minecraft = "1.21.7";  Yarn = "1.21.7+build.8";  FabricApi = "0.129.0+1.21.7";  UkuLib = "1.8.2+1.21.6";  UkuLibDependency = "1.8.2" },
    @{ Minecraft = "1.21.8";  Yarn = "1.21.8+build.1";  FabricApi = "0.136.1+1.21.8";  UkuLib = "1.8.2+1.21.6";  UkuLibDependency = "1.8.2" },
    @{ Minecraft = "1.21.9";  Yarn = "1.21.9+build.1";  FabricApi = "0.134.1+1.21.9";  UkuLib = "1.9.0+1.21.9";  UkuLibDependency = "1.9.0" },
    @{ Minecraft = "1.21.10"; Yarn = "1.21.10+build.3"; FabricApi = "0.138.4+1.21.10"; UkuLib = "1.9.0+1.21.9"; UkuLibDependency = "1.9.0" },
    @{ Minecraft = "1.21.11"; Yarn = "1.21.11+build.6"; FabricApi = "0.141.6+1.21.11"; UkuLib = "1.10.2+1.21.11"; UkuLibDependency = "1.10.2" }
)

if ($OnlyVersion) {
    $targets = @($targets | Where-Object { $_.Minecraft -eq $OnlyVersion })
    if ($targets.Count -eq 0) {
        throw "Unsupported Minecraft version: $OnlyVersion"
    }
}

Push-Location $PSScriptRoot
try {
    foreach ($target in $targets) {
        Write-Host "Building R7XTiertagger for Minecraft $($target.Minecraft)..."
        & .\gradlew.bat remapJar --no-daemon `
            "-Pminecraft_version=$($target.Minecraft)" `
            "-Pyarn_mappings=$($target.Yarn)" `
            "-Pfabric_api_version=$($target.FabricApi)" `
            "-Pukulib_version=$($target.UkuLib)" `
            "-Pukulib_dependency_version=$($target.UkuLibDependency)"

        if ($LASTEXITCODE -ne 0) {
            throw "Build failed for Minecraft $($target.Minecraft) (exit code $LASTEXITCODE)."
        }
    }
} finally {
    Pop-Location
}

Write-Host "All Minecraft 1.21.5 through 1.21.11 jars are in build\libs."
