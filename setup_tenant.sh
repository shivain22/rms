#!/bin/bash

# =============================================================================
# Tenant Provisioning Script
# =============================================================================
# This script automates the complete setup of a new tenant including:
# 1. DNS record creation (via Hostinger API)
# 2. Nginx virtual host setup with SSL
# 3. Tenant registration in RMS application (Keycloak realm + database)
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =============================================================================
# Configuration - Update these values
# =============================================================================
HOSTINGER_API_TOKEN="${HOSTINGER_API_TOKEN:-}"
BASE_DOMAIN="atparui.com"
AUTH_DOMAIN="auth.atparui.com"
GATEWAY_URL="https://rmsgateway.atparui.com"
KEYCLOAK_ADMIN_URL="https://auth.atparui.com"

# Backend configuration (where services run)
BACKEND_IP="${BACKEND_IP:-127.0.0.1}"
GATEWAY_PORT="${GATEWAY_PORT:-9293}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-9292}"

# Server IP for DNS A record (your server's public IP)
SERVER_PUBLIC_IP="${SERVER_PUBLIC_IP:-}"

# =============================================================================
# Functions
# =============================================================================

print_step() {
    echo -e "${BLUE}==>${NC} $1"
}

print_success() {
    echo -e "${GREEN}✅${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

print_error() {
    echo -e "${RED}❌${NC} $1"
}

usage() {
    echo "Usage: $0 <tenant_id> <tenant_name> [options]"
    echo ""
    echo "Arguments:"
    echo "  tenant_id      Unique identifier for the tenant (e.g., pizzahut)"
    echo "  tenant_name    Display name for the tenant (e.g., 'Pizza Hut')"
    echo ""
    echo "Options:"
    echo "  --skip-dns         Skip DNS record creation"
    echo "  --skip-nginx       Skip nginx virtual host setup"
    echo "  --skip-ssl         Skip SSL certificate issuance"
    echo "  --skip-rms         Skip RMS tenant registration"
    echo "  --dns-only         Only create DNS record"
    echo "  --nginx-only       Only setup nginx (assumes DNS is ready)"
    echo ""
    echo "Environment Variables:"
    echo "  HOSTINGER_API_TOKEN    Hostinger API token for DNS management"
    echo "  SERVER_PUBLIC_IP       Public IP for DNS A record"
    echo "  BACKEND_IP             Backend server IP (default: 127.0.0.1)"
    echo "  GATEWAY_PORT           Gateway service port (default: 9293)"
    echo ""
    echo "Examples:"
    echo "  $0 pizzahut 'Pizza Hut'"
    echo "  $0 mcdonalds 'McDonald\\'s' --skip-dns"
    echo "  $0 subway 'Subway' --dns-only"
    exit 1
}

# =============================================================================
# Step 1: Create DNS Record via Hostinger API
# =============================================================================
create_dns_record() {
    local subdomain=$1
    local record_type="${2:-A}"
    local target="${3:-$SERVER_PUBLIC_IP}"
    
    print_step "Creating DNS record: ${subdomain}.${BASE_DOMAIN} -> ${target}"
    
    if [ -z "$HOSTINGER_API_TOKEN" ]; then
        print_warning "HOSTINGER_API_TOKEN not set. Skipping DNS creation."
        print_warning "Please create DNS record manually: ${subdomain}.${BASE_DOMAIN} -> ${target}"
        return 1
    fi
    
    if [ -z "$target" ]; then
        print_error "SERVER_PUBLIC_IP not set. Cannot create DNS A record."
        return 1
    fi
    
    # Create A record via Hostinger API
    local response=$(curl -s -X POST "https://api.hostinger.com/api/dns/v1/zones/${BASE_DOMAIN}" \
        -H "Authorization: Bearer ${HOSTINGER_API_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{
            \"type\": \"${record_type}\",
            \"name\": \"${subdomain}\",
            \"content\": \"${target}\",
            \"ttl\": 3600
        }")
    
    # Check if successful
    if echo "$response" | grep -q "error"; then
        print_error "Failed to create DNS record: $response"
        return 1
    fi
    
    print_success "DNS record created: ${subdomain}.${BASE_DOMAIN}"
    
    # Wait for DNS propagation
    print_step "Waiting for DNS propagation (30 seconds)..."
    sleep 30
    
    return 0
}

# =============================================================================
# Step 2: Setup Nginx Virtual Host (from original setup_vh.sh)
# =============================================================================
setup_nginx_virtualhost() {
    local domain=$1
    local backend_ip=$2
    local backend_port=$3
    
    local conf_file="/etc/nginx/sites-available/${domain}.conf"
    local web_root="/var/www/${domain}"
    
    print_step "Setting up nginx virtual host for ${domain}"
    
    # Create nginx conf with proxy settings
    sudo tee "$conf_file" > /dev/null <<EOF
server {
    listen 80;
    listen [::]:80;

    server_name ${domain};

    location / {
        proxy_pass http://${backend_ip}:${backend_port};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host \$host;
        proxy_cache_bypass \$http_upgrade;
        
        # Increased buffer sizes for OAuth responses
        proxy_buffer_size 128k;
        proxy_buffers 4 256k;
        proxy_busy_buffers_size 256k;
    }
}
EOF
    
    # Create web root
    sudo mkdir -p "$web_root"
    echo "<h1>Welcome to ${domain}</h1>" | sudo tee "$web_root/index.html" > /dev/null
    
    # Symlink to sites-enabled
    sudo ln -sf "$conf_file" /etc/nginx/sites-enabled/
    
    # Test and reload nginx
    sudo nginx -t && sudo systemctl reload nginx
    
    print_success "Nginx virtual host configured for ${domain}"
}

# =============================================================================
# Step 3: Issue SSL Certificate
# =============================================================================
setup_ssl() {
    local domain=$1
    
    print_step "Issuing SSL certificate for ${domain}"
    
    # Issue SSL certificate using Certbot
    sudo certbot --nginx -d "$domain" --non-interactive --agree-tos -m "admin@${BASE_DOMAIN}" || {
        print_warning "Certbot failed. You may need to run manually: sudo certbot --nginx -d ${domain}"
        return 1
    }
    
    # Reload nginx
    sudo nginx -t && sudo systemctl reload nginx
    
    print_success "SSL certificate issued for ${domain}"
}

# =============================================================================
# Step 4: Register Tenant in RMS Application
# =============================================================================
register_tenant_in_rms() {
    local tenant_id=$1
    local tenant_name=$2
    local tenant_domain="${tenant_id}.${BASE_DOMAIN}"
    
    print_step "Registering tenant in RMS application: ${tenant_id}"
    
    # Get admin token from Keycloak (gateway realm)
    local token_response=$(curl -s -X POST "${KEYCLOAK_ADMIN_URL}/realms/gateway/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        -d "client_id=gateway-admin-client" \
        -d "client_secret=K8mN2pQ7rT5vW9xZ1aB3cD4eF6gH8j")
    
    local access_token=$(echo "$token_response" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$access_token" ]; then
        print_warning "Could not obtain admin token. Manual tenant registration required."
        print_warning "Register tenant via RMS Admin UI: ${GATEWAY_URL}/admin/tenants"
        return 1
    fi
    
    # Create tenant via RMS API
    local create_response=$(curl -s -X POST "${GATEWAY_URL}/api/tenants" \
        -H "Authorization: Bearer ${access_token}" \
        -H "Content-Type: application/json" \
        -d "{
            \"tenantId\": \"${tenant_id}\",
            \"name\": \"${tenant_name}\",
            \"domain\": \"${tenant_domain}\",
            \"databaseType\": \"PLATFORM\",
            \"status\": \"ACTIVE\"
        }")
    
    if echo "$create_response" | grep -q "error\|Error"; then
        print_error "Failed to create tenant: $create_response"
        return 1
    fi
    
    print_success "Tenant registered in RMS: ${tenant_id}"
    
    # The RMS application will automatically:
    # - Create Keycloak realm for the tenant
    # - Provision database for the tenant
    # - Configure OAuth clients
    
    return 0
}

# =============================================================================
# Step 5: Update /etc/hosts (for local testing)
# =============================================================================
update_hosts_file() {
    local domain=$1
    
    if ! grep -q "127.0.0.1[[:space:]]\+${domain}" /etc/hosts; then
        print_step "Adding ${domain} to /etc/hosts"
        echo "127.0.0.1 ${domain}" | sudo tee -a /etc/hosts > /dev/null
        print_success "Added ${domain} to /etc/hosts"
    else
        print_warning "${domain} already exists in /etc/hosts"
    fi
}

# =============================================================================
# Step 6: Verify Setup
# =============================================================================
verify_setup() {
    local domain=$1
    
    print_step "Verifying setup for ${domain}..."
    
    # Check DNS resolution
    if host "${domain}" > /dev/null 2>&1; then
        print_success "DNS resolves correctly"
    else
        print_warning "DNS not yet resolving (may take time to propagate)"
    fi
    
    # Check HTTPS
    local https_status=$(curl -s -o /dev/null -w "%{http_code}" "https://${domain}/" --max-time 10 2>/dev/null || echo "000")
    if [ "$https_status" != "000" ]; then
        print_success "HTTPS is working (status: ${https_status})"
    else
        print_warning "HTTPS not yet available"
    fi
}

# =============================================================================
# Main Script
# =============================================================================

# Parse arguments
TENANT_ID=""
TENANT_NAME=""
SKIP_DNS=false
SKIP_NGINX=false
SKIP_SSL=false
SKIP_RMS=false
DNS_ONLY=false
NGINX_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-dns)
            SKIP_DNS=true
            shift
            ;;
        --skip-nginx)
            SKIP_NGINX=true
            shift
            ;;
        --skip-ssl)
            SKIP_SSL=true
            shift
            ;;
        --skip-rms)
            SKIP_RMS=true
            shift
            ;;
        --dns-only)
            DNS_ONLY=true
            shift
            ;;
        --nginx-only)
            NGINX_ONLY=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            if [ -z "$TENANT_ID" ]; then
                TENANT_ID=$1
            elif [ -z "$TENANT_NAME" ]; then
                TENANT_NAME=$1
            fi
            shift
            ;;
    esac
