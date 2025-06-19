# JMeter Performance Testing Helm Chart

A Helm chart for deploying JMeter-based performance testing framework for DotCMS Analytics APIs.

## Overview

This chart deploys a comprehensive performance testing framework that can:
- Test DotCMS API endpoints
- Test Direct Analytics platforms 
- Run performance limit testing
- Generate detailed performance reports
- Support multiple environments

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- Access to target DotCMS and Analytics endpoints
- Valid JWT authentication token

## Installation

### Quick Start

```bash
# Add chart repository (if published)
helm repo add jmeter-performance https://your-repo.com/charts

# Install with default values
helm install my-jmeter jmeter-performance/jmeter-performance

# Or install from local directory
helm install my-jmeter ./helm-chart/jmeter-performance
```

### Environment-Specific Deployment

```bash
# Development environment
helm install jmeter-dev ./helm-chart/jmeter-performance -f values-dev.yaml

# Production environment  
helm install jmeter-prod ./helm-chart/jmeter-performance -f values-prod.yaml

# Custom JWT token
helm install my-jmeter ./helm-chart/jmeter-performance \
  --set auth.jwtToken="your-jwt-token-here"
```

## Configuration

### Key Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `namespace.name` | Kubernetes namespace | `analytics-dev` |
| `namespace.create` | Create namespace if not exists | `true` |
| `auth.jwtToken` | JWT token for authentication | (long JWT string) |
| `endpoints.dotcms.host` | DotCMS API hostname | `your-dotcms-instance.dotcms.cloud` |
| `endpoints.analytics.host` | Analytics API hostname | `jitsu-api.analytics-dev.svc.cluster.local` |
| `pod.resources.requests.cpu` | CPU request | `2000m` |
| `pod.resources.requests.memory` | Memory request | `4Gi` |
| `pod.resources.limits.cpu` | CPU limit | `4000m` |
| `pod.resources.limits.memory` | Memory limit | `8Gi` |

### Full Configuration

See [values.yaml](./values.yaml) for all available configuration options.

## Usage

### Basic Testing

```bash
# Quick DotCMS API test
kubectl exec my-jmeter-pod -n analytics-dev -- bash -c "
  export PATH=/opt/jmeter/bin:\$PATH &&
  jmeter -n -t /opt/jmx-tests/dotcms-api-cluster-test.jmx \\
    -l /opt/test-results/test.jtl \\
    -Jthread.number=10 -Jevents.per.second=25 -Jtest.duration=30"

# Performance limits testing
kubectl exec my-jmeter-pod -n analytics-dev -- bash /opt/jmeter-scripts/performance-limits-test.sh
```

### Advanced Configuration

```yaml
# custom-values.yaml
endpoints:
  dotcms:
    host: "custom-dotcms.example.com"
    port: 443
  analytics:
    host: "custom-analytics.example.com"
    port: 8080

testing:
  limits:
    dotcms:
      levels: [10, 25, 50, 100]
    analytics:
      levels: [50, 100, 200, 500]

pod:
  resources:
    limits:
      memory: "16Gi"
```

```bash
helm install custom-jmeter ./helm-chart/jmeter-performance -f custom-values.yaml
```

## Monitoring & Results

### Check Deployment Status

```bash
# Pod status
kubectl get pod -n analytics-dev

# Logs
kubectl logs my-jmeter-pod -n analytics-dev

# Describe pod
kubectl describe pod my-jmeter-pod -n analytics-dev
```

### Extract Results

```bash
# Find results files
kubectl exec my-jmeter-pod -n analytics-dev -- find /opt/test-results -name "*.csv"

# Copy specific results
kubectl exec my-jmeter-pod -n analytics-dev -- cat /opt/test-results/performance-limits-*/performance_results.csv > results.csv

# Copy all results
kubectl cp analytics-dev/my-jmeter-pod:/opt/test-results ./local-results/
```

## Upgrading

### Update JWT Token

```bash
helm upgrade my-jmeter ./helm-chart/jmeter-performance \
  --set auth.jwtToken="new-jwt-token"
```

### Update to New Chart Version

```bash
helm upgrade my-jmeter ./helm-chart/jmeter-performance \
  --reuse-values
```

### Update Resources

```bash
helm upgrade my-jmeter ./helm-chart/jmeter-performance \
  --set pod.resources.limits.memory="16Gi" \
  --set pod.resources.limits.cpu="8000m"
```

## Uninstallation

```bash
# Remove the deployment
helm uninstall my-jmeter

# Optionally remove namespace (if created by chart)
kubectl delete namespace analytics-dev
```

## Troubleshooting

### Common Issues

#### Pod Not Starting
```bash
# Check events
kubectl describe pod my-jmeter-pod -n analytics-dev

# Check resource constraints
kubectl top nodes
kubectl top pods -n analytics-dev
```

#### Authentication Failures
```bash
# Check JWT token in ConfigMap
kubectl get configmap jmeter-jmx-tests -n analytics-dev -o yaml | grep Authorization

# Test token validity
kubectl exec my-jmeter-pod -n analytics-dev -- curl -H "Authorization: Bearer YOUR_TOKEN" https://your-dotcms-instance.dotcms.cloud/api/v1/event
```

#### JMeter Command Not Found
```bash
# Check if JMeter was downloaded properly
kubectl exec my-jmeter-pod -n analytics-dev -- ls -la /opt/jmeter/bin/

# Check PATH
kubectl exec my-jmeter-pod -n analytics-dev -- echo $PATH
```

### Debug Shell

```bash
kubectl exec -it my-jmeter-pod -n analytics-dev -- /bin/bash
```

## Chart Development

### Validate Chart

```bash
helm lint ./helm-chart/jmeter-performance
helm template test ./helm-chart/jmeter-performance
```

### Package Chart

```bash
helm package ./helm-chart/jmeter-performance
```

## 📝 License

This testing framework is part of the DotCMS project and follows the same licensing terms.