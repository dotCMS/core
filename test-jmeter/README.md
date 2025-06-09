# DotCMS Analytics Load Testing

This project provides comprehensive load testing capabilities for DotCMS analytics endpoints using Apache JMeter. It includes automated scaling tests, result analysis, and performance profiling tools with a simple developer-friendly interface.

## 🚀 Quick Start for Developers

**One script does everything - no kubectl/helm knowledge required!**

```bash
# 1. Setup testing infrastructure (one time)
./dotcms-analytics-test.sh setup

# 2. Run a quick baseline test
./dotcms-analytics-test.sh quick-test

# 3. Compare endpoints to identify bottlenecks
./dotcms-analytics-test.sh compare-endpoints

# 4. Analyze results
./dotcms-analytics-test.sh analyze
```

That's it! The script handles all Kubernetes deployment, test execution, and analysis automatically.

## 📋 Prerequisites

- **Kubernetes cluster access** (kubectl configured)
- **Helm 3.x** installed
- **Analytics namespace** existing in your cluster (default: `analytics-dev`)
- **Analytics platform** deployed and running

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
# Use different analytics endpoint
./dotcms-analytics-test.sh setup --analytics-host your-analytics.domain.com

# Use different analytics key
./dotcms-analytics-test.sh setup --analytics-key js.cluster1.customer1.yourkey

# Use different namespace
./dotcms-analytics-test.sh setup --namespace your-namespace

# Custom test duration
./dotcms-analytics-test.sh quick-test --duration 300
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

*For detailed analysis and optimization recommendations, use the `generate-report` command after running your tests.*

## 🤝 Contributing

When adding new tests or modifying existing ones:

1. **Test locally** with small loads first
2. **Document** any new parameters or features  
3. **Update** this README with new findings
4. **Validate** that tests work with both local and cloud instances

## 📝 License

This testing framework is part of the DotCMS project and follows the same licensing terms.
