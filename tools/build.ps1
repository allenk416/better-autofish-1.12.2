[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$MinecraftRuntimeJar,
    [Parameter(Mandatory=$true)][string]$ForgeJar,
    [Parameter(Mandatory=$true)][string]$AsmLibraryDirectory,
    [string]$JdkHome=$env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
if([string]::IsNullOrWhiteSpace($JdkHome)) { throw 'Set JAVA_HOME or pass -JdkHome.' }
$MinecraftRuntimeJar = (Resolve-Path -LiteralPath $MinecraftRuntimeJar).Path
$ForgeJar = (Resolve-Path -LiteralPath $ForgeJar).Path
$AsmLibraryDirectory = (Resolve-Path -LiteralPath $AsmLibraryDirectory).Path
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$javac = Join-Path $JdkHome 'bin\javac.exe'
$java = Join-Path $JdkHome 'bin\java.exe'
if(!(Test-Path -LiteralPath $javac) -or !(Test-Path -LiteralPath $java)) { throw 'JdkHome must point to a JDK with javac.exe and java.exe.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $version = (Get-Content -LiteralPath 'VERSION' -Raw).Trim()
    $stage = Join-Path $projectRoot ('build\compile-' + $version + '-' + [Guid]::NewGuid().ToString('N'))
    $classes = Join-Path $stage 'classes'
    $testClasses = Join-Path $stage 'tests'
    $fixtureSource = Join-Path $stage 'fixtures-src'
    $fixtures = Join-Path $stage 'fixtures'
    $compilerApi = Join-Path $stage 'compiler-api'
    $mappingApi = Join-Path $stage 'mapping-api'
    $mappingClasses = Join-Path $stage 'mapping-tool'
    New-Item -ItemType Directory -Path $stage,$classes,$testClasses,$fixtures,$mappingClasses -Force | Out-Null
    $asm = @(Get-ChildItem -LiteralPath $AsmLibraryDirectory -Filter '*.jar' -Recurse |
        Where-Object { $_.Name -match '^asm(-commons)?-9' } | Select-Object -ExpandProperty FullName)
    if($asm.Count -lt 2) { throw 'ASM 9 and ASM Commons 9 JARs are required.' }
    python 'tools\extract_dependencies.py' --output $mappingApi --jars $asm
    if($LASTEXITCODE) { throw 'Could not prepare ASM compiler classes.' }
    & $javac -encoding UTF-8 --release 8 -cp $mappingApi -d $mappingClasses 'tools\RemapMinecraft.java'
    if($LASTEXITCODE) { throw 'Could not compile the mapping utility.' }
    $mapping = Join-Path $stage 'forge-runtime.tsrg'
    python 'tools\prepare_mappings.py' --forge-jar $ForgeJar --output $mapping
    if($LASTEXITCODE) { throw 'Could not extract the Forge mapping table.' }
    $minecraftMapped = Join-Path $stage 'minecraft-srg.jar'
    $forgeMapped = Join-Path $stage 'forge-srg.jar'
    & $java -cp "$mappingClasses;$mappingApi" RemapMinecraft $MinecraftRuntimeJar $mapping $minecraftMapped
    if($LASTEXITCODE) { throw 'Could not remap the Minecraft runtime.' }
    & $java -cp "$mappingClasses;$mappingApi" RemapMinecraft $ForgeJar $mapping $forgeMapped
    if($LASTEXITCODE) { throw 'Could not remap Forge.' }
    python 'tools\extract_dependencies.py' --output $compilerApi --jars $minecraftMapped $forgeMapped
    if($LASTEXITCODE) { throw 'Could not prepare compiler dependencies.' }
    $sources = @(Get-ChildItem -LiteralPath 'src\main\java' -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
    & $javac -encoding UTF-8 --release 8 -cp $compilerApi -d $classes $sources
    if($LASTEXITCODE) { throw 'Could not compile the mod source for Java 8.' }
    python 'tests\make_fixtures.py' --output $fixtureSource
    if($LASTEXITCODE) { throw 'Could not generate test fixtures.' }
    $fixtureFiles = @(Get-ChildItem -LiteralPath $fixtureSource -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
    & $javac -encoding UTF-8 --release 8 -d $fixtures $fixtureFiles
    if($LASTEXITCODE) { throw 'Could not compile test fixtures.' }
    $testSources = @(Get-ChildItem -LiteralPath 'tests' -Filter '*.java' | Select-Object -ExpandProperty FullName)
    & $javac -encoding UTF-8 --release 8 -cp "$fixtures;$classes" -d $testClasses $testSources
    if($LASTEXITCODE) { throw 'Could not compile tests.' }
    foreach($test in @('FishingPolicyTest','HandlerIntegrationTest','LocalizationIntegrationTest','CastingAngleIntegrationTest')) {
        & $java -cp "$fixtures;$classes;$testClasses;src\main\resources" $test
        if($LASTEXITCODE) { throw "$test failed." }
    }
    python 'tools\verify_languages.py'
    if($LASTEXITCODE) { throw 'Language resources failed validation.' }
    python 'tools\package_mod.py' --classes-dir $classes
    if($LASTEXITCODE) { throw 'Could not package this version. No existing release will be overwritten.' }
}
finally { Pop-Location }