done

# Validate required arguments
if [ -z "$TENANT_ID" ]; then
    print_error "Tenant ID is required"
    usage
fi

if [ -z "$TENANT_NAME" ]; then
    TENANT_NAME=$TENANT_ID
fi

# Construct domain
TENANT_DOMAIN="${TENANT_ID}.${BASE_DOMAIN}"

echo ""
echo "=============================================="
echo "  Tenant Provisioning: ${TENANT_ID}"
echo "=============================================="
echo "  Domain:      ${TENANT_DOMAIN}"
echo "  Name:        ${TENANT_NAME}"
echo "  Backend:     ${BACKEND_IP}:${GATEWAY_PORT}"
echo "=============================================="
echo ""

# DNS Only mode
if [ "$DNS_ONLY" = true ]; then
    create_dns_record "$TENANT_ID"
    exit $?
fi

# Nginx Only mode
if [ "$NGINX_ONLY" = true ]; then
    setup_nginx_virtualhost "$TENANT_DOMAIN" "$BACKEND_IP" "$GATEWAY_PORT"
    if [ "$SKIP_SSL" = false ]; then
        setup_ssl "$TENANT_DOMAIN"
    fi
    update_hosts_file "$TENANT_DOMAIN"
    exit 0
fi

# Full provisioning
STEPS_COMPLETED=0

