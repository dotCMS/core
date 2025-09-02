#!/bin/bash
# Keycloak JWT Token Generator for Analytics
#
# Usage:
#   ./get-token.sh                                    # Use defaults
#   ./get-token.sh --client-id my-client              # Custom client ID
#   ./get-token.sh --client-secret my-secret          # Custom client secret
#   ./get-token.sh --client-id my-client --client-secret my-secret  # Both custom
#   ./get-token.sh --json                             # Output full JSON response
#   ./get-token.sh --decode                           # Decode and show JWT payload
#   ./get-token.sh --help                             # Show this help

set -e

# Default values (matching docker-compose.yml)
DEFAULT_CLIENT_ID="analytics-customer-customer1"
DEFAULT_CLIENT_SECRET="testsecret"
DEFAULT_KEYCLOAK_URL="http://localhost:61111"
DEFAULT_REALM="dotcms"

# Initialize variables
CLIENT_ID="$DEFAULT_CLIENT_ID"
CLIENT_SECRET="$DEFAULT_CLIENT_SECRET"
KEYCLOAK_URL="$DEFAULT_KEYCLOAK_URL"
REALM="$DEFAULT_REALM"
OUTPUT_JSON=false
DECODE_TOKEN=false

show_help() {
    echo "Keycloak JWT Token Generator for dotCMS Analytics"
    echo ""
    echo "Usage:"
    echo "  ./get-token.sh                                    Generate token with defaults"
    echo "  ./get-token.sh --client-id CLIENT_ID              Specify custom client ID"
    echo "  ./get-token.sh --client-secret CLIENT_SECRET      Specify custom client secret"
    echo "  ./get-token.sh --keycloak-url URL                 Specify Keycloak URL"
    echo "  ./get-token.sh --realm REALM                      Specify realm name"
    echo "  ./get-token.sh --json                             Output full JSON response"
    echo "  ./get-token.sh --decode                           Decode and show JWT payload"
    echo "  ./get-token.sh --help                             Show this help"
    echo ""
    echo "Defaults:"
    echo "  Client ID:     $DEFAULT_CLIENT_ID"
    echo "  Client Secret: $DEFAULT_CLIENT_SECRET"
    echo "  Keycloak URL:  $DEFAULT_KEYCLOAK_URL"
    echo "  Realm:         $DEFAULT_REALM"
    echo ""
    echo "Examples:"
    echo "  # Get token and copy to clipboard"
    echo "  ./get-token.sh | pbcopy"
    echo ""
    echo "  # Use with Analytics API"
    echo "  curl -H \"Authorization: Bearer \$(./get-token.sh)\" \\"
    echo "       \"http://localhost:8088/c/customer1/cluster1/keys\""
    echo ""
    echo "  # Use with CubeJS (paste token into Security Context)"
    echo "  ./get-token.sh --decode  # Shows customer/cluster info"
    echo "  ./get-token.sh           # Copy this token to CubeJS UI"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --client-id)
            CLIENT_ID="$2"
            shift 2
            ;;
        --client-secret)
            CLIENT_SECRET="$2"
            shift 2
            ;;
        --keycloak-url)
            KEYCLOAK_URL="$2"
            shift 2
            ;;
        --realm)
            REALM="$2"
            shift 2
            ;;
        --json)
            OUTPUT_JSON=true
            shift
            ;;
        --decode)
            DECODE_TOKEN=true
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo "❌ Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Construct token endpoint URL
TOKEN_URL="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token"

# Get token from Keycloak
echo "🔐 Getting JWT token from Keycloak..." >&2
echo "   Client ID: $CLIENT_ID" >&2
echo "   Keycloak:  $TOKEN_URL" >&2
echo "" >&2

RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  "$TOKEN_URL")

# Check if request was successful
if ! echo "$RESPONSE" | grep -q "access_token"; then
    echo "❌ Failed to get token. Response:" >&2
    echo "$RESPONSE" >&2
    exit 1
fi

# Extract token using jq (or fallback to grep/sed)
if command -v jq >/dev/null 2>&1; then
    ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
    EXPIRES_IN=$(echo "$RESPONSE" | jq -r '.expires_in')
else
    # Fallback to grep/sed for systems without jq
    ACCESS_TOKEN=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    EXPIRES_IN=$(echo "$RESPONSE" | grep -o '"expires_in":[0-9]*' | cut -d':' -f2)
fi

# Validate token extraction
if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
    echo "❌ Failed to extract access token from response" >&2
    exit 1
fi

if [[ "$OUTPUT_JSON" == true ]]; then
    echo "✅ Full token response:" >&2
    if command -v jq >/dev/null 2>&1; then
        echo "$RESPONSE" | jq .
    else
        echo "$RESPONSE"
    fi
elif [[ "$DECODE_TOKEN" == true ]]; then
    echo "✅ JWT Token Payload:" >&2
    JWT_PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
    
    # Add padding if needed for base64 decoding
    case $((${#JWT_PAYLOAD} % 4)) in
        2) JWT_PAYLOAD="${JWT_PAYLOAD}==" ;;
        3) JWT_PAYLOAD="${JWT_PAYLOAD}=" ;;
    esac
    
    DECODED=$(echo "$JWT_PAYLOAD" | base64 -d 2>/dev/null)
    if [[ $? -eq 0 && -n "$DECODED" ]]; then
        if command -v jq >/dev/null 2>&1; then
            echo "$DECODED" | jq .
        else
            echo "$DECODED"
        fi
    else
        echo "❌ Failed to decode token payload" >&2
        exit 1
    fi
    echo "" >&2
    echo "🎯 Raw token (for copying):" >&2
    echo "$ACCESS_TOKEN"
else
    echo "✅ JWT Token generated successfully!" >&2
    echo "   Expires in: ${EXPIRES_IN:-unknown} seconds" >&2
    echo "" >&2
    
    # Decode and show JWT payload by default
    echo "🔍 JWT Token Payload:" >&2
    JWT_PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
    
    # Add padding if needed for base64 decoding
    case $((${#JWT_PAYLOAD} % 4)) in
        2) JWT_PAYLOAD="${JWT_PAYLOAD}==" ;;
        3) JWT_PAYLOAD="${JWT_PAYLOAD}=" ;;
    esac
    
    DECODED=$(echo "$JWT_PAYLOAD" | base64 -d 2>/dev/null)
    if [[ $? -eq 0 && -n "$DECODED" ]]; then
        if command -v jq >/dev/null 2>&1; then
            echo "$DECODED" | jq .
        else
            echo "$DECODED"
        fi
    else
        echo "❌ Failed to decode token payload" >&2
    fi
    
    echo "" >&2
    echo "💡 Usage examples:" >&2
    echo "   # Analytics API:" >&2
    echo "   curl -H \"Authorization: Bearer TOKEN\" http://localhost:8088/c/customer1/cluster1/keys" >&2
    echo "" >&2
    echo "   # CubeJS Security Context:" >&2
    echo "   Paste the token below into the CubeJS UI 'Edit Security Context' at http://localhost:4001" >&2
    echo "" >&2
    echo "🎯 Raw JWT Token:" >&2
    echo "$ACCESS_TOKEN"
fi