$ErrorActionPreference = "Stop"

$targets = @(
    @{ Minecraft = "1.21";   Yarn = "1.21+build.8";     FabricApi = "0.100.8+1.21";   UkuLib = "1.4.1+1.21";   UkuLibDependency = "1.4.1" },
    @{ Minecraft = "1.21.1"; Yarn = "1.21.1+build.3";   FabricApi = "0.102.1+1.21.1"; UkuLib = "1.4.1+1.21";   UkuLibDependency = "1.4.1" },
    @{ Minecraft = "1.21.2"; Yarn = "1.21.2+build.1";   FabricApi = "0.106.1+1.21.2"; UkuLib = "1.5.0+1.21.2"; UkuLibDependency = "1.5.0" },
    @{ Minecraft = "1.21.3"; Yarn = "1.21.3+build.2";   FabricApi = "0.114.1+1.21.3"; UkuLib = "1.5.0+1.21.2"; UkuLibDependency = "1.5.0" },
    @{ Minecraft = "1.21.4"; Yarn = "1.21.4+build.8";   FabricApi = "0.119.4+1.21.4"; UkuLib = "1.6.0+1.21.4"; UkuLibDependency = "1.6.0" }
)

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

Write-Host "All Minecraft 1.21 through 1.21.4 jars are in build\libs."
