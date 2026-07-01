param(
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version,

    [ValidateSet("patch", "minor", "major")]
    [string]$Bump = "patch",

    [string]$Remote = "origin",
    [string]$Branch = "main",
    [switch]$SkipPush,
    [switch]$AllowDirty
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$versionFile = "version.properties"
$versionsTomlFile = "gradle/libs.versions.toml"
$groupId = "com.github.wukuiqing49"
$jitpackGroupId = "com.github.wukuiqing49.AndroidCoreRouters"
$githubRepository = "wukuiqing49/AndroidCoreRouters"
$artifacts = @("core_router_api", "core_router_annotation", "core_router_processor", "core_router_gradle_plugin")

function Get-VersionFromTag($tagName) {
    if ($tagName -match '^v?(\d+)\.(\d+)\.(\d+)$') {
        return [version]"$($matches[1]).$($matches[2]).$($matches[3])"
    }
    return $null
}

function Get-VersionFromFile($path) {
    if (-not (Test-Path $path)) {
        return $null
    }
    $line = Get-Content -LiteralPath $path -Encoding UTF8 |
        Where-Object { $_ -match '^VERSION_NAME=' } |
        Select-Object -First 1
    if (-not $line) {
        return $null
    }
    return Get-VersionFromTag (($line -replace '^VERSION_NAME=', '').Trim())
}

function Get-NextVersion([string]$bump) {
    $fileVersion = Get-VersionFromFile $versionFile
    if (-not $fileVersion) {
        return "1.0.0"
    }
    switch ($bump) {
        "major" { return "$($fileVersion.Major + 1).0.0" }
        "minor" { return "$($fileVersion.Major).$($fileVersion.Minor + 1).0" }
        default { return "$($fileVersion.Major).$($fileVersion.Minor).$($fileVersion.Build + 1)" }
    }
}

function Run($command) {
    Write-Host ">> $command" -ForegroundColor Cyan
    Invoke-Expression $command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $command"
    }
}

function Replace-InFile($path, [scriptblock]$replace) {
    if (-not (Test-Path $path)) {
        return
    }
    $content = Get-Content -LiteralPath $path -Raw -Encoding UTF8
    $updated = & $replace $content
    if ($updated -ne $content) {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText((Resolve-Path $path), $updated, $utf8NoBom)
        Write-Host "updated $path"
    }
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    $Version = Get-NextVersion $Bump
    Write-Host "Auto version: $Version ($Bump bump)" -ForegroundColor Green
} else {
    Write-Host "Manual version: $Version" -ForegroundColor Green
}

$tag = "v$Version"

if (git tag --list $tag) {
    throw "Tag $tag already exists locally. Choose a new version."
}

if (git ls-remote --tags $Remote "refs/tags/$tag") {
    throw "Tag $tag already exists on $Remote. Choose a new version."
}

$statusBefore = git status --porcelain
if ($statusBefore -and -not $AllowDirty) {
    Write-Host "Working tree has uncommitted changes:" -ForegroundColor Yellow
    git status --short
    throw "Re-run with -AllowDirty to include current changes in the release commit."
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $versionFile), "VERSION_NAME=$Version`n", $utf8NoBom)

Replace-InFile "README.md" {
    param($content)
    $content = $content -replace 'AndroidCoreRouters/v\d+\.\d+\.\d+', "AndroidCoreRouters/$tag"
    foreach ($artifact in $artifacts) {
        $content = $content -replace "com\.github\.wukuiqing49:$artifact:v?\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?", "$groupId`:$artifact`:$tag"
        $content = $content -replace "com\.github\.wukuiqing49\.AndroidCoreRouters:$artifact:v?\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?", "$jitpackGroupId`:$artifact`:$tag"
    }
    $content = $content -replace 'POM_VERSION=v?\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?', "POM_VERSION=$tag"
    return $content
}

Replace-InFile $versionsTomlFile {
    param($content)
    return $content -replace 'androidCoreRouters = "v?\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?"', "androidCoreRouters = `"$tag`""
}

Replace-InFile "core_router_gradle_plugin/src/main/java/com/wkq/router/gradle/RouterGradlePlugin.kt" {
    param($content)
    return $content -replace '\.orElse\("v?\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?"\)', ".orElse(`"$tag`")"
}

Run ".\gradlew.bat --no-daemon --max-workers=1 --no-configuration-cache :core_router_processor:test :core_router_gradle_plugin:test :app:assembleDebug :app:assembleDebugAndroidTest verifyRouterReleaseMinify `"-PPOM_GROUP_ID=$groupId`" `"-PPOM_VERSION=$tag`" `"-PGITHUB_REPOSITORY=$githubRepository`""
Run ".\gradlew.bat --no-daemon --max-workers=1 --no-configuration-cache publishRouterToMavenLocal :app:assembleDebug `"-PusePublishedRouter=true`" `"-PuseMavenLocalRouter=true`" `"-PPOM_GROUP_ID=$jitpackGroupId`" `"-PPOM_VERSION=$tag`" `"-PGITHUB_REPOSITORY=$githubRepository`""

$statusAfter = git status --porcelain
if (-not $statusAfter) {
    throw "No changes to release."
}

if ($AllowDirty) {
    Run "git add -A"
} else {
    Run "git add README.md version.properties gradle/libs.versions.toml build.gradle gradle/router-publish.gradle jitpack.yml scripts/release-router.ps1 .github/workflows/release-router.yml .github/workflows/router-ci.yml"
    Run "git add core_router_gradle_plugin"
    Run "git add core_router_annotation/build.gradle core_router_api/build.gradle core_router_processor/build.gradle core_router_gradle_plugin/build.gradle"
}
Run "git commit -m `"release router $tag`""
Run "git tag $tag"

if (-not $SkipPush) {
    Run "git push $Remote $Branch"
    Run "git push $Remote $tag"
}

Write-Host ""
Write-Host "Release prepared: $tag" -ForegroundColor Green
Write-Host "JitPack: https://jitpack.io/#$githubRepository/$tag"
foreach ($artifact in $artifacts) {
    Write-Host "Dependency: $groupId`:$artifact`:$tag"
}
