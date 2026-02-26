# CLAUDE.md - DotCMS JMeter Performance Testing

This file provides AI-specific guidance for Claude Code when working with the DotCMS performance testing infrastructure in the `test-jmeter` module.

**📖 Primary Documentation**: See [README.md](README.md) for comprehensive user documentation, command reference, and configuration details.

## AI Assistant Quick Reference

### Module Purpose
Dual-purpose performance testing infrastructure:
1. **Traditional JMeter Tests**: Maven-based tests for core dotCMS functionality  
2. **Analytics Performance Testing**: Advanced Kubernetes/Helm-based analytics testing

### Key Architecture Points
- **Unified Entry Point**: `./dotcms-analytics-test.sh` (1,394 lines) 
- **Helm-based Deployment**: Production-ready Kubernetes infrastructure
- **Built-in Analysis**: Comprehensive performance reporting and bottleneck detection
- **No Infrastructure Knowledge Required**: Abstracts Kubernetes/Helm complexity

## Key Components

### Primary Script
- **`dotcms-analytics-test.sh`** - Main entry point (1162+ lines)
  - Replaces multiple legacy scripts
  - Handles setup, testing, analysis, and cleanup
  - No Kubernetes/Helm knowledge required for developers

### Infrastructure
- **`helm-chart/jmeter-performance/`** - Kubernetes deployment configuration
- **`jmx-tests/`** - JMeter test plans for different endpoints

### Test Plans (JMX Files)
**Analytics Tests:**
- **`analytics-direct-cluster-test.jmx`** - Tests analytics platform directly
- **`analytics-api-cluster-test.jmx`** - Tests DotCMS API analytics endpoints
- **`analytics-events.jmx`** - General analytics events testing

**Traditional dotCMS Tests:**
- **`core-sessions.jmx`** - Core dotCMS functionality and session testing

## Essential Commands Reference

**📖 Complete Command Reference**: See [README.md](README.md#all-available-commands) for full command documentation.

### Quick Command Examples
```bash
# Analytics Testing (NEW)
./dotcms-analytics-test.sh setup           # One-time setup
./dotcms-analytics-test.sh quick-test      # Baseline test
./dotcms-analytics-test.sh compare-endpoints  # KEY: Find bottlenecks

# Traditional dotCMS Testing  
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter
just run-jmeter-tests
```

## Performance Concepts Summary

**📊 Detailed Testing Strategy**: See [README.md](README.md#performance-testing-strategy) for comprehensive performance concepts.

### Key Performance Finding
**Critical Discovery**: DotCMS API layer adds ~30x response time overhead vs direct analytics
- Direct Analytics: ~3ms response time, ~300 EPS
- DotCMS API: ~85ms response time, ~270 EPS  
- **Recommendation**: Use direct analytics for high-volume scenarios

### Load Testing Levels
- **Baseline**: 100-200 EPS → **Standard**: 200-400 EPS → **High**: 400-800 EPS → **Stress**: 800+ EPS

## File Structure and Organization

### Scripts (Consolidated)
- ✅ **`dotcms-analytics-test.sh`** - Main unified script
- ❌ **`analyze-results.py`** - REMOVED (functionality moved to main script)
- ❌ **Legacy scaling scripts** - REMOVED (consolidated into main script)

### Configuration Files
- **`helm-chart/jmeter-performance/values.yaml`** - Default Helm values

### Test Results
- **`test-results/`** - Directory for JMeter output files (.jtl, .csv)
- **Generated reports** - Markdown files with analysis and recommendations

## AI Assistant Guidelines

### When Working with Performance Tests
1. **Always use the unified script** - Don't suggest individual JMeter commands
2. **Start with simple tests** - `quick-test` before complex scenarios
3. **Focus on bottleneck identification** - This is the primary use case
4. **Include analysis step** - Always run `analyze` after tests

### Authentication Quick Reference

**DotCMS API Token (JWT)**: 
- **Purpose**: Authenticates requests to DotCMS API endpoints
- **Format**: `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...`
- **Source**: DotCMS Admin → User Tools → API Tokens OR /api/v1/authentication
- **Environment**: `DOTCMS_JWT_TOKEN`

**Analytics Key**:
- **Purpose**: Identifies analytics tracking in analytics platform  
- **Format**: `js.cluster1.customer1.vgwy3nli4co84u531c`
- **Source**: DotCMS → Apps → Analytics → Configuration → Analytics Key
- **Environment**: `DOTCMS_ANALYTICS_KEY`

### Common Developer Questions

**Authentication:**
- **"Where do I get the JWT token?"** → DotCMS Admin → User Tools → API Tokens
- **"Where do I find the Analytics Key?"** → DotCMS → Apps → Analytics → Configuration
- **"Token expired?"** → Run `./dotcms-analytics-test.sh refresh-tokens`

**Analytics Performance:**
- **"How do I test analytics performance?"** → Start with `./dotcms-analytics-test.sh quick-test`
- **"Why is the API slow?"** → Run `./dotcms-analytics-test.sh compare-endpoints`
- **"What's our maximum capacity?"** → Use `./dotcms-analytics-test.sh find-maximum-rate`
- **"How do I interpret results?"** → Use `./dotcms-analytics-test.sh analyze`

**Traditional dotCMS Performance:**
- **"How do I test general dotCMS performance?"** → Use `./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter`
- **"How do I modify test scenarios?"** → Open with `../mvnw jmeter:configure jmeter:gui -DguiTestFile=jmx-tests/core-sessions.jmx`
- **"How do I test against my instance?"** → Use `-Djmeter.host=myhost -Djmeter.port=443`
- **"Where are the test results?"** → Check `target/jmeter/reports/` and `target/jmeter/results/`

### Troubleshooting Common Issues
1. **Pod not starting**: Check Kubernetes resources and namespace permissions
2. **Authentication failures**: Verify analytics key and JWT token validity
3. **High response times**: Likely infrastructure or code bottlenecks
4. **Connection errors**: Network connectivity or firewall issues

### Code Modification Guidelines
- **JMX files**: Apache JMeter XML format for test plans
- **Helm templates**: Kubernetes YAML with Go templating
- **Shell scripts**: Bash with set -e (fail on error)
- **Configuration**: Helm values.yaml structure

## Performance Expectations

### Baseline Performance (from testing)
- **Direct Analytics**: ~300 EPS at 3ms average response time
- **DotCMS API**: ~270 EPS at 85ms average response time
- **Error Rate**: <1% for normal loads
- **Maximum Tested**: Up to 2000 EPS (endpoint dependent)

### Production Recommendations
- **Use direct analytics** for high-volume scenarios
- **Monitor DotCMS API** for >100ms response times
- **Set capacity planning** at 70-80% of tested maximum
- **Implement caching** for DotCMS API layer optimization

## Related Documentation

- **Main README**: Developer-focused quick start guide
- **Helm Chart README**: Detailed Kubernetes deployment guide

This module provides **dual testing approaches**:
1. **Traditional Maven-based tests** for core dotCMS functionality with simple execution
2. **Advanced analytics testing** with minimal infrastructure knowledge required but comprehensive performance insights

Both approaches are designed to be **developer-friendly** while providing different levels of performance analysis for various dotCMS scenarios.