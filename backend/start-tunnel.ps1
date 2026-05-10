$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$toolsDir = Join-Path $PSScriptRoot "tools"
$cloudflared = Join-Path $toolsDir "cloudflared.exe"

if (-not (Test-Path $cloudflared)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    Invoke-WebRequest `
        -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" `
        -OutFile $cloudflared
}

& $cloudflared tunnel --protocol http2 --config "$env:USERPROFILE\.cloudflared\config.yml" run ad-backend
