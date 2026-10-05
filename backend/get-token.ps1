$response = Invoke-RestMethod -Uri "http://localhost:8080/realms/gpdi/protocol/openid-connect/token" `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "gpdi-client-id"
    username = "nicky"
    password = "nicky"
  }

$response.access_token | Set-Clipboard
Write-Host "Token copié dans le presse-papier ! Valide $($response.expires_in) secondes."