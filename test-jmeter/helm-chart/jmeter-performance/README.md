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



```bash
# Install with required parameters
helm install my-jmeter ./helm-chart/jmeter-performance \
  --set auth.jwtToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  --set endpoints.dotcms.host="demo.dotcms.com"

# Install with custom analytics configuration
helm install my-jmeter ./helm-chart/jmeter-performance \
  --set auth.jwtToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  --set endpoints.dotcms.host="your-instance.dotcms.cloud" \
  --set endpoints.analytics.host="custom-analytics.example.com"
```

## Configuration

### Key Parameters

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| **REQUIRED CONFIGURATION** | | | |
| `auth.jwtToken` | DotCMS API JWT token | *(none)* | ✅ **YES** |
| `endpoints.dotcms.host` | DotCMS API hostname | *(none)* | ✅ **YES** |
| **OPTIONAL CONFIGURATION** | | | |
| `namespace.name` | Kubernetes namespace | `analytics-dev` | No |
| `namespace.create` | Create namespace if not exists | `false` | No |
| `endpoints.analytics.host` | Analytics API hostname | `jitsu-api.analytics-dev.svc.cluster.local` | No |
| `endpoints.analytics.key` | Analytics key from DotCMS App | *(from secret)* | No* |
| **RESOURCE LIMITS** | | | |
| `pod.resources.requests.cpu` | CPU request | `2000m` | No |
| `pod.resources.requests.memory` | Memory request | `8Gi` | No |
| `pod.resources.limits.cpu` | CPU limit | `4000m` | No |
| `pod.resources.limits.memory` | Memory limit | `16Gi` | No |

**Notes:**
- ✅ **Required**: Must be provided via `--set` or values file
- *Analytics key: Required but managed via Kubernetes secret when using `auth.useSecret=true`

### Full Configuration

See [values.yaml](./values.yaml) for all available configuration options.

## Usage

### Basic Testing

```bash
# Quick DotCMS API test
kubectl exec my-jmeter-pod -n analytics-dev -- bash -c "
  export PATH=/opt/jmeter/bin:\$PATH &&
  jmeter -n -t /opt/jmx-tests/analytics-api-cluster-test.jmx \\
    -l /opt/test-results/test.jtl \\
    -Jthread.number=10 -Jevents.per.second=25 -Jtest.duration=30"

# Performance limits testing
kubectl exec my-jmeter-pod -n analytics-dev -- bash /opt/jmeter-scripts/performance-limits-test.sh
```

### Using Custom Values Files

Custom values files allow you to override any configuration in the default `values.yaml`:

```bash
# ⚠️ IMPORTANT: Copy the example file - DO NOT edit the example directly
cp ../../custom-values.yaml.example my-values.yaml

# Edit your copy (NOT the .example file) with your specific configuration
# Edit my-values.yaml with your actual settings

# Install with custom values
helm install my-jmeter ./helm-chart/jmeter-performance -f my-values.yaml

# Combine multiple values files
helm install my-jmeter ./helm-chart/jmeter-performance \
  -f base-values.yaml \
  -f environment-specific.yaml
```

### Values File Hierarchy

Values are applied in order (later overrides earlier):
1. Default values (`values.yaml`)
2. Custom values files (`-f custom.yaml`)
3. Command line overrides (`--set key=value`)

### Example Custom Values

```yaml
# production-values.yaml
endpoints:
  dotcms:
    host: "production.dotcms.cloud"

pod:
  resources:
    requests:
      cpu: "8000m"
      memory: "32Gi"
    limits:
      cpu: "16000m"
      memory: "64Gi"

testing:
  defaults:
    threads: 2000
    eventsPerSecond: 5000
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