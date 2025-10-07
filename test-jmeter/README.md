# DotCMS JMeter Performance Tests

This module contains comprehensive JMeter performance tests for dotCMS, including both **core dotCMS functionality tests** and **analytics performance testing**.

## 🎯 Testing Capabilities

### **1. Core dotCMS Performance Tests**
Traditional JMeter tests for general dotCMS functionality using Maven integration.

### **2. Analytics Performance Testing** 
**🚀 NEW**: Advanced load testing infrastructure for DotCMS analytics to identify performance bottlenecks and establish capacity baselines.

**Developer Entry Point**: Use the single `./dotcms-analytics-test.sh` script - no Kubernetes or JMeter knowledge required.

## Quick Start (5 minutes)

```bash
# 1. One-time setup
./dotcms-analytics-test.sh setup

# 2. Run baseline tests
./dotcms-analytics-test.sh quick-test

# 3. Compare DotCMS API vs Direct Analytics to find bottlenecks
./dotcms-analytics-test.sh compare-endpoints

# 4. View results with recommendations
./dotcms-analytics-test.sh analyze
```

**That's it!** The script handles all infrastructure deployment, test execution, and analysis automatically.

## 📋 Prerequisites

- **Kubernetes cluster access** (kubectl configured)
- **Helm 3.x** installed
- **Analytics namespace** existing in your cluster (default: `analytics-dev`)
- **Analytics platform** deployed and running

## 🔐 Authentication Setup

The testing framework requires two authentication tokens:

### DotCMS API Token (JWT Token)
**Purpose**: Authenticates requests to DotCMS API endpoints  
**Format**: `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...`

**Option 1: Manual Creation (Recommended for production)**
1. Login to DotCMS Admin
2. Navigate to **Admin → User Tools → API Tokens**
3. Create new token for your user
4. Copy the generated JWT token

**Option 2: Interactive Generation (Development)**
- The script can generate temporary tokens using your DotCMS username/password
- Tokens expire in 30 minutes for security
- Credentials are never stored

### Analytics Key
**Purpose**: Identifies your analytics tracking in the analytics platform  
**Format**: `js.cluster1.customer1.vgwy3nli4co84u531c`

**Location in DotCMS**:
1. Navigate to **Apps → Analytics → Configuration**
2. Copy the **Analytics Key** value
3. This key connects your DotCMS instance to the analytics platform

### Usage Options

**Environment Variables (CI/CD)**:
```bash
# Set secure authentication via environment variables
export DOTCMS_JWT_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
export DOTCMS_ANALYTICS_KEY="js.cluster1.customer1.vgwy3nli4co84u531c"

# Setup creates custom-values.yaml with your configuration
./dotcms-analytics-test.sh setup --dotcms-host demo.dotcms.com

# All subsequent deployments use the persistent configuration
./dotcms-analytics-test.sh quick-test
```

**Interactive Setup**:
```bash
./dotcms-analytics-test.sh setup
# Prompts for DotCMS host, username, password, and analytics key
```

**Manual Setup with Parameters**:
```bash
./dotcms-analytics-test.sh setup \
  --dotcms-host your-instance.dotcms.cloud \
  --analytics-host analytics.example.com \
  --namespace production-analytics
```

**Refresh Expired Tokens**:
```bash
./dotcms-analytics-test.sh refresh-tokens
```

## 📖 All Available Commands

```bash
# View all available commands and options
./dotcms-analytics-test.sh --help

# Infrastructure management
./dotcms-analytics-test.sh setup          # Deploy test infrastructure
./dotcms-analytics-test.sh status         # Check infrastructure status
./dotcms-analytics-test.sh cleanup        # Remove test infrastructure

# Testing commands
./dotcms-analytics-test.sh quick-test     # Baseline test
./dotcms-analytics-test.sh dotcms-test    # DotCMS API test
./dotcms-analytics-test.sh compare-endpoints  # Side-by-side comparison
./dotcms-analytics-test.sh performance-test   # Progressive testing
./dotcms-analytics-test.sh stress-test    # High-load testing
./dotcms-analytics-test.sh scaling-test   # Comprehensive scaling analysis
./dotcms-analytics-test.sh single-test 500 100  # Custom: 500 EPS, 100 threads

# Bottleneck analysis
./dotcms-analytics-test.sh bottleneck-analysis  # Multi-level bottleneck testing
./dotcms-analytics-test.sh find-maximum-rate    # Find failure points

# Analysis and reporting
./dotcms-analytics-test.sh analyze        # Analyze latest test results
./dotcms-analytics-test.sh analyze-all    # Analyze all results
./dotcms-analytics-test.sh generate-report # Generate performance report
./dotcms-analytics-test.sh logs           # View test execution logs
```

