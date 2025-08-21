#!/bin/bash

# 1) Request the token
response=$(curl -X POST http://localhost:8080/auth/token \
             -H "Content-Type: application/json" \
             -d '{
               "serviceName": "MondayMemory",
               "requestedRole": "ROLE_USER"
             }')

# 2) Parse the token from JSON
#   Requires jq: sudo apt install jq
token=$(echo "$response" | jq -r '.body.authentication.token')

# 3) Display the token (optional)
echo "Token: $token"

# 4) Build the Authorization header
authorization="Bearer $token"

# 5) Call the /auth/verify endpoint
curl -s -X POST \
  "http://localhost:8080/auth/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: $authorization"
