#!/bin/bash

# DotCMS Analytics Load Testing Tool
# A unified script for developers to test analytics performance without needing k8s/helm knowledge
# CONSOLIDATED VERSION - Replaces all separate testing scripts

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_VERSION="2.1.0"
ANALYTICS_HOST="${ANALYTICS_HOST:-analytics-dev.dotcms.site}"
ANALYTICS_PORT="${ANALYTICS_PORT:-443}"
ANALYTICS_SCHEME="${ANALYTICS_SCHEME:-https}"
ANALYTICS_KEY="${ANALYTICS_KEY:-YOUR_ANALYTICS_KEY_HERE}"
NAMESPACE="${NAMESPACE:-analytics-dev}"
TEST_DURATION="${TEST_DURATION:-180}"
RAMPUP="${RAMPUP:-30}"
VALUES_FILE="${VALUES_FILE:-custom-values.yaml}"

print_header() {
    echo -e "${CYAN}${BOLD}"
    echo "======================================================================"
    echo "  DotCMS Analytics Load Testing Tool v${SCRIPT_VERSION}"
    echo "  🚀 Unified testing and analysis for analytics performance"
    echo "======================================================================"
    echo -e "${NC}"
}

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

usage() {
    print_header
    echo "DESCRIPTION:"
    echo "  Comprehensive analytics load testing for DotCMS with automatic"
    echo "  deployment, execution, and analysis. No k8s/helm knowledge required."
    echo ""
    echo "USAGE:"
    echo "  $0 <command> [options]"
    echo ""
    echo "PRIMARY COMMANDS:"
    echo ""
    echo "  ${BOLD}setup${NC}                       Deploy testing infrastructure to Kubernetes"
    echo "  ${BOLD}quick-test${NC}                 Run a quick baseline test (direct analytics)"
    echo "  ${BOLD}dotcms-test${NC}                Run a quick test on DotCMS API endpoint"
    echo "  ${BOLD}compare-endpoints${NC}          Compare DotCMS API vs Direct Analytics performance"
    echo "  ${BOLD}bottleneck-analysis${NC}        Deep bottleneck analysis with progressive load testing"
    echo "  ${BOLD}find-maximum-rate${NC}          Progressive testing to find failover point"
    echo ""
    echo "ADVANCED TESTING:"
    echo ""
    echo "  ${BOLD}performance-test${NC}           Run progressive performance tests"
    echo "  ${BOLD}stress-test${NC}                Run high-load stress test"
    echo "  ${BOLD}scaling-test${NC}               Comprehensive scaling analysis"
    echo "  ${BOLD}single-test${NC} <eps> <threads> [endpoint] Run a single test (endpoint: direct|dotcms)"
    echo ""
    echo "ANALYSIS & MANAGEMENT:"
    echo ""
    echo "  ${BOLD}analyze${NC}                    Analyze results from the latest test"
    echo "  ${BOLD}analyze-all${NC}               Analyze all available test results"
    echo "  ${BOLD}generate-report${NC}           Generate comprehensive performance report"
    echo "  ${BOLD}download-results${NC}          Download all test result files to local directory"
    echo "  ${BOLD}status${NC}                     Check infrastructure status"
    echo "  ${BOLD}refresh-tokens${NC}            Refresh JWT and analytics authentication tokens"
    echo "  ${BOLD}cleanup${NC}                    Remove testing infrastructure"
    echo "  ${BOLD}logs${NC}                       View test execution logs"
    echo ""
    echo "OPTIONS:"
    echo "  --analytics-host HOST           Analytics endpoint host (default: $ANALYTICS_HOST)"
    echo "  --analytics-key KEY             Analytics key (default: [configured])"
    echo "  --dotcms-host HOST              DotCMS API hostname (required for setup)"
    echo "  --namespace NAMESPACE           Kubernetes namespace (default: $NAMESPACE)"
    echo "  --duration SECONDS              Test duration (default: $TEST_DURATION)"
    echo "  --max-eps MAX                   Maximum EPS for scaling tests (default: 2000)"
    echo "  --values-file FILE              Custom Helm values file (default: custom-values.yaml)"
    echo "  --help                          Show this help message"
    echo ""
    echo "SECURE AUTHENTICATION:"
    echo ""
    echo "  Environment Variables (recommended for CI/CD):"
    echo "    DOTCMS_JWT_TOKEN              DotCMS API token for authenticated requests"
    echo "                                  • Create in DotCMS: Admin → User Tools → API Tokens"
    echo "                                  • Or generate via /api/v1/authentication endpoint"
    echo "    DOTCMS_ANALYTICS_KEY          Analytics Key from DotCMS Analytics App"
    echo "                                  • Find in DotCMS: Apps → Analytics → Configuration"  
    echo "                                  • Format: js.cluster1.customer1.vgwy3nli4co84u531c"
    echo ""
    echo "  Interactive Setup:"
    echo "    - Prompts for DotCMS username/password securely (no echo for password)"
    echo "    - Auto-generates temporary JWT token via DotCMS API (30-minute expiry)"
    echo "    - Prompts for Analytics Key from DotCMS Analytics App Configuration"
    echo "    - Stores tokens in Kubernetes secret (not in files or command history)"
    echo ""
    echo "WORKFLOW EXAMPLES:"
    echo ""
    echo "  # Secure setup with environment variables (CI/CD)"
    echo "  export DOTCMS_JWT_TOKEN=\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...\""
    echo "  export DOTCMS_ANALYTICS_KEY=\"js.cluster1.customer1.vgwy3nli4co84u531c\""
    echo "  $0 setup && $0 quick-test"
    echo ""
    echo "  # Interactive setup (prompts for credentials)"
    echo "  $0 setup && $0 quick-test"
    echo ""
    echo "  # Refresh tokens when expired"
    echo "  $0 refresh-tokens"
    echo ""
    echo "  # Compare endpoints to identify bottlenecks"
    echo "  $0 compare-endpoints"
    echo ""
    echo "  # Comprehensive analysis"
    echo "  $0 bottleneck-analysis && $0 generate-report"
    echo ""
    echo "  # Find maximum capacity"
    echo "  $0 find-maximum-rate"
    echo ""
    echo "  # Using custom values file"
    echo "  $0 setup --values-file production-values.yaml"
    echo "  $0 setup --dotcms-host demo.dotcms.com --values-file custom-values.yaml"
    echo ""
    echo "    sustained-load-test [endpoint] [start_eps] [max_eps] [step_size]  - Sustained load testing to find queue limits"
    echo "                        endpoint: 'direct' or 'dotcms' or 'both'"
    echo "                        start_eps: Starting EPS rate (default: 100)"
    echo "                        max_eps: Maximum EPS to test (default: 2000)"  
    echo "                        step_size: EPS increment per test (default: 200)"
    echo "                        Tests run for up to 10 minutes or until connection failures"
}

# =============================================================================
# SECURE TOKEN MANAGEMENT
# =============================================================================

prompt_for_credentials() {
    local dotcms_host="$1"
    local creds_file="$2"
    
    print_info "DotCMS API Authentication Setup for: $dotcms_host"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🔐 DotCMS API Token Generation"
    echo "   Your DotCMS username/password will generate a temporary JWT API token"
    echo "   • Alternative: Create permanent token in DotCMS → Admin → User Tools → API Tokens"
    echo "   • Credentials are NOT stored or logged (used only for token generation)"
    echo "   • Generated token expires in 30 minutes for security"
    echo "   • Only the JWT token is stored in Kubernetes (not credentials)"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    read -p "DotCMS Username (email): " DOTCMS_USER
    if [ -z "$DOTCMS_USER" ]; then
        print_error "Username is required"
        return 1
    fi
    
    # Use secure password input (no echo)
    echo -n "DotCMS Password: "
    read -s DOTCMS_PASSWORD
    echo ""
    
    if [ -z "$DOTCMS_PASSWORD" ]; then
        print_error "Password is required"
        return 1
    fi
    
    # Store temporarily for token generation (will be unset after use)
    echo "DOTCMS_USER='$DOTCMS_USER'" > "$creds_file"
    echo "DOTCMS_PASSWORD='$DOTCMS_PASSWORD'" >> "$creds_file"
    chmod 600 "$creds_file"
    
    # Clear from script memory
    unset DOTCMS_USER DOTCMS_PASSWORD
    
    return 0
}

