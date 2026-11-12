#!/bin/sh
set -e

CERT_DIR="/etc/nginx/certs"
mkdir -p "$CERT_DIR"

if [ ! -f "$CERT_DIR/server.crt" ] || [ ! -f "$CERT_DIR/server.key" ]; then
  echo "Generating self-signed TLS certificate for GlobalR Prototype..."
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$CERT_DIR/server.key" \
    -out "$CERT_DIR/server.crt" \
    -subj "/C=VE/ST=Miranda/L=Caracas/O=GlobalR/OU=IT/CN=globalr-inventory.local"
fi

exec nginx -g "daemon off;"