## 🔧 Configuration

The script uses sensible defaults but can be customized:

```bash
# Required: Specify DotCMS host for setup
./dotcms-analytics-test.sh setup --dotcms-host demo.dotcms.com

# Use different analytics endpoint
./dotcms-analytics-test.sh setup \
  --dotcms-host demo.dotcms.com \
  --analytics-host your-analytics.domain.com

# Use different analytics key
./dotcms-analytics-test.sh setup \
  --dotcms-host demo.dotcms.com \
  --analytics-key js.cluster1.customer1.vgwy3nli4co84u531c

# Use different namespace
./dotcms-analytics-test.sh setup \
  --dotcms-host demo.dotcms.com \
  --namespace your-namespace

# Custom test duration
./dotcms-analytics-test.sh quick-test --duration 300
```

## 📝 Custom Values Files

### Overview

Helm values files allow you to override any configuration in the default `values.yaml`. This provides a powerful way to customize your testing environment without modifying the base configuration.

### File Types

**🔧 `custom-values.yaml.example`** - Template file with examples (DO NOT EDIT)
- Contains comprehensive examples and documentation
- Committed to git as reference documentation
- Use as template to create your own values files

**📝 `custom-values.yaml`** - Your actual configuration file (edit this)
- Created automatically by the setup script
- Contains your specific DotCMS host and settings
- Excluded from git (contains configuration details)
- Safe to edit and customize

**⚙️ `values.yaml`** - Default Helm chart values (DO NOT EDIT)
- Contains sensible defaults for all configuration
- Part of the Helm chart itself
- Override with custom values files, not direct edits

### Using Custom Values Files

**🚀 Recommended: Automatic Generation**
```bash
# The script automatically copies the example file and customizes it for you
# Creates custom-values.yaml with your DotCMS host and current settings
./dotcms-analytics-test.sh setup --dotcms-host demo.dotcms.com

# Use a different values file name (also auto-generated)
./dotcms-analytics-test.sh setup --values-file production-values.yaml

# What happens automatically:
# 1. Copies custom-values.yaml.example → custom-values.yaml (or your filename)
# 2. Uncomments and sets your DotCMS host configuration
# 3. Sets useSecret: true for secure authentication
# 4. Includes all example configurations as comments for easy customization
```

**Manual Creation (Advanced Users)**:
```bash
# ⚠️ IMPORTANT: Copy the example file - DO NOT edit the example directly
cp custom-values.yaml.example my-values.yaml

# Edit your copy (NOT the .example file) with your configuration  
# Edit my-values.yaml with your specific settings
./dotcms-analytics-test.sh setup --values-file my-values.yaml

# Note: The script will still auto-generate if the file doesn't exist
```

**Direct Helm Usage**:
```bash
# Use directly with Helm
helm install my-jmeter ./helm-chart/jmeter-performance -f custom-values.yaml

# Combine multiple values files
helm install my-jmeter ./helm-chart/jmeter-performance \
  -f custom-values.yaml \
  -f production-overrides.yaml
```

### Example Configurations

**High-Performance Production**:
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
  jvm:
    heap: "-Xms24g -Xmx48g"

testing:
  defaults:
    threads: 2000
    eventsPerSecond: 5000
    duration: 1800  # 30 minutes
```

**Development/Testing**:
```yaml
# dev-values.yaml
endpoints:
  dotcms:
    host: "dev.dotcms.com"

namespace:
  name: "analytics-dev"

pod:
  resources:
    requests:
      cpu: "1000m"
      memory: "4Gi"
    limits:
      cpu: "2000m"
      memory: "8Gi"

testing:
  defaults:
    threads: 100
    eventsPerSecond: 200
    duration: 300  # 5 minutes
```

**Custom Analytics Platform**:
```yaml
# custom-analytics-values.yaml
endpoints:
  dotcms:
    host: "demo.dotcms.com"
  analytics:
    host: "analytics.company.com"
    port: 9000
    scheme: "https"
    
environment:
  name: "production"
  customer: "your-company"
