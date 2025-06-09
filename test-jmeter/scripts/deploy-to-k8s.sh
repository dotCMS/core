#!/bin/bash

# JMeter Performance Testing K8s Deployment Script
# Manages the separated ConfigMaps and Pod deployment

set -e

NAMESPACE="analytics-dev"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== JMeter K8s Deployment Manager ==="
echo "Base directory: $BASE_DIR"
echo "Namespace: $NAMESPACE"

# Function to check if namespace exists
check_namespace() {
    if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
        echo "Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
    else
        echo "Namespace $NAMESPACE already exists"
    fi
}

# Function to deploy ConfigMaps
deploy_configmaps() {
    echo ""
    echo "=== Deploying ConfigMaps ==="
    
    echo "Deploying JMeter scripts ConfigMap..."
    kubectl apply -f "$BASE_DIR/k8s/configmaps/jmeter-scripts-configmap.yaml"
    
    echo "Deploying JMX tests ConfigMap..."
    kubectl apply -f "$BASE_DIR/k8s/configmaps/jmeter-jmx-configmap.yaml"
    
    echo "ConfigMaps deployed successfully!"
}

# Function to deploy Pod
deploy_pod() {
    echo ""
    echo "=== Deploying JMeter Pod ==="
    
    # Delete existing pod if it exists
    if kubectl get pod jmeter-test-pod -n "$NAMESPACE" >/dev/null 2>&1; then
        echo "Deleting existing pod..."
        kubectl delete pod jmeter-test-pod -n "$NAMESPACE" --force --grace-period=0
        sleep 5
    fi
    
    echo "Deploying new pod..."
    kubectl apply -f "$BASE_DIR/k8s/jmeter-pod.yaml"
    
    echo "Waiting for pod to be ready..."
    kubectl wait --for=condition=Ready pod/jmeter-test-pod -n "$NAMESPACE" --timeout=180s
    
    echo "Pod deployed and ready!"
}

# Function to show status
show_status() {
    echo ""
    echo "=== Deployment Status ==="
    echo ""
    echo "ConfigMaps:"
    kubectl get configmaps -n "$NAMESPACE" | grep jmeter || echo "No JMeter ConfigMaps found"
    echo ""
    echo "Pod:"
    kubectl get pod jmeter-test-pod -n "$NAMESPACE" 2>/dev/null || echo "JMeter pod not found"
    echo ""
    echo "Pod details:"
    kubectl describe pod jmeter-test-pod -n "$NAMESPACE" 2>/dev/null | grep -A 5 "Conditions:" || echo "Pod not ready"
}

# Function to run a test
run_test() {
    local test_type="$1"
    local threads="${2:-10}"
    local eps="${3:-25}"
    local duration="${4:-30}"
    
    echo ""
    echo "=== Running $test_type Test ==="
    echo "Configuration: $threads threads, $eps eps, ${duration}s duration"
    
    case "$test_type" in
        "dotcms")
            kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
                export PATH=/opt/jmeter/bin:\$PATH && 
                jmeter -n -t /opt/jmx-tests/dotcms-api-cluster-test.jmx \
                -l /opt/test-results/test-\$(date +%Y%m%d-%H%M%S).jtl \
                -j /opt/test-results/test-\$(date +%Y%m%d-%H%M%S).log \
                -Jthread.number=$threads \
                -Jevents.per.second=$eps \
                -Jtest.duration=$duration \
                -Jrampup=10 \
                -Jdotcms.host=your-dotcms-instance.dotcms.cloud \
                -Jdotcms.port=443 \
                -Jdotcms.scheme=https \
                -Jmax.response.time=10000"
            ;;
        "direct")
            kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash -c "
                export PATH=/opt/jmeter/bin:\$PATH && 
                jmeter -n -t /opt/jmx-tests/direct-analytics-cluster-test.jmx \
                -l /opt/test-results/test-\$(date +%Y%m%d-%H%M%S).jtl \
                -j /opt/test-results/test-\$(date +%Y%m%d-%H%M%S).log \
                -Jthread.number=$threads \
                -Jevents.per.second=$eps \
                -Jtest.duration=$duration \
                -Jrampup=10 \
                -Janalytics.host=jitsu-api.analytics-dev.svc.cluster.local \
                -Janalytics.port=8001 \
                -Janalytics.scheme=http \
                -Jmax.response.time=10000"
            ;;
        "limits")
            kubectl exec jmeter-test-pod -n "$NAMESPACE" -- bash /opt/jmeter-scripts/performance-limits-test.sh
            ;;
        *)
            echo "Unknown test type: $test_type"
            echo "Available tests: dotcms, direct, limits"
            exit 1
            ;;
    esac
}

