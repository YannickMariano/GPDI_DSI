# run.ps1
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        Set-Item -Path "Env:$name" -Value $value
        # [System.Environment]::SetEnvironmentVariable($name, $value)
    }
}

Write-Host "=== Variables chargées ==="
Write-Host "SERVER_PORT = '$env:SERVER_PORT'"
Write-Host "DB_URL = '$env:DB_URL'"
Write-Host "DB_PASSWORD = '$env:DB_PASSWORD'"
Write-Host "KEYCLOAK_ISSUER_URI = '$env:KEYCLOAK_ISSUER_URI'"
Write-Host "=========================="

.\mvnw.cmd clean spring-boot:run