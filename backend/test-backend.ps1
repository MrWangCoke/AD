$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$jdkHome = "C:\Users\MrWang\.jdks\ms-17.0.17"
if (-not (Test-Path (Join-Path $jdkHome "bin\java.exe"))) {
    throw "Java 17 not found at $jdkHome. Please install JDK 17 or update test-backend.ps1."
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

.\mvnw.cmd test
