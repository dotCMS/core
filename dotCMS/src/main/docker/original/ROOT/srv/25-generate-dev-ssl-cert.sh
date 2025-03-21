#!/bin/bash

set -e
# Generate self-signed SSL certificate for development
# Note this certificate will present as untrusted in browsers and is just intended as a placeholder
# The recommendation is to use a trusted certificate for production
# In develoment to create a certificate with the mkcert tool

DAYS_VALID=365  # 1 years validity for development
DOMAIN="local.dotcms.site"
CERT_FILE_NAME="${DOMAIN}.pem"
KEY_FILE_NAME="${DOMAIN}-key.pem"
CERT_FOLDER=/data/shared/assets/certs
mkdir -p $CERT_FOLDER
CERT_FILE="$CERT_FOLDER/$CERT_FILE_NAME"
KEY_FILE="$CERT_FOLDER/$KEY_FILE_NAME"

# Only generate if the certificate doesn't exist or if forced
if [[ ! -f "$CERT_FILE" || "${FORCE_GENERATE_SSL:-false}" == "true" ]]; then
    echo "Generating self-signed SSL certificate for $DOMAIN..."

    # Create a temporary config file for the SAN extension
    cat > /tmp/openssl.cnf << EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[dn]
C=US
ST=FL
L=Miami
O=dotCMS Development
OU=Development
CN=$DOMAIN

[req_ext]
subjectAltName = @alt_names

[alt_names]
DNS.1 = $DOMAIN
DNS.2 = *.$DOMAIN
EOF

    # Generate private key and certificate
    openssl req -x509 \
        -nodes \
        -days $DAYS_VALID \
        -newkey rsa:2048 \
        -keyout "$KEY_FILE" \
        -out "$CERT_FILE" \
        -config /tmp/openssl.cnf \
        -extensions req_ext

    # Clean up temporary files
    rm -f /tmp/openssl.cnf

    echo "Self-signed SSL certificate generated successfully at $CERT_FILE" "$KEY_FILE"
else
    echo "SSL certificate already exists at $CERT_FILE"
fi

# Ensure correct permissions
chmod 600 "$CERT_FILE"
chmod 600 "$KEY_FILE"
