$body = @{
    serviceName = "monday-service"
    secret = "role"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/auth/token" `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing

#Invoke-WebRequest -Uri "http://localhost:8080/auth/verify" `
#    -Method POST `
#    -Headers @{ Authorization = "Bearer Mnn83ve7iEp3ax2uuP/eIsOh7nFgn5NTLlhuPGQhhU8=" } `
#    -UseBasicParsing