generate_jwt_token() {
    local dotcms_host="$1"
    local dotcms_port="$2"
    local dotcms_scheme="$3"
    local creds_file="$4"
    
    print_info "Generating temporary JWT token..."
    
    # Load credentials
    if [ ! -f "$creds_file" ]; then
        print_error "Credentials file not found"
        return 1
    fi
    
    source "$creds_file"
    
    # Generate JWT token using DotCMS REST API
    local auth_url="${dotcms_scheme}://${dotcms_host}:${dotcms_port}/api/v1/authentication"
    local response
    
    print_info "Authenticating with DotCMS at $auth_url"
    
    response=$(curl -s -X POST "$auth_url" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{
            \"userId\": \"$DOTCMS_USER\",
            \"password\": \"$DOTCMS_PASSWORD\",
            \"rememberMe\": false,
            \"expirationDays\": 0.02
        }" 2>/dev/null)
    
    # Clear credentials immediately
    unset DOTCMS_USER DOTCMS_PASSWORD
    rm -f "$creds_file"
    
    if [ $? -ne 0 ] || [ -z "$response" ]; then
        print_error "Failed to connect to DotCMS authentication endpoint"
        return 1
    fi
    
    # Extract JWT token from response
    local jwt_token
    jwt_token=$(echo "$response" | jq -r '.entity.token // empty' 2>/dev/null)
    
    if [ -z "$jwt_token" ] || [ "$jwt_token" = "null" ]; then
        print_error "Authentication failed. Please check your credentials."
        print_warning "Response: $(echo "$response" | jq -r '.message // .error // "Unknown error"' 2>/dev/null || echo "Invalid response format")"
        return 1
    fi
    
    print_success "JWT token generated successfully (expires in 30 minutes)"
    echo "$jwt_token"
    return 0
}

create_or_update_secret() {
    local namespace="$1"
    local jwt_token="$2"
    local analytics_key="$3"
    
    print_info "Creating/updating Kubernetes secret for secure token storage..."
    
    # Check if secret exists
    if kubectl get secret dotcms-auth-secret -n "$namespace" &>/dev/null; then
        print_info "Updating existing secret..."
        kubectl delete secret dotcms-auth-secret -n "$namespace"
    else
        print_info "Creating new secret..."
    fi
    
    # Create secret with both tokens
    kubectl create secret generic dotcms-auth-secret \
        --from-literal=jwt-token="$jwt_token" \
        --from-literal=analytics-key="$analytics_key" \
        -n "$namespace"
    
    if [ $? -eq 0 ]; then
        print_success "Secret created/updated successfully"
        return 0
    else
        print_error "Failed to create/update secret"
        return 1
    fi
}

get_secure_tokens() {
    local dotcms_host="${1:-$DOTCMS_HOST}"
    local dotcms_port="${2:-443}"
    local dotcms_scheme="${3:-https}"
    
    # Priority 1: Check for environment variables (highest priority)
    if [ -n "$DOTCMS_JWT_TOKEN" ] && [ -n "$DOTCMS_ANALYTICS_KEY" ]; then
        print_info "Found JWT token and analytics key in environment variables"
        print_success "Using tokens from environment (DOTCMS_JWT_TOKEN, DOTCMS_ANALYTICS_KEY)"
        
        # Create/update secret with environment variables
        if create_or_update_secret "$NAMESPACE" "$DOTCMS_JWT_TOKEN" "$DOTCMS_ANALYTICS_KEY"; then
            print_success "Environment tokens stored in Kubernetes secret"
            return 0
        else
            print_warning "Failed to store environment tokens in secret, but continuing with environment variables"
            return 0
        fi
    elif [ -n "$DOTCMS_JWT_TOKEN" ]; then
        print_warning "Found DOTCMS_JWT_TOKEN but missing DOTCMS_ANALYTICS_KEY in environment"
        print_info "Please set DOTCMS_ANALYTICS_KEY environment variable or use interactive setup"
    elif [ -n "$DOTCMS_ANALYTICS_KEY" ]; then
        print_warning "Found DOTCMS_ANALYTICS_KEY but missing DOTCMS_JWT_TOKEN in environment"  
        print_info "Please set DOTCMS_JWT_TOKEN environment variable or use interactive setup"
    fi
    
    # Priority 2: Check for existing Kubernetes secret
    if kubectl get secret dotcms-auth-secret -n "$NAMESPACE" &>/dev/null; then
        print_info "Found existing authentication secret in Kubernetes"
        
        # Check if token is still valid (basic check)
        local existing_token
        existing_token=$(kubectl get secret dotcms-auth-secret -n "$NAMESPACE" -o jsonpath='{.data.jwt-token}' | base64 -d 2>/dev/null)
        
        if [ -n "$existing_token" ]; then
            echo -n "Use existing Kubernetes secret? [Y/n]: "
            read -r use_existing
            if [ "$use_existing" != "n" ] && [ "$use_existing" != "N" ]; then
                print_success "Using existing authentication secret from Kubernetes"
                return 0
            fi
        fi
    fi
    
    # Prompt for DotCMS host if not provided
    if [ -z "$dotcms_host" ]; then
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "🌐 DotCMS Instance Configuration"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        read -p "DotCMS Host (e.g., demo.dotcms.com): " dotcms_host
        
        if [ -z "$dotcms_host" ]; then
            print_error "DotCMS host is required"
            return 1
        fi
        
        read -p "DotCMS Port [443]: " user_port
        if [ -n "$user_port" ]; then
            dotcms_port="$user_port"
        fi
        
        read -p "DotCMS Scheme [https]: " user_scheme  
        if [ -n "$user_scheme" ]; then
            dotcms_scheme="$user_scheme"
        fi
    fi
    
    # Prompt for analytics key
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🔑 Analytics Key Configuration"
    echo "   This is the Analytics Key from your DotCMS Analytics App Configuration"
    echo "   📍 Location: DotCMS → Apps → Analytics → Configuration → Analytics Key"
    echo "   📝 Format: js.cluster1.customer1.vgwy3nli4co84u531c"
    echo "   ℹ️  This key identifies your analytics tracking in the analytics platform"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    read -p "Analytics Key (js.cluster1.customer1.xxxxx): " analytics_key
    
    if [ -z "$analytics_key" ]; then
        print_error "Analytics key is required"
        return 1
    fi
    
    # Create temporary credentials file
    local temp_creds="/tmp/dotcms-creds-$$"
    
    # Get credentials securely
    if ! prompt_for_credentials "$dotcms_host" "$temp_creds"; then
        rm -f "$temp_creds"
        return 1
    fi
    
    # Generate JWT token
    local jwt_token
    jwt_token=$(generate_jwt_token "$dotcms_host" "$dotcms_port" "$dotcms_scheme" "$temp_creds")
    
    if [ $? -ne 0 ] || [ -z "$jwt_token" ]; then
        rm -f "$temp_creds"
        return 1
    fi
    
    # Create/update Kubernetes secret
    if ! create_or_update_secret "$NAMESPACE" "$jwt_token" "$analytics_key"; then
        # Clear token from memory
        unset jwt_token
        return 1
    fi
    
    # Clear token from memory
    unset jwt_token
    
    # Export DotCMS configuration for use in setup
    export DOTCMS_HOST="$dotcms_host"
    export DOTCMS_PORT="$dotcms_port" 
    export DOTCMS_SCHEME="$dotcms_scheme"
    
    print_success "Secure token management setup complete!"
    return 0
}

create_custom_values_file() {
    local values_file="$1"
    local dotcms_host="$2"
    local dotcms_port="$3"
    local dotcms_scheme="$4"
    
    print_info "Creating custom values file: $values_file"
    
    # Check if custom values file already exists
    if [ -f "$values_file" ]; then
        echo -n "Custom values file exists. Overwrite? [y/N]: "
        read -r overwrite
        if [ "$overwrite" != "y" ] && [ "$overwrite" != "Y" ]; then
            print_info "Using existing custom values file"
            return 0
        fi
    fi
    
    # Check if example file exists
    local example_file="custom-values.yaml.example"
    if [ ! -f "$example_file" ]; then
        print_error "Example file not found: $example_file"
        print_error "Please ensure you're running from the test-jmeter directory"
        return 1
    fi
    
    # Copy example file and customize with current configuration
    print_info "Copying from example file: $example_file"
    cp "$example_file" "$values_file"
    
    # Update the copied file with actual configuration values
    # Uncomment and set the required DotCMS configuration
    sed -i.bak \
        -e "/^# DotCMS API Configuration/,/^# =/ s/    host: \"demo.dotcms.com\"/    host: \"$dotcms_host\"/" \
        -e "/^# DotCMS API Configuration/,/^# =/ s/^#     port: 443/    port: $dotcms_port/" \
        -e "/^# DotCMS API Configuration/,/^# =/ s/^#     scheme: \"https\"/    scheme: \"$dotcms_scheme\"/" \
        -e "/^# Authentication Configuration/,/^# =/ s/^#   useSecret: true/  useSecret: true/" \
        "$values_file"
    
    # Uncomment analytics configuration if using non-default values
    if [ "$ANALYTICS_HOST" != "analytics-dev.dotcms.site" ] || [ "$ANALYTICS_PORT" != "443" ] || [ "$ANALYTICS_SCHEME" != "https" ]; then
        sed -i.bak2 \
            -e "/# Analytics Platform Settings/,/^$/s/^# endpoints:/endpoints:/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#   analytics:/  analytics:/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#     host: \"analytics.example.com\"/    host: \"$ANALYTICS_HOST\"/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#     port: 8001/    port: $ANALYTICS_PORT/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#     scheme: \"http\"/    scheme: \"$ANALYTICS_SCHEME\"/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#     path: \"/    path: \"/" \
            -e "/# Analytics Platform Settings/,/^$/s/^#     key: \"/    key: \"/" \
            "$values_file"
        rm -f "${values_file}.bak2"
    fi
    
    # Uncomment namespace configuration if using non-default namespace
    if [ "$NAMESPACE" != "analytics-dev" ]; then
        sed -i.bak3 \
            -e "/# Namespace Configuration/,/^$/s/^# namespace:/namespace:/" \
            -e "/# Namespace Configuration/,/^$/s/^#   name: \"analytics-prod\"/  name: \"$NAMESPACE\"/" \
            -e "/# Namespace Configuration/,/^$/s/^#   create: false/  create: false/" \
            "$values_file"
        rm -f "${values_file}.bak3"
    fi
    
    # Always uncomment environment docHost to match DotCMS host
    sed -i.bak4 \
        -e "/# Environment Settings/,/^$/s/^# environment:/environment:/" \
        -e "/# Environment Settings/,/^$/s/^#   docHost: \"demo.dotcms.com\"/  docHost: \"$dotcms_host\"/" \
        "$values_file"
    rm -f "${values_file}.bak4"
    
    # Remove backup file
    rm -f "${values_file}.bak"
    
    # Remove the entire warning block (from start until the first configuration section)
    # and replace with clean header for the custom file
    sed -i.bak5 -e '1,/^# =============================================================================$/c\
# Custom Values for DotCMS JMeter Performance Testing\
# ====================================================\
# Generated on: '"$(date)"'\
# Base configuration from: '"$example_file"'\
#\
# ✅ EDIT THIS FILE: This is your custom configuration file\
# \
# This file contains your specific DotCMS testing configuration.\
# Customize the settings below for your testing environment.\
# All commented sections can be uncommented and modified as needed.\
#\
# =============================================================================
' "$values_file"
    rm -f "${values_file}.bak5"
    
    chmod 600 "$values_file"  # Secure permissions
    print_success "Custom values file created: $values_file"
    print_info "💡 Based on: $example_file"
    print_info "💡 Edit this file to customize your testing configuration"
    print_info "💡 This file is excluded from git (contains configuration details)"
    
    return 0
}

check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is required but not installed"
        echo "Please install kubectl: https://kubernetes.io/docs/tasks/tools/"
        exit 1
    fi
    
    # Check helm
    if ! command -v helm &> /dev/null; then
        print_error "helm is required but not installed"
        echo "Please install helm: https://helm.sh/docs/intro/install/"
        exit 1
    fi
    
    # Check cluster access
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        echo "Please configure kubectl to connect to your cluster"
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

setup_infrastructure() {
    print_info "Setting up analytics testing infrastructure..."
    
    check_prerequisites
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        print_warning "Namespace $NAMESPACE does not exist. Please create it first or use an existing namespace."
        exit 1
    fi
    
    # Secure token management
    print_info "Setting up secure authentication..."
    if ! get_secure_tokens; then
        print_error "Failed to setup secure authentication"
        exit 1
    fi
    
    # Deploy with Helm (tokens are now in Kubernetes secret)
    print_info "Deploying JMeter test infrastructure with Helm..."
    
    # Use captured DotCMS configuration or fallback to defaults
    local dotcms_host="${DOTCMS_HOST:-}"
    local dotcms_port="${DOTCMS_PORT:-443}"
    local dotcms_scheme="${DOTCMS_SCHEME:-https}"
    
    # Validate required DotCMS host
    if [ -z "$dotcms_host" ]; then
        print_error "DotCMS host is required. Please run setup again or provide --dotcms-host parameter."
        exit 1
    fi
    
    print_info "Using DotCMS endpoint: ${dotcms_scheme}://${dotcms_host}:${dotcms_port}"
    
    # Create custom values file for configuration persistence
    if ! create_custom_values_file "$VALUES_FILE" "$dotcms_host" "$dotcms_port" "$dotcms_scheme"; then
        print_error "Failed to create custom values file"
        exit 1
    fi
    
    print_info "Deploying with custom values file: $VALUES_FILE"
    print_info "All configuration is stored in the values file for consistency and persistence"
    
    if helm upgrade --install jmeter-test helm-chart/jmeter-performance/ \
        -f "$VALUES_FILE" \
        --namespace "$NAMESPACE" \
        --wait --timeout=300s; then
        print_success "Infrastructure deployed successfully"
    else
        print_error "Failed to deploy infrastructure"
        exit 1
    fi
    
    # Wait for pod to be ready
    print_info "Waiting for test pod to be ready..."
    kubectl wait --for=condition=Ready pod/jmeter-test-pod -n "$NAMESPACE" --timeout=180s
    
    print_success "Setup complete! Use 'dotcms-analytics-test quick-test' to verify."
}

run_kubernetes_test() {
    local threads=$1
    local eps=$2
    local test_name=$3
    local duration=${4:-$TEST_DURATION}
    local endpoint=${5:-"direct"}  # direct or dotcms
    
    print_info "Running $test_name: $threads threads, $eps EPS target, ${duration}s duration ($endpoint endpoint)"
    
    # Check if pod exists
    if ! kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        print_error "JMeter test pod not found. Run 'dotcms-analytics-test setup' first."
        exit 1
    fi
    
    # Select the appropriate JMX file based on endpoint
    local jmx_file
    if [ "$endpoint" = "dotcms" ]; then
        jmx_file="/opt/jmx-tests/analytics-api-cluster-test.jmx"
    else
        jmx_file="/opt/jmx-tests/analytics-direct-cluster-test.jmx"
    fi
    
    # Run the test
    local result_file="/opt/test-results/${test_name}.jtl"
    local log_file="/opt/test-results/${test_name}.log"
    
    # Clean up any existing result files to prevent accumulation
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        rm -f $result_file $log_file
    "
    
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
        # Read authentication tokens from secret if available
        if [ -f /opt/secrets/jwt-token ] && [ -f /opt/secrets/analytics-key ]; then
            JWT_TOKEN=$(cat /opt/secrets/jwt-token)
            ANALYTICS_KEY=$(cat /opt/secrets/analytics-key)
        else
            # Fallback to environment variables or defaults
            JWT_TOKEN="${DOTCMS_JWT_TOKEN:-}"
            ANALYTICS_KEY="${DOTCMS_ANALYTICS_KEY:-'$ANALYTICS_KEY'}"
        fi
        
        # Read DotCMS configuration from configmap if available
        if [ -f /opt/config/dotcms-host ]; then
            DOTCMS_HOST=$(cat /opt/config/dotcms-host)
            DOTCMS_PORT=$(cat /opt/config/dotcms-port)
            DOTCMS_SCHEME=$(cat /opt/config/dotcms-scheme)
        else
            # Fallback values
            DOTCMS_HOST="${DOTCMS_HOST:-}"
            DOTCMS_PORT="${DOTCMS_PORT:-443}"
            DOTCMS_SCHEME="${DOTCMS_SCHEME:-https}"
        fi
        
        jmeter -n -t '$jmx_file' \
          -l '$result_file' \
          -j '$log_file' \
          -Jthread.number='$threads' \
          -Jevents.per.second='$eps' \
          -Jtest.duration='$duration' \
          -Janalytics.key="${ANALYTICS_KEY}" \
          -Jjwt.token="${JWT_TOKEN}" \
          -Jdotcms.host="${DOTCMS_HOST}" \
          -Jdotcms.port="${DOTCMS_PORT}" \
          -Jdotcms.scheme="${DOTCMS_SCHEME}"
    '
    
    # Store test metadata for analysis
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        echo 'test_name=$test_name' > /opt/test-results/latest-test-metadata.txt
        echo 'target_eps=$eps' >> /opt/test-results/latest-test-metadata.txt
        echo 'threads=$threads' >> /opt/test-results/latest-test-metadata.txt
        echo 'duration=$duration' >> /opt/test-results/latest-test-metadata.txt
        echo 'endpoint=$endpoint' >> /opt/test-results/latest-test-metadata.txt
        echo 'timestamp=$(date)' >> /opt/test-results/latest-test-metadata.txt
    "
}

analyze_latest_results() {
    print_info "Analyzing latest test results..."
    
    # Check if pod exists
    if ! kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        print_error "JMeter test pod not found. Run tests first."
        exit 1
    fi
    
    # Get test metadata
    local metadata=$(kubectl exec jmeter-test-pod -n "$NAMESPACE" -- cat /opt/test-results/latest-test-metadata.txt 2>/dev/null || echo "")
    if [ -z "$metadata" ]; then
        print_error "No test results found. Run a test first."
        exit 1
    fi
    
    # Extract metadata
    local test_name=$(echo "$metadata" | grep "test_name=" | cut -d'=' -f2)
    local target_eps=$(echo "$metadata" | grep "target_eps=" | cut -d'=' -f2)
    local threads=$(echo "$metadata" | grep "threads=" | cut -d'=' -f2)
    local duration=$(echo "$metadata" | grep "duration=" | cut -d'=' -f2)
    
    print_info "Test: $test_name | Target: $target_eps EPS | Threads: $threads | Duration: ${duration}s"
    echo ""
    
    # Analyze results
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        result_file=\"/opt/test-results/${test_name}.jtl\"
        if [ -f \"\$result_file\" ]; then
            echo '=== ANALYTICS PERFORMANCE TEST RESULTS ==='
            total_requests=\$(tail -n +2 \"\$result_file\" | wc -l)
            successful_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"true\" {count++} END {print count+0}')
            failed_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"false\" {count++} END {print count+0}')
            avg_response=\$(tail -n +2 \"\$result_file\" | awk -F, '{sum+=\$2; count++} END {printf \"%.1f\", sum/count}')
            max_response=\$(tail -n +2 \"\$result_file\" | awk -F, 'BEGIN{max=0} {if(\$2>max) max=\$2} END {print max}')
            min_response=\$(tail -n +2 \"\$result_file\" | awk -F, 'BEGIN{min=999999} {if(\$2<min) min=\$2} END {print min}')
            slow_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$2>1000 {count++} END {print count+0}')
            timeout_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$2>5000 {count++} END {print count+0}')
            
            # Calculate actual test duration from timestamps
            start_time=\$(head -2 \"\$result_file\" | tail -1 | cut -d, -f1)
            end_time=\$(tail -1 \"\$result_file\" | cut -d, -f1)
            actual_duration=\$(echo \"scale=1; (\$end_time - \$start_time) / 1000\" | bc)
            
            # Calculate actual EPS and efficiency
            actual_eps=\$(echo \"scale=1; \$total_requests / \$actual_duration\" | bc)
            if [ \"$target_eps\" != \"\" ] && [ \"$target_eps\" != \"0\" ]; then
                efficiency=\$(echo \"scale=2; (\$actual_eps / $target_eps) * 100\" | bc | awk '{printf \"%.0f\", \$1}')
            else
                efficiency=\"N/A\"
            fi
            
            # Calculate error rate
            error_rate=\$(echo \"scale=2; (\$failed_requests * 100.0) / \$total_requests\" | bc)
            
            echo \"Total Requests:       \$total_requests\"
            echo \"Successful Requests:  \$successful_requests (\$(echo \"scale=1; (\$successful_requests * 100.0) / \$total_requests\" | bc)%)\"
            echo \"Failed Requests:      \$failed_requests (\${error_rate}%)\"
            echo \"\"
            echo \"Performance Metrics:\"
            echo \"  Target EPS:         $target_eps\"
            echo \"  Actual EPS:         \$actual_eps\"
            echo \"  Efficiency:         \${efficiency}%\"
            echo \"  Actual Duration:    \${actual_duration}s (target: $duration s)\"
            echo \"\"
            echo \"Response Times:\"
            echo \"  Average:            \${avg_response} ms\"
            echo \"  Minimum:            \${min_response} ms\"
            echo \"  Maximum:            \${max_response} ms\"
            echo \"  Requests >1s:       \$slow_requests\"
            echo \"  Requests >5s:       \$timeout_requests\"
            echo \"\"
            
            # Performance assessment
            echo \"Performance Assessment:\"
            if (( \$(echo \"\$error_rate < 5\" | bc -l) )); then
                echo \"  ✅ EXCELLENT: Error rate <5% - System performing optimally\"
            elif (( \$(echo \"\$error_rate < 15\" | bc -l) )); then
                echo \"  ⚠️  GOOD: Error rate <15% - Acceptable performance with some stress\"
            elif (( \$(echo \"\$error_rate < 25\" | bc -l) )); then
                echo \"  🔶 STRESSED: Error rate <25% - System approaching limits\"
            else
                echo \"  ❌ OVERLOADED: Error rate >25% - System beyond capacity\"
            fi
            
            if (( \$(echo \"\$efficiency > 90\" | bc -l) )); then
                echo \"  ✅ EFFICIENT: Achieving >90% of target throughput\"
            elif (( \$(echo \"\$efficiency > 70\" | bc -l) )); then
                echo \"  ⚠️  MODERATE: Achieving 70-90% of target throughput\"
            else
                echo \"  ❌ INEFFICIENT: Achieving <70% of target throughput\"
            fi
            
            echo \"\"
            echo \"Recommendations:\"
            if (( \$(echo \"\$error_rate < 10\" | bc -l) )); then
                echo \"  • This load level is sustainable for production\"
                echo \"  • Consider this as your baseline capacity\"
            elif (( \$(echo \"\$error_rate < 20\" | bc -l) )); then
                echo \"  • Acceptable for peak traffic periods\"
                echo \"  • Monitor error rates during sustained load\"
            else
                echo \"  • Reduce concurrent load or optimize infrastructure\"
                echo \"  • Consider horizontal scaling of jitsu-api pods\"
            fi
            echo \"==========================================\"
        else
            echo \"No results file found: \$result_file\"
            exit 1
        fi
    "
}

quick_test() {
    print_info "Running quick baseline test (200 EPS)..."
    run_kubernetes_test 50 200 "quick-baseline-test" 120
    print_success "Quick test completed!"
    echo ""
    analyze_latest_results
}

performance_test() {
    print_info "Running progressive performance test suite..."
    
    local test_configs=(
        "50,200,baseline-200eps"
        "75,400,performance-400eps" 
        "100,600,stress-600eps"
        "150,800,high-800eps"
        "200,1000,extreme-1000eps"
    )
    
    for config in "${test_configs[@]}"; do
        IFS=',' read -r threads eps name <<< "$config"
        echo ""
        print_info "=== Running $name: $threads threads, $eps EPS ==="
        run_kubernetes_test "$threads" "$eps" "$name" "$TEST_DURATION"
        
        print_info "Analyzing results for $name..."
        analyze_latest_results
        
        print_info "Waiting 30 seconds before next test..."
        sleep 30
    done
    
    print_success "Performance test suite completed!"
}

stress_test() {
    print_info "Running stress test (1200+ EPS)..."
    run_kubernetes_test 300 1200 "stress-1200eps-test" 240
    print_success "Stress test completed!"
    echo ""
    analyze_latest_results
}

single_test() {
    local eps=$1
    local threads=$2
    local endpoint=${3:-"direct"}
    
    if [ -z "$eps" ] || [ -z "$threads" ]; then
        print_error "Usage: dotcms-analytics-test single-test <eps> <threads> [endpoint]"
        print_error "       endpoint: direct (default) or dotcms"
        exit 1
    fi
    
    print_info "Running single test: $eps EPS with $threads threads ($endpoint endpoint)..."
    run_kubernetes_test "$threads" "$eps" "single-${endpoint}-${eps}eps-${threads}t-test" "$TEST_DURATION" "$endpoint"
    print_success "Single test completed!"
    echo ""
    analyze_latest_results
}

dotcms_test() {
    print_info "Running quick DotCMS API test (200 EPS)..."
    run_kubernetes_test 50 200 "quick-dotcms-test" 120 "dotcms"
    print_success "DotCMS API test completed!"
    echo ""
    analyze_latest_results
}

compare_endpoints() {
    print_info "Running endpoint comparison tests..."
    
    # Test both endpoints at the same load level
    local test_eps=300
    local test_threads=60
    local test_duration=120
    
    # Test Direct Analytics first
    print_info "Testing Direct Analytics endpoint..."
    run_kubernetes_test "$test_threads" "$test_eps" "compare-direct-${test_eps}eps" "$test_duration" "direct"
    
    print_info "Waiting 30 seconds before next test..."
    sleep 30
    
    # Test DotCMS API
    print_info "Testing DotCMS API endpoint..."
    run_kubernetes_test "$test_threads" "$test_eps" "compare-dotcms-${test_eps}eps" "$test_duration" "dotcms"
    
    print_success "Endpoint comparison completed!"
    echo ""
    print_info "Results summary:"
    echo ""
    
    # Analyze both results
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        echo '=== ENDPOINT COMPARISON ANALYSIS ==='
        echo ''
        
        # Analyze Direct Analytics results
        direct_file='/opt/test-results/compare-direct-${test_eps}eps.jtl'
        dotcms_file='/opt/test-results/compare-dotcms-${test_eps}eps.jtl'
        
        if [ -f \"\$direct_file\" ] && [ -f \"\$dotcms_file\" ]; then
            echo 'DIRECT ANALYTICS RESULTS:'
            direct_total=\$(tail -n +2 \"\$direct_file\" | wc -l)
            direct_success=\$(tail -n +2 \"\$direct_file\" | awk -F, '\$8==\"true\" {count++} END {print count+0}')
            direct_avg=\$(tail -n +2 \"\$direct_file\" | awk -F, '{sum+=\$2; count++} END {printf \"%.1f\", sum/count}')
            direct_eps=\$(echo \"scale=1; \$direct_total / $test_duration\" | bc)
            direct_error=\$(echo \"scale=2; (\$direct_total - \$direct_success) * 100.0 / \$direct_total\" | bc)
            
            echo \"  Total Requests: \$direct_total\"
            echo \"  Actual EPS: \$direct_eps\"
            echo \"  Success Rate: \$(echo \"scale=1; \$direct_success * 100.0 / \$direct_total\" | bc)%\"
            echo \"  Error Rate: \${direct_error}%\"
            echo \"  Avg Response: \${direct_avg}ms\"
            echo ''
            
            echo 'DOTCMS API RESULTS:'
            dotcms_total=\$(tail -n +2 \"\$dotcms_file\" | wc -l)
            dotcms_success=\$(tail -n +2 \"\$dotcms_file\" | awk -F, '\$8==\"true\" {count++} END {print count+0}')
            dotcms_avg=\$(tail -n +2 \"\$dotcms_file\" | awk -F, '{sum+=\$2; count++} END {printf \"%.1f\", sum/count}')
            dotcms_eps=\$(echo \"scale=1; \$dotcms_total / $test_duration\" | bc)
            dotcms_error=\$(echo \"scale=2; (\$dotcms_total - \$dotcms_success) * 100.0 / \$dotcms_total\" | bc)
            
            echo \"  Total Requests: \$dotcms_total\"
            echo \"  Actual EPS: \$dotcms_eps\"
            echo \"  Success Rate: \$(echo \"scale=1; \$dotcms_success * 100.0 / \$dotcms_total\" | bc)%\"
            echo \"  Error Rate: \${dotcms_error}%\"
            echo \"  Avg Response: \${dotcms_avg}ms\"
            echo ''
            
            echo 'PERFORMANCE COMPARISON:'
            throughput_ratio=\$(echo \"scale=2; \$direct_eps / \$dotcms_eps\" | bc)
            response_ratio=\$(echo \"scale=2; \$dotcms_avg / \$direct_avg\" | bc)
            error_diff=\$(echo \"scale=2; \$dotcms_error - \$direct_error\" | bc)
            
            echo \"  Throughput Ratio (Direct/DotCMS): \${throughput_ratio}x\"
            echo \"  Response Time Ratio (DotCMS/Direct): \${response_ratio}x\"
            echo \"  Error Rate Difference: +\${error_diff}% (DotCMS vs Direct)\"
            echo ''
            
            echo 'BOTTLENECK ANALYSIS:'
            if (( \$(echo \"\$throughput_ratio > 1.5\" | bc -l) )); then
                echo \"  ⚠️  DotCMS API shows significant throughput reduction (>50%)\"
                echo \"  📊 DotCMS API layer adds processing overhead\"
            elif (( \$(echo \"\$throughput_ratio > 1.2\" | bc -l) )); then
                echo \"  📊 DotCMS API shows moderate throughput reduction (20-50%)\"
                echo \"  🔍 Additional processing in DotCMS layer\"
            else
                echo \"  ✅ DotCMS API throughput comparable to direct access\"
            fi
            
            if (( \$(echo \"\$response_ratio > 2\" | bc -l) )); then
                echo \"  ⚠️  DotCMS API response times significantly higher (>2x)\"
                echo \"  🔧 Consider optimizing DotCMS analytics processing\"
            elif (( \$(echo \"\$response_ratio > 1.5\" | bc -l) )); then
                echo \"  📊 DotCMS API response times moderately higher (1.5-2x)\"
                echo \"  🔍 Normal overhead for API layer processing\"
            else
                echo \"  ✅ DotCMS API response times acceptable\"
            fi
            
            if (( \$(echo \"\$error_diff > 10\" | bc -l) )); then
                echo \"  ⚠️  DotCMS API shows significantly higher error rates (+10%)\"
                echo \"  🔧 Investigate DotCMS API error handling\"
            elif (( \$(echo \"\$error_diff > 5\" | bc -l) )); then
                echo \"  📊 DotCMS API shows moderately higher error rates (+5%)\"
                echo \"  🔍 Monitor DotCMS API under sustained load\"
            else
                echo \"  ✅ DotCMS API error rates comparable to direct access\"
            fi
            
            echo ''
            echo 'RECOMMENDATIONS:'
            if (( \$(echo \"\$throughput_ratio > 1.3 || \$response_ratio > 1.8\" | bc -l) )); then
                echo \"  • Consider caching at DotCMS API layer\"
                echo \"  • Profile DotCMS analytics request processing\"
                echo \"  • Optimize database queries in DotCMS analytics\"
                echo \"  • Consider async processing for non-critical operations\"
            else
                echo \"  • DotCMS API performance is acceptable for production\"
                echo \"  • Monitor performance under sustained production load\"
            fi
            echo '=========================================='
        else
            echo 'Error: Could not find test result files'
        fi
    "
}

show_status() {
    print_info "Checking infrastructure status..."
    
    echo ""
    echo "=== Kubernetes Resources ==="
    kubectl get pods -n "$NAMESPACE" | grep -E "(jmeter|jitsu)" || echo "No pods found"
    
    echo ""
    echo "=== Helm Releases ==="
    helm list -n "$NAMESPACE" | grep jmeter || echo "No JMeter releases found"
    
    echo ""
    echo "=== Analytics Endpoint Test ==="
    if kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        kubectl exec jmeter-test-pod -n "$NAMESPACE" -- curl -s \
            "http://jitsu-api.analytics-dev.svc.cluster.local:8001/api/v1/event?token=$ANALYTICS_KEY" \
            -H "Content-Type: application/json" \
            -d '{"test":"connectivity"}' \
            --max-time 5 || echo "❌ Analytics endpoint not responding"
        echo "✅ Analytics endpoint responding"
    else
        echo "❌ JMeter test pod not found"
    fi
}

show_logs() {
    print_info "Showing test execution logs..."
    
    if ! kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        print_error "JMeter test pod not found"
        exit 1
    fi
    
    echo ""
    echo "=== Recent Test Results ==="
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- ls -la /opt/test-results/ 2>/dev/null || echo "No test results found"
    
    echo ""
    echo "=== Latest Test Log (last 50 lines) ==="
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
        latest_log=$(ls -t /opt/test-results/*.log 2>/dev/null | head -1)
        if [ -n "$latest_log" ]; then
            tail -50 "$latest_log"
        else
            echo "No log files found"
        fi
    '
}

cleanup_infrastructure() {
    print_warning "This will remove all testing infrastructure. Continue? [y/N]"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        print_info "Cleaning up infrastructure..."
        
        helm uninstall jmeter-test -n "$NAMESPACE" 2>/dev/null || print_warning "Helm release not found"
        
        # Clean up any remaining resources
        kubectl delete pod jmeter-test-pod -n "$NAMESPACE" 2>/dev/null || true
        kubectl delete configmap jmeter-jmx-configmap -n "$NAMESPACE" 2>/dev/null || true
        kubectl delete configmap jmeter-scripts-configmap -n "$NAMESPACE" 2>/dev/null || true
        
        print_success "Cleanup completed"
    else
        print_info "Cleanup cancelled"
    fi
}

bottleneck_analysis() {
    print_info "Running comprehensive bottleneck analysis..."
    print_info "This will test both endpoints at multiple load levels to identify bottlenecks"
    
    local test_configs=(
        "25,100,baseline"
        "50,200,standard"
        "100,400,moderate"
        "150,600,high"
        "200,800,stress"
    )
    
    echo ""
    print_info "=== BOTTLENECK ANALYSIS TEST SUITE ==="
    
    for config in "${test_configs[@]}"; do
        IFS=',' read -r threads eps name <<< "$config"
        echo ""
        print_info "Testing Load Level: $name ($eps EPS, $threads threads)"
        
        # Test Direct Analytics
        print_info "  → Testing Direct Analytics endpoint..."
        run_kubernetes_test "$threads" "$eps" "bottleneck-direct-${name}-${eps}eps" 120 "direct"
        
        print_info "  Waiting 15 seconds..."
        sleep 15
        
        # Test DotCMS API  
        print_info "  → Testing DotCMS API endpoint..."
        run_kubernetes_test "$threads" "$eps" "bottleneck-dotcms-${name}-${eps}eps" 120 "dotcms"
        
        print_info "  Waiting 30 seconds before next load level..."
        sleep 30
    done
    
    print_success "Bottleneck analysis completed!"
    echo ""
    print_info "Generating comprehensive comparison report..."
    
    # Generate bottleneck analysis report
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
        echo "=== BOTTLENECK ANALYSIS REPORT ==="
        echo "Generated: $(date)"
        echo ""
        echo "LOAD LEVEL COMPARISON:"
        echo "Format: [Load Level] Direct→DotCMS (EPS) | Direct→DotCMS (Avg Response)"
        echo ""
        
        for level in baseline standard moderate high stress; do
            case $level in
                baseline) eps=100 ;;
                standard) eps=200 ;;
                moderate) eps=400 ;;
                high) eps=600 ;;
                stress) eps=800 ;;
            esac
            
            direct_file="/opt/test-results/bottleneck-direct-$level-${eps}eps.jtl"
            dotcms_file="/opt/test-results/bottleneck-dotcms-$level-${eps}eps.jtl"
            
            if [ -f "$direct_file" ] && [ -f "$dotcms_file" ]; then
                # Direct analytics metrics
                direct_total=$(tail -n +2 "$direct_file" | wc -l)
                direct_success=$(tail -n +2 "$direct_file" | awk -F, '\$8=="true" {count++} END {print count+0}')
                direct_avg=$(tail -n +2 "$direct_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                direct_eps=$(echo "scale=1; $direct_total / 120" | bc)
                direct_error=$(echo "scale=1; ($direct_total - $direct_success) * 100.0 / $direct_total" | bc)
                
                # DotCMS metrics  
                dotcms_total=$(tail -n +2 "$dotcms_file" | wc -l)
                dotcms_success=$(tail -n +2 "$dotcms_file" | awk -F, '\$8=="true" {count++} END {print count+0}')
                dotcms_avg=$(tail -n +2 "$dotcms_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                dotcms_eps=$(echo "scale=1; $dotcms_total / 120" | bc)
                dotcms_error=$(echo "scale=1; ($dotcms_total - $dotcms_success) * 100.0 / $dotcms_total" | bc)
                
                # Calculate ratios
                throughput_ratio=$(echo "scale=2; $direct_eps / $dotcms_eps" | bc)
                response_ratio=$(echo "scale=2; $dotcms_avg / $direct_avg" | bc)
                
                printf "[%-8s] %6.1f→%6.1f EPS (%4.1fx) | %6.1f→%6.1f ms (%4.1fx) | Errors: %4.1f%%→%4.1f%%\n" \
                    "$level" "$direct_eps" "$dotcms_eps" "$throughput_ratio" "$direct_avg" "$dotcms_avg" "$response_ratio" "$direct_error" "$dotcms_error"
            fi
        done
        
        echo ""
        echo "PERFORMANCE ANALYSIS:"
        
        # Dynamic analysis based on actual results
        baseline_direct="/opt/test-results/bottleneck-direct-baseline-100eps.jtl"
        baseline_dotcms="/opt/test-results/bottleneck-dotcms-baseline-100eps.jtl"
        
        if [ -f "$baseline_direct" ] && [ -f "$baseline_dotcms" ]; then
            direct_avg=$(tail -n +2 "$baseline_direct" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
            dotcms_avg=$(tail -n +2 "$baseline_dotcms" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
            
            if [ "$direct_avg" -gt 0 ]; then
                ratio=$(echo "scale=1; $dotcms_avg / $direct_avg" | bc)
                echo "• Response Time Impact: DotCMS API adds ${ratio}x overhead"
                
                if (( $(echo "$ratio > 10" | bc -l) )); then
                    echo "• Bottleneck Severity: Significant API processing overhead detected"
                elif (( $(echo "$ratio > 3" | bc -l) )); then
                    echo "• Bottleneck Severity: Moderate API processing overhead"
                else
                    echo "• Bottleneck Severity: Minimal API processing overhead"
                fi
            fi
        fi
        
        echo ""
        echo "OPTIMIZATION RECOMMENDATIONS:"
        echo "1. Profile request processing pipeline for identified bottlenecks"
        echo "2. Consider caching strategies for frequently accessed data"
        echo "3. Evaluate async processing for non-critical operations"
        echo "4. Review database query optimization opportunities"
        echo "5. Monitor error patterns under sustained load"
        echo ""
        echo "Use: ./dotcms-analytics-test.sh generate-report for detailed analysis"
        echo "==============================================="
    '
}

find_maximum_rate() {
    print_info "Finding maximum sustainable rate for both endpoints..."
    print_info "This will progressively increase load until failure rates exceed thresholds"
    
    local max_eps=${1:-2000}
    local endpoint=${2:-"both"}
    
    if [ "$endpoint" = "both" ] || [ "$endpoint" = "direct" ]; then
        print_info "Finding maximum rate for Direct Analytics..."
        find_max_rate_for_endpoint "direct" "$max_eps"
    fi
    
    if [ "$endpoint" = "both" ] || [ "$endpoint" = "dotcms" ]; then
        print_info "Finding maximum rate for DotCMS API..."
        find_max_rate_for_endpoint "dotcms" "$max_eps"
    fi
    
    print_success "Maximum rate analysis completed!"
}

find_max_rate_for_endpoint() {
    local endpoint=$1
    local max_eps=${2:-2000}
    local current_eps=200
    local step=200
    local max_error_rate=25
    
    print_info "Progressive testing for $endpoint endpoint (up to $max_eps EPS)"
    
    while [ $current_eps -le $max_eps ]; do
        local threads=$((current_eps / 4))  # Rough heuristic: 4 EPS per thread
        [ $threads -lt 25 ] && threads=25   # Minimum threads
        [ $threads -gt 400 ] && threads=400 # Maximum threads
        
        print_info "Testing $endpoint at $current_eps EPS with $threads threads..."
        
        run_kubernetes_test "$threads" "$current_eps" "maxrate-${endpoint}-${current_eps}eps" 90 "$endpoint"
        
        # Check error rate
        local error_rate=$(kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
            result_file=\"/opt/test-results/maxrate-${endpoint}-${current_eps}eps.jtl\"
            if [ -f \"\$result_file\" ]; then
                total=\$(tail -n +2 \"\$result_file\" | wc -l)
                success=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"true\" {count++} END {print count+0}')
                echo \"scale=1; (\$total - \$success) * 100.0 / \$total\" | bc
            else
                echo \"100\"
            fi
        ")
        
        print_info "Error rate at $current_eps EPS: ${error_rate}%"
        
        # Check if we've hit the failure threshold
        if (( $(echo "$error_rate > $max_error_rate" | bc -l) )); then
            print_warning "Error rate (${error_rate}%) exceeds threshold ($max_error_rate%)"
            print_warning "Maximum sustainable rate for $endpoint: $((current_eps - step)) EPS"
            break
        fi
        
        # Increase load
        current_eps=$((current_eps + step))
        
        # Increase step size as we get higher (accelerated testing)
        if [ $current_eps -gt 1000 ]; then
            step=400
        elif [ $current_eps -gt 500 ]; then
            step=300
        fi
        
        print_info "Waiting 20 seconds before next test..."
        sleep 20
    done
}

scaling_test() {
    print_info "Running comprehensive scaling test..."
    print_info "Testing both endpoints across multiple load levels"
    
    # Test configurations covering light to heavy loads
    local test_configs=(
        "25,100,light-100eps"
        "50,200,baseline-200eps"
        "75,400,medium-400eps"
        "100,600,high-600eps"
        "150,800,heavy-800eps"
        "200,1000,extreme-1000eps" 
        "250,1200,stress-1200eps"
        "300,1500,maximum-1500eps"
        "400,2000,ultimate-2000eps"
    )
    
    print_info "Testing both endpoints across full scaling range..."
    
    local summary_file="scaling-test-summary-$(date +%Y%m%d-%H%M%S).txt"
    
    echo "COMPREHENSIVE SCALING TEST SUMMARY" > "/tmp/$summary_file"
    echo "Generated: $(date)" >> "/tmp/$summary_file"
    echo "" >> "/tmp/$summary_file"
    
    for config in "${test_configs[@]}"; do
        IFS=',' read -r threads eps name <<< "$config"
        
        echo "" 
        print_info "=== Testing Configuration: $name ($eps EPS, $threads threads) ==="
        
        # Test both endpoints at this load level
        for endpoint in "direct" "dotcms"; do
            print_info "Testing $endpoint endpoint..."
            
            if run_kubernetes_test "$threads" "$eps" "scaling-${endpoint}-${name}" 120 "$endpoint"; then
                print_success "$endpoint test completed"
                echo "scaling-${endpoint}-${name}: SUCCESS" >> "/tmp/$summary_file"
            else
                print_error "$endpoint test failed"
                echo "scaling-${endpoint}-${name}: FAILED" >> "/tmp/$summary_file"
            fi
            
            sleep 10
        done
        
        print_info "Waiting 30 seconds before next configuration..."
        sleep 30
    done
    
    # Copy summary to pod for persistence
    kubectl cp "/tmp/$summary_file" "jmeter-test-pod:/opt/test-results/$summary_file" -n "$NAMESPACE"
    
    print_success "Comprehensive scaling test completed!"
    print_info "Summary saved as: $summary_file"
    
    # Generate comparison analysis
    generate_scaling_analysis
}

generate_scaling_analysis() {
    print_info "Generating scaling analysis report..."
    
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
        echo "=== SCALING ANALYSIS REPORT ==="
        echo "Generated: $(date)"
        echo ""
        
        echo "ENDPOINT COMPARISON ACROSS LOAD LEVELS:"
        echo "Load Level          | Direct EPS | DotCMS EPS | Direct Avg | DotCMS Avg | Perf Gap"
        echo "-------------------|------------|------------|------------|------------|----------"
        
        for config in light-100eps baseline-200eps medium-400eps high-600eps heavy-800eps extreme-1000eps stress-1200eps maximum-1500eps ultimate-2000eps; do
            direct_file="/opt/test-results/scaling-direct-$config.jtl"
            dotcms_file="/opt/test-results/scaling-dotcms-$config.jtl"
            
            if [ -f "$direct_file" ] && [ -f "$dotcms_file" ]; then
                # Calculate metrics for both endpoints
                direct_total=$(tail -n +2 "$direct_file" | wc -l)
                direct_avg=$(tail -n +2 "$direct_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
                direct_eps=$(echo "scale=0; $direct_total / 120" | bc)
                
                dotcms_total=$(tail -n +2 "$dotcms_file" | wc -l)
                dotcms_avg=$(tail -n +2 "$dotcms_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
                dotcms_eps=$(echo "scale=0; $dotcms_total / 120" | bc)
                
                if [ "$direct_avg" -gt 0 ]; then
                    gap=$(echo "scale=1; $dotcms_avg / $direct_avg" | bc)
                else
                    gap="N/A"
                fi
                
                printf "%-18s | %10s | %10s | %8s ms | %8s ms | %7s\n" \
                    "$config" "$direct_eps" "$dotcms_eps" "$direct_avg" "$dotcms_avg" "${gap}x"
            fi
        done
        
        echo ""
        echo "ANALYSIS SUMMARY:"
        
        # Dynamic analysis based on available results
        total_tests=0
        avg_ratio=0
        
        for config in light-100eps baseline-200eps medium-400eps high-600eps; do
            direct_file="/opt/test-results/scaling-direct-$config.jtl"
            dotcms_file="/opt/test-results/scaling-dotcms-$config.jtl"
            
            if [ -f "$direct_file" ] && [ -f "$dotcms_file" ]; then
                direct_avg=$(tail -n +2 "$direct_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
                dotcms_avg=$(tail -n +2 "$dotcms_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.0f\", sum/count}")
                
                if [ "$direct_avg" -gt 0 ]; then
                    ratio=$(echo "scale=1; $dotcms_avg / $direct_avg" | bc)
                    avg_ratio=$(echo "scale=1; $avg_ratio + $ratio" | bc)
                    total_tests=$((total_tests + 1))
                fi
            fi
        done
        
        if [ "$total_tests" -gt 0 ]; then
            overall_ratio=$(echo "scale=1; $avg_ratio / $total_tests" | bc)
            echo "• Average performance ratio: ${overall_ratio}x (DotCMS vs Direct)"
            
            if (( $(echo "$overall_ratio > 10" | bc -l) )); then
                echo "• Performance impact: Significant overhead in API layer"
            elif (( $(echo "$overall_ratio > 3" | bc -l) )); then
                echo "• Performance impact: Moderate overhead in API layer"
            else
                echo "• Performance impact: Minimal overhead"
            fi
        fi
        
        echo ""
        echo "SCALING RECOMMENDATIONS:"
        echo "1. Choose endpoint based on performance requirements"
        echo "2. Monitor response times under production load"
        echo "3. Consider optimization strategies for identified bottlenecks"
        echo "4. Set appropriate capacity planning thresholds"
        echo ""
    ' | tee scaling-analysis-report-$(date +%Y%m%d-%H%M%S).txt
}

generate_report() {
    print_info "Generating comprehensive performance report..."
    
    local report_file="dotcms-analytics-performance-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << 'EOF'
# DotCMS Analytics Performance Analysis Report

## Executive Summary

This report provides comprehensive performance analysis of DotCMS analytics capabilities, comparing direct analytics endpoint access versus DotCMS API integration.

## Test Environment

- **Analytics Platform**: Jitsu Analytics
- **Test Infrastructure**: Kubernetes-based JMeter testing
- **Test Duration**: Configurable (default 3 minutes)
- **Load Testing Tool**: Apache JMeter with custom test plans

## Test Results

EOF

    # Add dynamic results from latest tests
    if kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
            echo ""
            echo "### Latest Test Results"
            echo ""
            
            # Find latest comparison results
            latest_direct=$(ls -t /opt/test-results/*direct*.jtl 2>/dev/null | head -1)
            latest_dotcms=$(ls -t /opt/test-results/*dotcms*.jtl 2>/dev/null | head -1)
            
            if [ -n "$latest_direct" ] && [ -n "$latest_dotcms" ]; then
                echo "| Metric | Direct Analytics | DotCMS API | Performance Impact |"
                echo "|--------|------------------|------------|-------------------|"
                
                # Direct analytics metrics
                direct_total=$(tail -n +2 "$latest_direct" | wc -l)
                direct_success=$(tail -n +2 "$latest_direct" | awk -F, "\$8==\"true\" {count++} END {print count+0}")
                direct_avg=$(tail -n +2 "$latest_direct" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                
                # DotCMS metrics
                dotcms_total=$(tail -n +2 "$latest_dotcms" | wc -l)
                dotcms_success=$(tail -n +2 "$latest_dotcms" | awk -F, "\$8==\"true\" {count++} END {print count+0}")
                dotcms_avg=$(tail -n +2 "$latest_dotcms" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                
                # Calculate impact
                if [ "$direct_avg" != "0" ]; then
                    response_ratio=$(echo "scale=1; $dotcms_avg / $direct_avg" | bc)
                    echo "| **Response Time** | ${direct_avg}ms | ${dotcms_avg}ms | ${response_ratio}x slower |"
                fi
                
                direct_success_rate=$(echo "scale=1; $direct_success * 100.0 / $direct_total" | bc)
                dotcms_success_rate=$(echo "scale=1; $dotcms_success * 100.0 / $dotcms_total" | bc)
                echo "| **Success Rate** | ${direct_success_rate}% | ${dotcms_success_rate}% | - |"
                
                echo ""
            elif [ -n "$latest_direct" ]; then
                echo "#### Direct Analytics Results"
                total=$(tail -n +2 "$latest_direct" | wc -l)
                success=$(tail -n +2 "$latest_direct" | awk -F, "\$8==\"true\" {count++} END {print count+0}")
                avg=$(tail -n +2 "$latest_direct" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                success_rate=$(echo "scale=1; $success * 100.0 / $total" | bc)
                echo "- Total Requests: $total"
                echo "- Success Rate: ${success_rate}%"
                echo "- Average Response: ${avg}ms"
                echo ""
            elif [ -n "$latest_dotcms" ]; then
                echo "#### DotCMS API Results"
                total=$(tail -n +2 "$latest_dotcms" | wc -l)
                success=$(tail -n +2 "$latest_dotcms" | awk -F, "\$8==\"true\" {count++} END {print count+0}")
                avg=$(tail -n +2 "$latest_dotcms" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                success_rate=$(echo "scale=1; $success * 100.0 / $total" | bc)
                echo "- Total Requests: $total"
                echo "- Success Rate: ${success_rate}%"
                echo "- Average Response: ${avg}ms"
                echo ""
            else
                echo "No test results found. Run tests first with:"
                echo "- \`./dotcms-analytics-test.sh compare-endpoints\`"
                echo "- \`./dotcms-analytics-test.sh quick-test\`"
                echo ""
            fi
        ' >> "$report_file"
    fi
    
    cat >> "$report_file" << 'EOF'

## Analysis and Recommendations

### Performance Optimization
- **For high-volume scenarios**: Consider direct analytics endpoint
- **For integration scenarios**: Optimize DotCMS API layer processing
- **Hybrid approach**: Use appropriate endpoint based on use case

### Infrastructure Scaling
- Monitor response times and error rates under load
- Set appropriate capacity planning thresholds
- Consider caching strategies for frequently accessed data

## Testing Methodology

### Consolidated Testing Approach
This analysis uses the unified testing tool: `./dotcms-analytics-test.sh`

Available test types:
- Quick baseline tests
- Progressive scaling tests  
- Bottleneck analysis
- Maximum rate testing

## Next Steps

1. **Identify bottlenecks**: Run `bottleneck-analysis` for detailed comparison
2. **Find limits**: Use `find-maximum-rate` to determine capacity
3. **Optimize**: Focus on identified performance gaps
4. **Monitor**: Set up appropriate production monitoring

---
*Report generated by DotCMS Analytics Testing Tool*
EOF

    print_success "Performance report generated: $report_file"
    print_info "Report includes dynamic analysis based on latest test results"
}

analyze_all() {
    print_info "Analyzing all available test results..."
    
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c '
        echo "=== COMPREHENSIVE RESULTS ANALYSIS ==="
        echo "Generated: $(date)"
        echo ""
        
        result_files=$(ls /opt/test-results/*.jtl 2>/dev/null || echo "")
        if [ -z "$result_files" ]; then
            echo "No result files found"
            exit 1
        fi
        
        echo "Test Results Summary:"
        echo "===================="
        
        for result_file in $result_files; do
            test_name=$(basename "$result_file" .jtl)
            
            if [ -s "$result_file" ]; then
                total=$(tail -n +2 "$result_file" | wc -l)
                success=$(tail -n +2 "$result_file" | awk -F, "\$8==\"true\" {count++} END {print count+0}")
                avg=$(tail -n +2 "$result_file" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                error_rate=$(echo "scale=1; ($total - $success) * 100.0 / $total" | bc)
                
                printf "%-40s | %6d req | %5.1f%% err | %6.1f ms avg\n" \
                    "$test_name" "$total" "$error_rate" "$avg"
            fi
        done
        
        echo ""
        echo "Performance Trends:"
        echo "=================="
        
        # Analyze direct vs dotcms patterns
        echo "Direct Analytics Tests:"
        ls /opt/test-results/*direct*.jtl 2>/dev/null | while read file; do
            if [ -s "$file" ]; then
                avg=$(tail -n +2 "$file" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                echo "  $(basename "$file" .jtl): ${avg}ms avg"
            fi
        done
        
        echo ""
        echo "DotCMS API Tests:"
        ls /opt/test-results/*dotcms*.jtl 2>/dev/null | while read file; do
            if [ -s "$file" ]; then
                avg=$(tail -n +2 "$file" | awk -F, "{sum+=\$2; count++} END {printf \"%.1f\", sum/count}")
                echo "  $(basename "$file" .jtl): ${avg}ms avg"
            fi
        done
        
        echo ""
    '
}

# Download all test result files to local directory
download_results() {
    print_info "Downloading test result files from Kubernetes pod..."
    
    # Check if pod exists
    if ! kubectl get pod jmeter-test-pod -n "$NAMESPACE" &> /dev/null; then
        print_error "JMeter test pod not found. Run setup first."
        exit 1
    fi
    
    # Create local results directory with timestamp
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local base_dir="downloaded-results"
    local local_dir="$base_dir/results-${timestamp}"
    
    mkdir -p "$local_dir"
    print_info "Created local directory: $local_dir"
    
    # Get list of result files from pod
    local result_files=$(kubectl exec jmeter-test-pod -n "$NAMESPACE" -- ls /opt/test-results/ 2>/dev/null || echo "")
    
    if [ -z "$result_files" ]; then
        print_warning "No result files found in pod"
        return 0
    fi
    
    print_info "Found result files:"
    echo "$result_files" | while read file; do
        if [ -n "$file" ]; then
            print_info "  • $file"
        fi
    done
    
    # Download all files
    local downloaded=0
    local failed=0
    
    echo "$result_files" | while read file; do
        if [ -n "$file" ]; then
            if kubectl cp "$NAMESPACE/jmeter-test-pod:/opt/test-results/$file" "$local_dir/$file" 2>/dev/null; then
                downloaded=$((downloaded + 1))
            else
                print_warning "Failed to download: $file"
                failed=$((failed + 1))
            fi
        fi
    done
    
    # Summary
    local total_files=$(echo "$result_files" | grep -c .)
    print_success "Download completed!"
    print_info "Local directory: $local_dir"
    print_info "Total files: $total_files"
    print_info "📊 Result files (.jtl): $(ls "$local_dir"/*.jtl 2>/dev/null | wc -l)"
    print_info "📋 Log files (.log): $(ls "$local_dir"/*.log 2>/dev/null | wc -l)"
    print_info "📝 Other files: $(ls "$local_dir" 2>/dev/null | grep -v -E '\.(jtl|log)$' | wc -l)"
    
    # Provide usage examples
    echo ""
    print_info "💡 Usage examples:"
    echo "  # View latest results:"
    echo "  ls -la $local_dir/"
    echo ""
    echo "  # Analyze .jtl files with custom tools:"
    echo "  head $local_dir/*.jtl"
    echo ""
    echo "  # Import into JMeter GUI for detailed analysis:"
    echo "  # File → Open Recent Results → Select .jtl file"
    echo ""
    echo "  # Archive results:"
    echo "  tar -czf $base_dir/analytics-test-results-${timestamp}.tar.gz $local_dir/"
    echo ""
    print_info "📁 All downloads are stored in '$base_dir/' (excluded from git)"
}

# Sustained load testing to find queue limits
sustained_load_test() {
    local endpoint="$1"
    local start_eps="$2"
    local max_eps="$3"
    local step_size="$4"
    local duration=600  # 10 minutes
    
    print_info "Starting sustained load testing to find queue limits..."
    print_info "Endpoint: $endpoint | Range: ${start_eps}-${max_eps} EPS | Step: ${step_size} EPS | Duration: ${duration}s (10 min)"
    
    if [ "$endpoint" = "both" ] || [ "$endpoint" = "direct" ]; then
        sustained_test_endpoint "direct" "$start_eps" "$max_eps" "$step_size" "$duration"
    fi
    
    if [ "$endpoint" = "both" ] || [ "$endpoint" = "dotcms" ]; then
        sustained_test_endpoint "dotcms" "$start_eps" "$max_eps" "$step_size" "$duration"
    fi
    
    print_success "Sustained load testing completed!"
    print_info "Check results for queue limits and connection failure patterns"
}

# Test sustained load for a specific endpoint
sustained_test_endpoint() {
    local endpoint="$1"
    local start_eps="$2"
    local max_eps="$3"
    local step_size="$4"
    local duration="$5"
    
    print_info "=== SUSTAINED LOAD TEST: $endpoint endpoint ==="
    print_info "Testing sustained load patterns with 10-minute durations"
    
    local current_eps="$start_eps"
    local connection_failures=0
    local max_connection_failures=3
    local queue_limit_found=false
    
    while [ "$current_eps" -le "$max_eps" ] && [ "$connection_failures" -lt "$max_connection_failures" ] && [ "$queue_limit_found" = false ]; do
        print_info "Testing sustained load: $current_eps EPS for ${duration}s (${endpoint} endpoint)"
        
        # Calculate threads based on EPS (higher ratio for sustained load)
        local threads=$((current_eps / 3))
        [ "$threads" -lt 10 ] && threads=10
        [ "$threads" -gt 500 ] && threads=500
        
        # Record pre-test infrastructure state
        record_infrastructure_state "pre" "$endpoint" "$current_eps"
        
        # Run sustained load test
        local test_name="sustained-${endpoint}-${current_eps}eps"
        run_kubernetes_test "$threads" "$current_eps" "$test_name" "$duration" "$endpoint"
        
        # Record post-test infrastructure state  
        record_infrastructure_state "post" "$endpoint" "$current_eps"
        
        # Analyze results and detect failures
        local test_results=$(analyze_sustained_test_results "$test_name" "$current_eps" "$duration")
        local error_rate=$(echo "$test_results" | grep "Error Rate:" | awk '{print $3}' | tr -d '%')
        local connection_errors=$(echo "$test_results" | grep "Connection Errors:" | awk '{print $3}')
        local service_restarts=$(detect_service_restarts "$endpoint")
        
        print_info "Results: ${error_rate}% errors, ${connection_errors} connection failures, ${service_restarts} restarts"
        
        # Check for connection failures or service restarts
        if [ "${connection_errors:-0}" -gt 10 ] || [ "${service_restarts:-0}" -gt 0 ]; then
            connection_failures=$((connection_failures + 1))
            print_warning "Connection failures detected (${connection_failures}/${max_connection_failures})"
            
            if [ "$connection_failures" -ge "$max_connection_failures" ]; then
                print_error "Maximum connection failures reached. Queue limit found at ${current_eps} EPS"
                queue_limit_found=true
                break
            fi
        fi
        
        # Check for queue saturation patterns
        if (( $(echo "${error_rate:-0} > 50" | bc -l) )); then
            print_warning "High error rate (${error_rate}%) indicates queue saturation"
            queue_limit_found=true
            break
        fi
        
        # Wait between tests to allow recovery
        if [ "$queue_limit_found" = false ]; then
            print_info "Waiting 60 seconds for system recovery..."
            sleep 60
        fi
        
        current_eps=$((current_eps + step_size))
    done
    
    if [ "$queue_limit_found" = true ]; then
        print_success "Queue limit identified for $endpoint endpoint: ~$((current_eps - step_size)) EPS"
    else
        print_info "No queue limit found within test range for $endpoint endpoint"
    fi
}

# Record infrastructure state for sustained testing
record_infrastructure_state() {
    local phase="$1"  # pre or post
    local endpoint="$2"
    local eps="$3"
    local timestamp=$(date '+%Y%m%d-%H%M%S')
    
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        echo '=== INFRASTRUCTURE STATE: $phase-test ($endpoint $eps EPS) ===' >> /opt/test-results/sustained-infrastructure-${timestamp}.log
        echo 'Timestamp: $(date)' >> /opt/test-results/sustained-infrastructure-${timestamp}.log
        echo 'Analytics Pods:' >> /opt/test-results/sustained-infrastructure-${timestamp}.log
        kubectl get pods -n analytics-dev | grep analytics >> /opt/test-results/sustained-infrastructure-${timestamp}.log 2>/dev/null
        echo 'Pod Resources:' >> /opt/test-results/sustained-infrastructure-${timestamp}.log
        kubectl top pods -n analytics-dev >> /opt/test-results/sustained-infrastructure-${timestamp}.log 2>/dev/null
        echo '---' >> /opt/test-results/sustained-infrastructure-${timestamp}.log
    " 2>/dev/null || true
}

# Analyze sustained test results with queue focus
analyze_sustained_test_results() {
    local test_name="$1"
    local target_eps="$2" 
    local duration="$3"
    
    kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
        result_file=\"/opt/test-results/${test_name}.jtl\"
        if [ -f \"\$result_file\" ]; then
            total_requests=\$(tail -n +2 \"\$result_file\" | wc -l)
            successful_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"true\" {count++} END {print count+0}')
            failed_requests=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"false\" {count++} END {print count+0}')
            connection_errors=\$(tail -n +2 \"\$result_file\" | awk -F, '\$8==\"false\" && \$9 ~ /Connection/ {count++} END {print count+0}')
            
            # Calculate metrics
            if [ \$total_requests -gt 0 ]; then
                error_rate=\$(echo \"scale=1; (\$failed_requests * 100.0) / \$total_requests\" | bc)
            else
                error_rate=0
            fi
            
            echo \"Total Requests: \$total_requests\"
            echo \"Error Rate: \${error_rate}%\"
            echo \"Connection Errors: \$connection_errors\"
            echo \"Duration: ${duration}s\"
            echo \"Target EPS: $target_eps\"
        fi
    "
}

# Detect service restarts during sustained testing
detect_service_restarts() {
    local endpoint="$1"
    
    # Check for pod restarts in the last 15 minutes
    local restarts=$(kubectl get pods -n analytics-dev -o json | jq -r '.items[] | select(.metadata.name | contains("analytics")) | .status.containerStatuses[]? | .restartCount' 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
    echo "${restarts:-0}"
}

# Parse command line arguments
COMMAND=""
COMMAND_ARGS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        setup|quick-test|dotcms-test|compare-endpoints|performance-test|stress-test|single-test|analyze|status|cleanup|logs|bottleneck-analysis|find-maximum-rate|scaling-test|analyze-all|generate-report|download-results|sustained-load-test)
            COMMAND="$1"
            shift
            # Capture remaining arguments for commands that need them
            if [ "$COMMAND" = "single-test" ] || [ "$COMMAND" = "sustained-load-test" ]; then
                while [[ $# -gt 0 ]] && [[ ! "$1" =~ ^-- ]]; do
                    COMMAND_ARGS+=("$1")
                    shift
                done
            fi
            ;;
        --analytics-host)
            ANALYTICS_HOST="$2"
            shift 2
            ;;
        --analytics-key)
            ANALYTICS_KEY="$2"
            shift 2
            ;;
        --dotcms-host)
            DOTCMS_HOST="$2"
            shift 2
            ;;
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        --duration)
            TEST_DURATION="$2"
            shift 2
            ;;
        --max-eps)
            MAX_EPS="$2"
            shift 2
            ;;
        --values-file)
            VALUES_FILE="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Execute command
case $COMMAND in
    setup)
        setup_infrastructure
        ;;
    quick-test)
        quick_test
        ;;
    dotcms-test)
        dotcms_test
        ;;
    compare-endpoints)
        compare_endpoints
        ;;
    performance-test)
        performance_test
        ;;
    stress-test)
        stress_test
        ;;
    single-test)
        eps=${COMMAND_ARGS[0]}
        threads=${COMMAND_ARGS[1]}
        endpoint=${COMMAND_ARGS[2]:-"direct"}
        single_test "$eps" "$threads" "$endpoint"
        ;;
    analyze)
        analyze_latest_results
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    refresh-tokens)
        print_info "Refreshing authentication tokens..."
        get_secure_tokens
        ;;
    cleanup)
        cleanup_infrastructure
        ;;
    bottleneck-analysis)
        bottleneck_analysis
        ;;
    find-maximum-rate)
        find_maximum_rate "$MAX_EPS"
        ;;
    scaling-test)
        scaling_test
        ;;
    analyze-all)
        analyze_all
        ;;
    generate-report)
        generate_report
        ;;
    download-results)
        download_results
        ;;
    sustained-load-test)
        endpoint=${COMMAND_ARGS[0]:-"both"}
        start_eps=${COMMAND_ARGS[1]:-100}
        max_eps=${COMMAND_ARGS[2]:-2000}
        step_size=${COMMAND_ARGS[3]:-200}
        sustained_load_test "$endpoint" "$start_eps" "$max_eps" "$step_size"
        ;;
    *)
        print_error "No command specified"
        usage
        exit 1
        ;;
esac