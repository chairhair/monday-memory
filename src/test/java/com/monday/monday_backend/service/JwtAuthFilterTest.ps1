$response = Invoke-WebRequest -Uri "http://localhost:8080/auth/token?serviceName=monday-service&role=role_admin" `
    -Method POST `
    -UseBasicParsing


$parsed = $response.Content | ConvertFrom-Json
$token = $parsed.body.authentication.token
$token

$authorization = "Bearer "+$token

Invoke-WebRequest -Uri "http://localhost:8080/auth/verify" `
    -Method POST `
    -Headers @{ Authorization = $authorization } `
    -UseBasicParsing