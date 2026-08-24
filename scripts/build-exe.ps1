# Builds the Arch Hub as a self-contained Windows app (bundles its own JRE via
# jpackage - no separate Java install needed to run it), plus an .exe installer.
# Mirrors scripts/build-package.sh (the Linux/macOS equivalent).
#
# Requires: JDK 17+ on PATH (for jpackage) and the WiX Toolset v3
# (candle.exe/light.exe) on PATH for the installer step; the app-image step
# works without WiX.
#
# Usage:  powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1
#         powershell -ExecutionPolicy Bypass -File scripts\build-exe.ps1 -Version 1.2.3
param(
    [string]$Version,
    [switch]$SkipSmoke
)

$ErrorActionPreference = 'Stop'

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$DistDir = Join-Path $RepoRoot 'dist-desktop'
$AppName = 'ArchHub'

Push-Location $RepoRoot
try {
    if (-not $Version) {
        Write-Host '==> Resolving version from pom.xml' -ForegroundColor Cyan
        $Version = (mvn -q -DforceStdout help:evaluate -Dexpression=project.version).Trim()
    }
    Write-Host "    version: $Version"

    Write-Host '==> Running tests and building the jar' -ForegroundColor Cyan
    mvn -q clean package
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed with exit code $LASTEXITCODE" }

    $jar = Join-Path $RepoRoot "target\arch-hub-$Version.jar"
    if (-not (Test-Path $jar)) { throw "Jar not found: $jar" }

    if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
    New-Item -ItemType Directory -Path $DistDir | Out-Null

    Write-Host '==> Building the portable app-image' -ForegroundColor Cyan
    jpackage `
        --input (Join-Path $RepoRoot 'target') `
        --main-jar "arch-hub-$Version.jar" `
        --name $AppName `
        --app-version $Version `
        --type app-image `
        --dest $DistDir `
        --java-options '-Djava.awt.headless=true'
    if ($LASTEXITCODE -ne 0) { throw "jpackage (app-image) failed with exit code $LASTEXITCODE" }

    $appImageDir = Join-Path $DistDir $AppName
    $launcher = Join-Path $appImageDir "$AppName.exe"
    if (-not (Test-Path $launcher)) { throw "Launcher not found: $launcher" }

    if (-not $SkipSmoke) {
        Write-Host '==> Smoke test (--version)' -ForegroundColor Cyan
        & $launcher --version
        if ($LASTEXITCODE -ne 0) { throw "Smoke test failed for $launcher" }
    }

    Write-Host '==> Building the installer (jpackage + WiX)' -ForegroundColor Cyan
    $wix = Get-Command candle.exe -ErrorAction SilentlyContinue
    if (-not $wix) {
        Write-Host '    (skipped: WiX Toolset (candle.exe) not found on PATH)' -ForegroundColor Yellow
        Write-Host "Done. Portable app-image ready at $appImageDir" -ForegroundColor Green
        return
    }

    jpackage `
        --app-image $appImageDir `
        --name $AppName `
        --app-version $Version `
        --type exe `
        --win-shortcut `
        --win-menu `
        --dest $DistDir
    if ($LASTEXITCODE -ne 0) { throw "jpackage (installer) failed with exit code $LASTEXITCODE" }

    $installer = Get-ChildItem $DistDir -Filter '*.exe' | Where-Object { $_.Name -ne "$AppName.exe" } | Select-Object -First 1
    if (-not $installer) { throw "No installer .exe produced in $DistDir" }

    $versionedExe = Join-Path $DistDir "$AppName-$Version.exe"
    if ($installer.FullName -ne $versionedExe) { Move-Item $installer.FullName $versionedExe -Force }

    $hash = (Get-FileHash $versionedExe -Algorithm SHA256).Hash.ToLower()
    "$hash  $(Split-Path $versionedExe -Leaf)" | Set-Content "$versionedExe.sha256" -Encoding ascii

    $manifest = @{
        version    = $Version
        builtAtUtc = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
        exePath    = $versionedExe
        sha256     = $hash
    } | ConvertTo-Json -Depth 4
    Set-Content -Path (Join-Path $DistDir 'archhub-build-info.json') -Value $manifest -Encoding UTF8

    Write-Host ''
    Write-Host "Pronto: $versionedExe" -ForegroundColor Green
    Write-Host "SHA256: $hash" -ForegroundColor DarkGray
}
finally {
    Pop-Location
}
