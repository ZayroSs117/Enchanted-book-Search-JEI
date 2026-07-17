$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Build = Join-Path $Root "build"
$Classes = Join-Path $Build "classes"
$Libs = Join-Path $Build "libs"
$Version = "1.0.6"
$JarName = "enchanted-book-search-$Version.jar"

Remove-Item $Classes, $Libs -Recurse -Force -ErrorAction SilentlyContinue
New-Item $Classes, $Libs -ItemType Directory -Force | Out-Null

$Sources = Get-ChildItem @(
    (Join-Path $Root "stubs/src"),
    (Join-Path $Root "src/main/java")
) -Recurse -Filter *.java | ForEach-Object FullName

& javac --release 17 -d $Classes $Sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Remove-Item (Join-Path $Classes "mezz"), (Join-Path $Classes "net"), (Join-Path $Classes "dev") -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item (Join-Path $Root "src/main/resources/*") $Classes -Recurse -Force
Copy-Item (Join-Path $Root "LICENSE") (Join-Path $Classes "LICENSE_enchantedbooksearch") -Force

$Manifest = Join-Path $Build "MANIFEST.MF"
@"
Manifest-Version: 1.0
Implementation-Title: Enchanted Book Search
Implementation-Version: $Version
Created-By: Enchanted Book Search build script
"@ | Set-Content $Manifest -Encoding ascii

& jar --create --file (Join-Path $Libs $JarName) --manifest $Manifest -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host "Built $(Join-Path $Libs $JarName)"