# Step 1: DNS
if [ "$SKIP_DNS" = false ]; then
    if create_dns_record "$TENANT_ID"; then
        ((STEPS_COMPLETED++))
    fi
else
    print_warning "Skipping DNS record creation"
fi

# Step 2: Nginx
if [ "$SKIP_NGINX" = false ]; then
    setup_nginx_virtualhost "$TENANT_DOMAIN" "$BACKEND_IP" "$GATEWAY_PORT"
    ((STEPS_COMPLETED++))
else
    print_warning "Skipping nginx setup"
fi

# Step 3: SSL
if [ "$SKIP_SSL" = false ] && [ "$SKIP_NGINX" = false ]; then
    if setup_ssl "$TENANT_DOMAIN"; then
        ((STEPS_COMPLETED++))
    fi
else
    print_warning "Skipping SSL certificate"
fi

# Step 4: RMS Registration
if [ "$SKIP_RMS" = false ]; then
    if register_tenant_in_rms "$TENANT_ID" "$TENANT_NAME"; then
        ((STEPS_COMPLETED++))
    fi
else
    print_warning "Skipping RMS tenant registration"
fi

# Step 5: Update hosts file
update_hosts_file "$TENANT_DOMAIN"

# Step 6: Verify
echo ""
verify_setup "$TENANT_DOMAIN"

echo ""
echo "=============================================="
echo "  Provisioning Complete!"
echo "=============================================="
echo ""
echo "  Tenant URL:    https://${TENANT_DOMAIN}"
echo "  Keycloak:      https://${AUTH_DOMAIN}/realms/${TENANT_ID}"
echo "  Admin Console: https://${AUTH_DOMAIN}/admin/${TENANT_ID}/console"
echo ""
echo "  Next steps:"
echo "  1. Verify DNS propagation: dig ${TENANT_DOMAIN}"
echo "  2. Test the tenant URL in browser"
echo "  3. Create users in Keycloak admin console"
echo ""
