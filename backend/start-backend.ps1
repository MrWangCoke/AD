$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$jdkHome = "C:\Users\MrWang\.jdks\ms-17.0.17"
if (-not (Test-Path (Join-Path $jdkHome "bin\java.exe"))) {
    throw "Java 17 not found at $jdkHome. Please install JDK 17 or update start-backend.ps1."
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

$existingListener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existingListener) {
    Write-Host "Port 8080 is already in use by process $($existingListener.OwningProcess). Backend may already be running."
    exit 0
}

.\mvnw.cmd spring-boot:run