```

### Values File Hierarchy

Values are applied in the following order (later values override earlier ones):

1. **Default values** (`helm-chart/jmeter-performance/values.yaml`)
2. **Custom values file** (`-f custom-values.yaml`)
3. **Additional values files** (`-f override-values.yaml`)
4. **Command line overrides** (`--set key=value`)

### Security Considerations

- **Custom values files are excluded from git** (see `.gitignore`)
- **Files contain configuration details** - treat as sensitive
- **Use environment variables** for truly sensitive data like tokens
- **Set restrictive file permissions** (600) for values files containing secrets

### Common Overrides

```yaml
# Resource scaling
pod:
  resources:
    requests:
      memory: "16Gi"
      cpu: "4000m"

# Test parameters
testing:
  defaults:
    eventsPerSecond: 1000
    duration: 600

# Environment configuration
environment:
  name: "staging"
  cluster: "staging-cluster"

# Namespace
namespace:
  name: "analytics-staging"
```

## 📊 Understanding Test Results

The `analyze` command provides comprehensive performance metrics:

- **Total Requests**: Number of analytics events sent
- **Success/Error Rates**: Percentage of successful vs failed requests  
- **Response Times**: Average, min, max, and percentile analysis
- **Actual vs Target EPS**: How close you got to your target throughput
- **Performance Assessment**: Automated recommendations

### Sample Output

```
=== ANALYTICS PERFORMANCE TEST RESULTS ===
Total Requests:       24817
Successful Requests:  24817 (100.0%)
Failed Requests:      0 (0.0%)

Performance Metrics:
  Target EPS:         200
  Actual EPS:         207.0
  Efficiency:         103.5%

Response Times:
  Average:            3.2 ms
  Minimum:            1 ms
  Maximum:            98 ms
  Requests >1s:       0
  Requests >5s:       0

Performance Assessment:
  ✅ EXCELLENT: Error rate <5% - System performing optimally
  ✅ EFFICIENT: Achieving >90% of target throughput

Recommendations:
  • This load level is sustainable for production
  • Consider this as your baseline capacity
```

## 🎯 Performance Testing Strategy

### Recommended Testing Workflow

1. **Baseline Testing**
   ```bash
   ./dotcms-analytics-test.sh quick-test
   ./dotcms-analytics-test.sh dotcms-test
   ```

2. **Comparative Analysis**
   ```bash
   ./dotcms-analytics-test.sh compare-endpoints
   ```

3. **Bottleneck Identification**
   ```bash
   ./dotcms-analytics-test.sh bottleneck-analysis
   ```

4. **Capacity Planning**
   ```bash
   ./dotcms-analytics-test.sh find-maximum-rate
   ```

5. **Comprehensive Reporting**
   ```bash
   ./dotcms-analytics-test.sh generate-report
   ```

### Load Testing Levels

The testing framework supports various load levels:

- **Baseline**: Light load testing (100-200 EPS)
- **Standard**: Normal production load (200-400 EPS)
- **High**: Peak traffic simulation (400-800 EPS)
- **Stress**: Beyond normal capacity (800+ EPS)

## 📈 Performance Analysis

### Key Metrics Tracked

- **Events Per Second (EPS)**: Throughput measurement
- **Response Time**: End-to-end request latency
- **Error Rate**: Percentage of failed requests
- **Resource Utilization**: System performance under load

### Bottleneck Detection

The tool automatically identifies:
- Response time differences between endpoints
- Throughput limitations
- Error rate patterns
- Performance degradation points

## 🛠 Production Recommendations

### Setting Performance Baselines

1. **Run baseline tests** on your target environment
2. **Establish acceptable thresholds** for response times and error rates
3. **Set monitoring alerts** based on test findings
4. **Plan capacity** using maximum rate testing results

### Monitoring Strategy

- **Response Time Thresholds**: Based on endpoint comparison results
- **Error Rate Limits**: Typically <5% for production traffic
- **Capacity Planning**: Use 70-80% of maximum tested capacity

## 🔧 Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify analytics key is valid and has proper permissions
   - Check token format and expiration

2. **Connection Errors**
   - Verify network connectivity to analytics endpoint
   - Check firewall and security group settings

3. **High Response Times**
   - Review server resource utilization
   - Check for network latency issues
   - Analyze application logs for bottlenecks

4. **Test Infrastructure Issues**
   - Ensure Kubernetes cluster has sufficient resources
   - Verify namespace permissions and pod status
   - Check Helm chart deployment status

### Getting Help

1. **Check infrastructure status**: `./dotcms-analytics-test.sh status`
2. **View test logs**: `./dotcms-analytics-test.sh logs`
3. **Verify configuration**: Review command output for configuration details

## 📝 Advanced Usage

### Custom Test Scenarios

```bash
# Test specific load levels
./dotcms-analytics-test.sh single-test 300 75 direct

