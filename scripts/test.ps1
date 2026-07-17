$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Build = Join-Path $Root "build/test-classes"

Remove-Item $Build -Recurse -Force -ErrorAction SilentlyContinue
New-Item $Build -ItemType Directory -Force | Out-Null

$Sources = Get-ChildItem @(
    (Join-Path $Root "stubs/src"),
    (Join-Path $Root "src/main/java"),
    (Join-Path $Root "test")
) -Recurse -Filter *.java | ForEach-Object FullName

& javac --release 17 -d $Build $Sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

& java -cp $Build TestHarness
if ($LASTEXITCODE -ne 0) { throw "tests failed" }
