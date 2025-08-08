#!/bin/bash

# 1) Request the token
response=$(curl -s -X POST \
  "http://localhost:8080/auth/token?serviceName=monday-service&role=role_admin")

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
  -H "Authorization: $authorization"