# Test with custom duration
./dotcms-analytics-test.sh quick-test --duration 600

# Find maximum rate with custom limit
./dotcms-analytics-test.sh find-maximum-rate --max-eps 1500
```

### Batch Testing

```bash
# Run comprehensive analysis suite
./dotcms-analytics-test.sh compare-endpoints && \
./dotcms-analytics-test.sh bottleneck-analysis && \
./dotcms-analytics-test.sh generate-report
```

---

## 📊 Core dotCMS Performance Tests (Maven-based)

### Test Configuration

The traditional JMeter tests are configured in `jmx-tests/core-sessions.jmx`. The default configuration includes:

- Host: dotcms.local
- Port: 443
- Ramp-up period: 0 seconds
- Startup delay: 5 seconds
- Test duration: 5 seconds

### Running the Tests

**Basic Execution:**

```bash
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter
```

**Using justfile alias:**

```bash
just run-jmeter-tests
```

**Opening test script in JMeter GUI:**

```bash
cd test-jmeter
../mvnw jmeter:configure jmeter:gui -DguiTestFile=jmx-tests/core-sessions.jmx
```

### Overriding Test Parameters

You can override the default settings using command-line properties:

```bash
# Override host and port
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=my-dotcms-instance.com \
    -Djmeter.port=444 \
    -Djmeter.thread.number=10

# Override test timing parameters
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.rampup=10 \
    -Djmeter.startup.delay=2 \
    -Djmeter.test.duration=30
```

### Test Reports

HTML reports are generated in the `target/jmeter/reports` directory. A CSV is also generated in the `target/jmeter/results` directory (e.g., `20241203-sessions.csv`) containing:

- Additional variables: JVM_HOSTNAME, SESSION_ID, X_DOT_SERVER
- SESSION_ID and X_DOT_SERVER can validate session propagation across multiple replicas

### Environment Password Configuration

When connecting to an external instance, avoid command-line passwords by using environment variables:

```bash
export JMETER_ADMIN_PASSWORD=mysecretpassword
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=myhost -Djmeter.env.password=true
```

### Configuration Files

- Main JMeter test file: `jmx-tests/core-sessions.jmx`
- Maven configuration: `pom.xml`

### Default Properties

```xml
<properties>
    <jmeter.host>dotcms.local</jmeter.host>
    <jmeter.port>443</jmeter.port>
    <jmeter.rampup>0</jmeter.rampup>
    <jmeter.startup.delay>5</jmeter.startup.delay>
    <jmeter.test.duration>60</jmeter.test.duration>
    <jmeter.thread.number>2</jmeter.thread.number>
</properties>
```

### Troubleshooting Traditional Tests

**Memory Requirements:**
JVM memory settings can be configured in the pom.xml:

```xml
<jMeterProcessJVMSettings>
    <arguments>
        <argument>-XX:MaxMetaspaceSize=256m</argument>
        <argument>-Xmx1024m</argument>
        <argument>-Xms1024m</argument>
    </arguments>
</jMeterProcessJVMSettings>
```

**High Load Testing:**
Currently runs standalone. For distributed high-load testing with multiple JMeter instances, ensure JMeter doesn't run on the same server as dotCMS to avoid resource conflicts.

---

*For detailed analysis and optimization recommendations, use the `generate-report` command after running your tests.*

## 🤝 Contributing

When adding new tests or modifying existing ones:

1. **Test locally** with small loads first
2. **Document** any new parameters or features  
3. **Update** this README with new findings
4. **Validate** that tests work with both local and cloud instances

## 📚 Additional Documentation

For advanced users and platform teams:

- **[Helm Chart Documentation](helm-chart/jmeter-performance/README.md)** - Detailed Kubernetes deployment configuration
- **[Performance Analysis Documentation](performance_analysis_log_rotation.md)** - Log rotation and analysis strategies

These documents provide deeper technical details but are **not required** for standard performance testing workflows.

## 📝 License

This testing framework is part of the DotCMS project and follows the same licensing terms.