# Function to copy results
copy_results() {
    local output_file="${1:-performance_results_k8s.csv}"
    
    echo ""
    echo "=== Copying Results ==="
    
    # Find the latest results file
    local latest_results=$(kubectl exec jmeter-test-pod -n "$NAMESPACE" -- find /opt/test-results -name "performance_results.csv" -type f 2>/dev/null | head -1)
    
    if [[ -n "$latest_results" ]]; then
        echo "Copying $latest_results to $output_file"
        kubectl exec jmeter-test-pod -n "$NAMESPACE" -- cat "$latest_results" > "$output_file"
        echo "Results copied to: $output_file"
        
        # Show summary
        echo ""
        echo "Results Summary:"
        echo "==============="
        head -1 "$output_file"
        tail -n +2 "$output_file" | while IFS=',' read -r endpoint target_eps actual_eps avg_response min_response max_response error_count error_rate total_requests success_rate; do
            printf "%-8s %3d eps -> %5.1f eps actual, %4.0fms avg, %5.1f%% success\n" "$endpoint" "$target_eps" "$actual_eps" "$avg_response" "$success_rate"
        done
    else
        echo "No results file found in pod"
        echo "Available files:"
        kubectl exec jmeter-test-pod -n "$NAMESPACE" -- ls -la /opt/test-results/ 2>/dev/null || echo "No results directory found"
    fi
}

# Function to clean up
cleanup() {
    echo ""
    echo "=== Cleanup ==="
    
    echo "Deleting pod..."
    kubectl delete pod jmeter-test-pod -n "$NAMESPACE" --force --grace-period=0 2>/dev/null || echo "Pod not found"
    
    echo "Deleting ConfigMaps..."
    kubectl delete configmap jmeter-scripts -n "$NAMESPACE" 2>/dev/null || echo "Scripts ConfigMap not found"
    kubectl delete configmap jmeter-jmx-tests -n "$NAMESPACE" 2>/dev/null || echo "JMX ConfigMap not found"
    
    echo "Cleanup complete!"
}

# Function to show logs
show_logs() {
    echo ""
    echo "=== Pod Logs ==="
    kubectl logs jmeter-test-pod -n "$NAMESPACE" --tail=50 || echo "No logs available"
}

# Function to shell into pod
shell() {
    echo ""
    echo "=== Opening shell in pod ==="
    kubectl exec -it jmeter-test-pod -n "$NAMESPACE" -- /bin/bash
}

# Main script logic
case "${1:-}" in
    "deploy")
        check_namespace
        deploy_configmaps
        deploy_pod
        show_status
        ;;
    "status")
        show_status
        ;;
    "test")
        run_test "${2:-dotcms}" "${3:-10}" "${4:-25}" "${5:-30}"
        ;;
    "results")
        copy_results "${2:-performance_results_k8s.csv}"
        ;;
    "cleanup")
        cleanup
        ;;
    "logs")
        show_logs
        ;;
    "shell")
        shell
        ;;
    *)
        echo "Usage: $0 {deploy|status|test|results|cleanup|logs|shell}"
        echo ""
        echo "Commands:"
        echo "  deploy                           - Deploy all components to K8s"
        echo "  status                           - Show deployment status"
        echo "  test <type> [threads] [eps] [duration] - Run a test"
        echo "    Types: dotcms, direct, limits"
        echo "    Example: $0 test dotcms 10 25 30"
        echo "  results [filename]               - Copy results from pod"
        echo "  cleanup                          - Remove all deployed components"
        echo "  logs                             - Show pod logs"
        echo "  shell                            - Open shell in pod"
        echo ""
        echo "Examples:"
        echo "  $0 deploy                        # Deploy everything"
        echo "  $0 test dotcms 15 50 45         # Test DotCMS API: 15 threads, 50 eps, 45s"
        echo "  $0 test direct 25 100 60        # Test Direct Analytics: 25 threads, 100 eps, 60s"
        echo "  $0 test limits                   # Run full performance limits test"
        echo "  $0 results my_results.csv       # Copy results to file"
        exit 1
        ;;
esac 