#!/bin/bash

set -e

DOMAIN=$1
BACKEND_IP=$2
BACKEND_PORT=$3

CONF_FILE="/etc/nginx/sites-available/${DOMAIN}.conf"
WEB_ROOT="/var/www/${DOMAIN}"

echo "Setting up virtual host for $DOMAIN"

# Step 1: Create nginx conf with proxy settings
sudo tee "$CONF_FILE" > /dev/null <<EOF
server {
    listen 80;
    listen [::]:80;

    server_name $DOMAIN;

    location / {
        proxy_pass http://$BACKEND_IP:$BACKEND_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Step 2: Create web root and sample index.html
sudo mkdir -p $WEB_ROOT
echo "<h1>Welcome to $DOMAIN</h1>" | sudo tee $WEB_ROOT/index.html > /dev/null

# Step 3: Symlink to sites-enabled
sudo ln -sf $CONF_FILE /etc/nginx/sites-enabled/

# Step 4: Reload nginx to apply config
sudo nginx -t && sudo systemctl reload nginx

# Step 5: Issue SSL certificate using Certbot
sudo certbot --nginx -d $DOMAIN --non-interactive --agree-tos -m admin@$DOMAIN

# Step 6: Reload nginx again
sudo nginx -t && sudo systemctl reload nginx

# Step 7: Add domain to /etc/hosts if not already present
if ! grep -q "127.0.0.1[[:space:]]\+$DOMAIN" /etc/hosts; then
    echo "Adding $DOMAIN to /etc/hosts"
    echo "127.0.0.1 $DOMAIN" | sudo tee -a /etc/hosts > /dev/null
else
    echo "$DOMAIN already exists in /etc/hosts"
fi

echo "✅ Virtual host setup completed for $DOMAIN"
