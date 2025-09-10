# Performance Tests (JMeter)

## Overview

The dotCMS performance testing suite uses Apache JMeter for load testing and performance validation. The framework includes both traditional JMeter tests and advanced Kubernetes-based analytics performance testing for comprehensive bottleneck analysis.

## Test Structure

### Location & Architecture
- **Path**: `test-jmeter/`
- **Framework**: Apache JMeter + Kubernetes (for analytics)
- **Test Runner**: Maven + Custom shell scripts
- **Integration Status**: Partially integrated - not fully automated in CI/CD

### Project Structure
```
test-jmeter/
├── src/test/jmeter/                # JMeter test plans
│   ├── dotcms-load-test.jmx        # Main load test
│   ├── content-api-test.jmx        # Content API specific tests
│   ├── graphql-test.jmx            # GraphQL performance tests
│   └── user-scenarios.jmx          # User workflow scenarios
├── src/test/resources/             # Test data and configurations
│   ├── data/                       # Test data files
│   ├── config/                     # Environment configurations
│   └── scripts/                    # Pre/post test scripts
├── kubernetes/                     # Kubernetes deployment configs
│   ├── analytics-tests/            # Analytics performance tests
│   ├── deployment.yaml             # K8s deployment
│   └── service.yaml                # K8s service configuration
├── docker/                         # Docker configurations
│   └── docker-compose.yml          # Docker environment
├── scripts/                        # Test execution scripts
│   ├── run-load-test.sh            # Load test execution
│   ├── run-analytics-test.sh       # Analytics test execution
│   └── generate-report.sh          # Report generation
└── pom.xml                         # Maven configuration
```

## Traditional JMeter Tests

### Maven Configuration
```xml
<plugin>
    <groupId>com.lazerycode.jmeter</groupId>
    <artifactId>jmeter-maven-plugin</artifactId>
    <version>3.7.0</version>
    <configuration>
        <testFilesDirectory>src/test/jmeter</testFilesDirectory>
        <resultsDirectory>target/jmeter/results</resultsDirectory>
        <testResultsTimestamp>false</testResultsTimestamp>
        <propertiesUser>
            <jmeter.host>${jmeter.host}</jmeter.host>
            <jmeter.port>${jmeter.port}</jmeter.port>
            <jmeter.thread.number>${jmeter.thread.number}</jmeter.thread.number>
            <jmeter.test.duration>${jmeter.test.duration}</jmeter.test.duration>
        </propertiesUser>
    </configuration>
</plugin>
```

### Test Plan Properties
```xml
<properties>
    <jmeter.host>localhost</jmeter.host>
    <jmeter.port>8080</jmeter.port>
    <jmeter.protocol>http</jmeter.protocol>
    <jmeter.thread.number>10</jmeter.thread.number>
    <jmeter.test.duration>300</jmeter.test.duration>
    <jmeter.ramp.time>60</jmeter.ramp.time>
    <jmeter.results.file>target/jmeter/results/results.jtl</jmeter.results.file>
</properties>
```

### Load Test Configuration
```xml
<!-- Thread Group Configuration -->
<ThreadGroup>
    <name>dotCMS Load Test</name>
    <numThreads>${jmeter.thread.number}</numThreads>
    <rampTime>${jmeter.ramp.time}</rampTime>
    <duration>${jmeter.test.duration}</duration>
    <scheduler>true</scheduler>
    <delayedStart>false</delayedStart>
</ThreadGroup>

<!-- HTTP Request Defaults -->
<ConfigTestElement>
    <name>HTTP Request Defaults</name>
    <serverNameOrIp>${jmeter.host}</serverNameOrIp>
    <port>${jmeter.port}</port>
    <protocol>${jmeter.protocol}</protocol>
    <connectTimeout>30000</connectTimeout>
    <responseTimeout>30000</responseTimeout>
</ConfigTestElement>
```

### Test Scenarios

#### 1. Content API Load Test
```xml
<!-- Login Request -->
<HTTPSamplerProxy>
    <name>Login</name>
    <method>POST</method>
    <path>/api/v1/authentication/api-token</path>
    <body>{
        "username": "admin@dotcms.com",
        "password": "admin"
    }</body>
    <headers>
        <header>
            <name>Content-Type</name>
            <value>application/json</value>
        </header>
    </headers>
</HTTPSamplerProxy>

<!-- Extract Token -->
<JSONExtractor>
    <name>Extract Token</name>
    <jsonPath>$.entity.token</jsonPath>
    <variableName>auth_token</variableName>
</JSONExtractor>

<!-- Content Creation Request -->
<HTTPSamplerProxy>
    <name>Create Content</name>
    <method>POST</method>
    <path>/api/v1/content</path>
    <body>{
        "contentType": "webPageContent",
        "title": "Load Test Content ${__threadNum}",
        "body": "Performance test content body"
    }</body>
    <headers>
        <header>
            <name>Content-Type</name>
            <value>application/json</value>
        </header>
        <header>
            <name>Authorization</name>
            <value>Bearer ${auth_token}</value>
        </header>
    </headers>
</HTTPSamplerProxy>
```

#### 2. GraphQL Performance Test
```xml
<!-- GraphQL Query -->
<HTTPSamplerProxy>
    <name>GraphQL Content Query</name>
    <method>POST</method>
    <path>/api/v1/graphql</path>
    <body>{
        "query": "query { contentSearch(query: \"*\", limit: 10) { content { identifier title } } }"
    }</body>
    <headers>
        <header>
            <name>Content-Type</name>
            <value>application/json</value>
        </header>
        <header>
            <name>Authorization</name>
            <value>Bearer ${auth_token}</value>
        </header>
    </headers>
</HTTPSamplerProxy>
```

#### 3. User Workflow Scenario
```xml
<!-- User Journey: Login → Browse → Create → Edit → Publish -->
<TransactionController>
    <name>User Workflow</name>
    
    <!-- Login -->
    <HTTPSamplerProxy>
        <name>01_Login</name>
        <method>POST</method>
        <path>/api/v1/authentication/api-token</path>
    </HTTPSamplerProxy>
    
    <!-- Browse Content -->
    <HTTPSamplerProxy>
        <name>02_Browse_Content</name>
        <method>GET</method>
        <path>/api/v1/content</path>
    </HTTPSamplerProxy>
    
    <!-- Create Content -->
    <HTTPSamplerProxy>
        <name>03_Create_Content</name>
        <method>POST</method>
        <path>/api/v1/content</path>
    </HTTPSamplerProxy>
    
    <!-- Edit Content -->
    <HTTPSamplerProxy>
        <name>04_Edit_Content</name>
        <method>PUT</method>
        <path>/api/v1/content/${content_id}</path>
    </HTTPSamplerProxy>
    
    <!-- Publish Content -->
    <HTTPSamplerProxy>
        <name>05_Publish_Content</name>
        <method>PUT</method>
        <path>/api/v1/workflow/actions/publish/content/${content_id}</path>
    </HTTPSamplerProxy>
</TransactionController>
```

### Running Traditional JMeter Tests
```bash
# Run all JMeter tests
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter

# Run with custom parameters
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=localhost \
    -Djmeter.port=8080 \
    -Djmeter.thread.number=20 \
    -Djmeter.test.duration=600

# Run specific test plan
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.test.plan=content-api-test.jmx

# Run with Docker environment
docker-compose -f docker/docker-compose.yml up -d
./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=localhost \
    -Djmeter.port=8080
```

## Advanced Analytics Performance Testing

### Kubernetes-Based Testing
The advanced analytics performance testing uses Kubernetes for scalable load generation and sophisticated bottleneck analysis.

#### Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Load Generator │    │   Target dotCMS  │    │  Analytics API  │
│   (K8s Pods)    │───▶│    Instance      │◀───│   (Direct)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Metrics       │    │   Response      │    │   Bottleneck    │
│  Collection     │    │   Analysis      │    │   Analysis      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### Kubernetes Configuration
```yaml
# kubernetes/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jmeter-analytics-test
spec:
  replicas: 3
  selector:
    matchLabels:
      app: jmeter-analytics-test
  template:
    metadata:
      labels:
        app: jmeter-analytics-test
    spec:
      containers:
      - name: jmeter-runner
        image: dotcms/jmeter-analytics:latest
        env:
        - name: DOTCMS_HOST
          value: "dotcms.local"
        - name: ANALYTICS_API_HOST
          value: "analytics-api.local"
        - name: TEST_DURATION
          value: "600"
        - name: THREAD_COUNT
          value: "50"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

#### Test Execution Script
```bash
#!/bin/bash
# scripts/run-analytics-test.sh

set -e

echo "Starting Analytics Performance Test..."

# Deploy JMeter test pods
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=jmeter-analytics-test --timeout=300s

# Run performance test
kubectl exec -it deployment/jmeter-analytics-test -- \
    /opt/jmeter/bin/jmeter.sh \
    -n -t /tests/analytics-performance-test.jmx \
    -Jdotcms.host=${DOTCMS_HOST} \
    -Janalytics.host=${ANALYTICS_API_HOST} \
    -Jthread.count=${THREAD_COUNT} \
    -Jtest.duration=${TEST_DURATION} \
    -l /results/analytics-test-results.jtl

# Generate comparative analysis
kubectl exec -it deployment/jmeter-analytics-test -- \
    python3 /scripts/analyze-performance.py \
    --dotcms-results /results/dotcms-results.jtl \
    --analytics-results /results/analytics-results.jtl \
    --output /results/comparison-report.html

# Copy results locally
kubectl cp deployment/jmeter-analytics-test:/results ./test-results/

echo "Analytics Performance Test Complete"
echo "Results available in: ./test-results/"
```

### Performance Analysis Scripts
```python
#!/usr/bin/env python3
# scripts/analyze-performance.py

import pandas as pd
import matplotlib.pyplot as plt
import argparse
import json

def analyze_performance_results(dotcms_file, analytics_file, output_file):
    """Analyze and compare performance between dotCMS and direct analytics API"""
    
    # Load JMeter results
    dotcms_data = pd.read_csv(dotcms_file)
    analytics_data = pd.read_csv(analytics_file)
    
    # Calculate statistics
    dotcms_stats = {
        'avg_response_time': dotcms_data['elapsed'].mean(),
        'max_response_time': dotcms_data['elapsed'].max(),
        'min_response_time': dotcms_data['elapsed'].min(),
        'success_rate': (dotcms_data['success'] == True).sum() / len(dotcms_data),
        'throughput': len(dotcms_data) / (dotcms_data['timeStamp'].max() - dotcms_data['timeStamp'].min()) * 1000
    }
    
    analytics_stats = {
        'avg_response_time': analytics_data['elapsed'].mean(),
        'max_response_time': analytics_data['elapsed'].max(),
        'min_response_time': analytics_data['elapsed'].min(),
        'success_rate': (analytics_data['success'] == True).sum() / len(analytics_data),
        'throughput': len(analytics_data) / (analytics_data['timeStamp'].max() - analytics_data['timeStamp'].min()) * 1000
    }
    
    # Generate comparison report
    report = {
        'dotcms_performance': dotcms_stats,
        'analytics_performance': analytics_stats,
        'bottlenecks': identify_bottlenecks(dotcms_data, analytics_data),
        'recommendations': generate_recommendations(dotcms_stats, analytics_stats)
    }
    
    # Create visualizations
    create_performance_charts(dotcms_data, analytics_data, output_file)
    
    # Save report
    with open(f'{output_file}.json', 'w') as f:
        json.dump(report, f, indent=2)
    
    print(f"Performance analysis complete. Report saved to {output_file}")

def identify_bottlenecks(dotcms_data, analytics_data):
    """Identify performance bottlenecks"""
    bottlenecks = []
    
    # Response time analysis
    if dotcms_data['elapsed'].mean() > analytics_data['elapsed'].mean() * 1.5:
        bottlenecks.append("dotCMS API response time significantly slower than direct analytics API")
    
    # Error rate analysis
    dotcms_error_rate = (dotcms_data['success'] == False).sum() / len(dotcms_data)
    analytics_error_rate = (analytics_data['success'] == False).sum() / len(analytics_data)
    
    if dotcms_error_rate > analytics_error_rate * 2:
        bottlenecks.append("dotCMS API has significantly higher error rate")
    
    # Throughput analysis
    dotcms_throughput = len(dotcms_data) / (dotcms_data['timeStamp'].max() - dotcms_data['timeStamp'].min())
    analytics_throughput = len(analytics_data) / (analytics_data['timeStamp'].max() - analytics_data['timeStamp'].min())
    
    if analytics_throughput > dotcms_throughput * 1.5:
        bottlenecks.append("Direct analytics API has significantly higher throughput")
    
    return bottlenecks

def generate_recommendations(dotcms_stats, analytics_stats):
    """Generate performance improvement recommendations"""
    recommendations = []
    
    if dotcms_stats['avg_response_time'] > analytics_stats['avg_response_time'] * 1.2:
        recommendations.append("Consider optimizing dotCMS API response time")
        recommendations.append("Implement caching for frequently accessed analytics data")
    
    if dotcms_stats['success_rate'] < 0.95:
        recommendations.append("Investigate and fix API reliability issues")
    
    if dotcms_stats['throughput'] < analytics_stats['throughput'] * 0.8:
        recommendations.append("Consider horizontal scaling of dotCMS instances")
        recommendations.append("Optimize database queries for analytics data")
    
    return recommendations

def create_performance_charts(dotcms_data, analytics_data, output_file):
    """Create performance comparison charts"""
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))
    
    # Response time comparison
    axes[0, 0].hist(dotcms_data['elapsed'], alpha=0.7, label='dotCMS', bins=50)
    axes[0, 0].hist(analytics_data['elapsed'], alpha=0.7, label='Analytics API', bins=50)
    axes[0, 0].set_title('Response Time Distribution')
    axes[0, 0].set_xlabel('Response Time (ms)')
    axes[0, 0].legend()
    
    # Response time over time
    axes[0, 1].plot(dotcms_data['timeStamp'], dotcms_data['elapsed'], label='dotCMS', alpha=0.7)
    axes[0, 1].plot(analytics_data['timeStamp'], analytics_data['elapsed'], label='Analytics API', alpha=0.7)
    axes[0, 1].set_title('Response Time Over Time')
    axes[0, 1].set_xlabel('Time')
    axes[0, 1].set_ylabel('Response Time (ms)')
    axes[0, 1].legend()
    
    # Success rate comparison
    dotcms_success_rate = (dotcms_data['success'] == True).sum() / len(dotcms_data) * 100
    analytics_success_rate = (analytics_data['success'] == True).sum() / len(analytics_data) * 100
    
    axes[1, 0].bar(['dotCMS', 'Analytics API'], [dotcms_success_rate, analytics_success_rate])
    axes[1, 0].set_title('Success Rate Comparison')
    axes[1, 0].set_ylabel('Success Rate (%)')
    
    # Throughput comparison
    dotcms_throughput = len(dotcms_data) / ((dotcms_data['timeStamp'].max() - dotcms_data['timeStamp'].min()) / 1000)
    analytics_throughput = len(analytics_data) / ((analytics_data['timeStamp'].max() - analytics_data['timeStamp'].min()) / 1000)
    
    axes[1, 1].bar(['dotCMS', 'Analytics API'], [dotcms_throughput, analytics_throughput])
    axes[1, 1].set_title('Throughput Comparison')
    axes[1, 1].set_ylabel('Requests per Second')
    
    plt.tight_layout()
    plt.savefig(f'{output_file}_charts.png')
    plt.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Analyze JMeter performance results')
    parser.add_argument('--dotcms-results', required=True, help='dotCMS JMeter results file')
    parser.add_argument('--analytics-results', required=True, help='Analytics API JMeter results file')
    parser.add_argument('--output', required=True, help='Output file prefix')
    
    args = parser.parse_args()
    
    analyze_performance_results(args.dotcms_results, args.analytics_results, args.output)
```

## Test Data Management

### Test Data Generation
```bash
#!/bin/bash
# scripts/generate-test-data.sh

# Generate content data
cat > src/test/resources/data/content-data.csv << EOF
contentType,title,body
webPageContent,Test Page 1,Test content body 1
webPageContent,Test Page 2,Test content body 2
blogPost,Test Blog 1,Test blog content 1
newsItem,Test News 1,Test news content 1
EOF

# Generate user data
cat > src/test/resources/data/user-data.csv << EOF
username,password,role
testuser1,password123,contributor
testuser2,password123,author
testuser3,password123,editor
EOF

echo "Test data generated successfully"
```

### JMeter CSV Data Set Config
```xml
<CSVDataSet>
    <name>Content Data</name>
    <filename>src/test/resources/data/content-data.csv</filename>
    <variableNames>contentType,title,body</variableNames>
    <delimiter>,</delimiter>
    <quotedData>true</quotedData>
    <recycle>true</recycle>
    <stopThread>false</stopThread>
    <shareMode>shareMode.all</shareMode>
</CSVDataSet>
```

## Running Performance Tests

### Maven Commands
```bash
# Run all performance tests
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter

# Run with specific configuration
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=production.dotcms.com \
    -Djmeter.port=443 \
    -Djmeter.protocol=https \
    -Djmeter.thread.number=50 \
    -Djmeter.test.duration=1800

# Run analytics performance test
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.test.plan=analytics-performance-test.jmx

# Generate HTML report
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.generate.report=true
```

### Docker Environment
```bash
# Start test environment
docker-compose -f docker/docker-compose.yml up -d

# Run performance test
./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
    -Djmeter.host=localhost \
    -Djmeter.port=8080

# Scale services for higher load
docker-compose -f docker/docker-compose.yml up -d --scale dotcms=3
```

### Kubernetes Analytics Tests
```bash
# Deploy test environment
kubectl apply -f kubernetes/

# Run analytics performance test
./scripts/run-analytics-test.sh

# Monitor test execution
kubectl logs -f deployment/jmeter-analytics-test

# Clean up test environment
kubectl delete -f kubernetes/
```

## Performance Monitoring and Analysis

### JMeter Reporting
```bash
# Generate HTML dashboard report
jmeter -g target/jmeter/results/results.jtl -o target/jmeter/reports/

# Generate specific reports
jmeter -g target/jmeter/results/results.jtl -o target/jmeter/reports/ \
    -Jjmeter.reportgenerator.overall_granularity=1000
```

### Key Performance Metrics
```xml
<!-- Response Time Percentiles -->
<ResponseTimePercentiles>
    <property name="aggregate_rpt_pct1" value="90"/>
    <property name="aggregate_rpt_pct2" value="95"/>
    <property name="aggregate_rpt_pct3" value="99"/>
</ResponseTimePercentiles>

<!-- Throughput Measurement -->
<ThroughputMeasurement>
    <property name="summariser.interval" value="30"/>
    <property name="summariser.log" value="true"/>
</ThroughputMeasurement>
```

### Performance Thresholds
```xml
<!-- Performance Assertions -->
<ResponseAssertion>
    <name>Response Time Threshold</name>
    <responseTime>2000</responseTime>
    <testType>duration</testType>
</ResponseAssertion>

<ThroughputAssertion>
    <name>Throughput Threshold</name>
    <throughput>100</throughput>
    <testType>throughput</testType>
</ThroughputAssertion>
```

## CI/CD Integration (Limited)

### GitHub Actions (Manual Trigger)
```yaml
# .github/workflows/performance-test.yml
name: Performance Test
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        default: 'staging'
      duration:
        description: 'Test duration (seconds)'
        required: true
        default: '300'
      threads:
        description: 'Number of threads'
        required: true
        default: '10'

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Run Performance Test
        run: |
          ./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
            -Djmeter.host=${{ github.event.inputs.environment }}.dotcms.com \
            -Djmeter.thread.number=${{ github.event.inputs.threads }} \
            -Djmeter.test.duration=${{ github.event.inputs.duration }}
      
      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: performance-test-results
          path: |
            target/jmeter/results/
            target/jmeter/reports/
```

### Jenkins Integration
```groovy
// Jenkinsfile for performance testing
pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['staging', 'production'],
            description: 'Target environment'
        )
        string(
            name: 'THREAD_COUNT',
            defaultValue: '10',
            description: 'Number of threads'
        )
        string(
            name: 'DURATION',
            defaultValue: '300',
            description: 'Test duration in seconds'
        )
    }
    
    stages {
        stage('Performance Test') {
            steps {
                sh """
                    ./mvnw install -Djmeter.test.skip=false -pl :dotcms-test-jmeter \
                        -Djmeter.host=${params.ENVIRONMENT}.dotcms.com \
                        -Djmeter.thread.number=${params.THREAD_COUNT} \
                        -Djmeter.test.duration=${params.DURATION}
                """
            }
        }
        
        stage('Generate Report') {
            steps {
                sh """
                    jmeter -g target/jmeter/results/results.jtl \
                        -o target/jmeter/reports/
                """
            }
        }
        
        stage('Archive Results') {
            steps {
                archiveArtifacts artifacts: 'target/jmeter/results/**', fingerprint: true
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/jmeter/reports',
                    reportFiles: 'index.html',
                    reportName: 'Performance Test Report'
                ])
            }
        }
    }
}
```

## Debugging Performance Issues

### Local Debugging
```bash
# Run JMeter in GUI mode for debugging
jmeter -t src/test/jmeter/dotcms-load-test.jmx

# Enable debug logging
jmeter -Jjmeter.loglevel=DEBUG -t src/test/jmeter/dotcms-load-test.jmx

# Run with specific JVM options
jmeter -Xmx4g -XX:MaxPermSize=512m -t src/test/jmeter/dotcms-load-test.jmx
```

### Performance Profiling
```bash
# Enable JMeter profiling
jmeter -Jjmeter.save.saveservice.output_format=csv \
    -Jjmeter.save.saveservice.response_data=true \
    -t src/test/jmeter/dotcms-load-test.jmx

# Application profiling during test
java -javaagent:profiler.jar -jar dotcms.jar
```

### Common Performance Issues
```xml
<!-- Memory Issues -->
<JavaRequest>
    <name>Memory Configuration</name>
    <jvmArgs>-Xmx4g -XX:MaxPermSize=512m</jvmArgs>
</JavaRequest>

<!-- Connection Pool Issues -->
<HTTPSamplerProxy>
    <name>Connection Pool Config</name>
    <connectTimeout>30000</connectTimeout>
    <responseTimeout>60000</responseTimeout>
</HTTPSamplerProxy>

<!-- Database Connection Issues -->
<JDBCSampler>
    <name>DB Connection Config</name>
    <maxConnections>50</maxConnections>
    <connectionTimeout>30000</connectionTimeout>
</JDBCSampler>
```

## Best Practices

### ✅ Performance Testing Standards
- **Baseline establishment**: Always establish performance baselines
- **Environment consistency**: Use consistent test environments
- **Realistic load patterns**: Model actual user behavior
- **Gradual load increase**: Implement proper ramp-up periods
- **Monitor system resources**: Track CPU, memory, and I/O during tests

### ✅ Test Design Principles
```xml
<!-- Proper ramp-up configuration -->
<ThreadGroup>
    <name>Realistic Load Pattern</name>
    <numThreads>100</numThreads>
    <rampTime>300</rampTime>  <!-- 5 minutes ramp-up -->
    <duration>1800</duration>  <!-- 30 minutes steady state -->
</ThreadGroup>

<!-- Think time simulation -->
<UniformRandomTimer>
    <name>Think Time</name>
    <delay>1000</delay>
    <range>2000</range>
</UniformRandomTimer>
```

### ✅ Results Analysis
- **Response time analysis**: Focus on percentiles, not just averages
- **Throughput analysis**: Measure requests per second under load
- **Error rate analysis**: Identify failure patterns
- **Resource utilization**: Monitor server resources during tests
- **Bottleneck identification**: Use profiling tools to find performance bottlenecks

## Common Issues and Solutions

### 1. Memory Issues
```bash
# Increase JMeter heap size
export HEAP="-Xms2g -Xmx4g"
jmeter -t test-plan.jmx
```

### 2. Connection Issues
```xml
<!-- Increase connection timeout -->
<HTTPSamplerProxy>
    <connectTimeout>60000</connectTimeout>
    <responseTimeout>120000</responseTimeout>
</HTTPSamplerProxy>
```

### 3. Test Data Issues
```bash
# Generate large test datasets
for i in {1..1000}; do
    echo "testuser$i,password123,contributor" >> user-data.csv
done
```

### 4. Kubernetes Resource Issues
```yaml
# Increase resource limits
resources:
  limits:
    memory: "4Gi"
    cpu: "2000m"
  requests:
    memory: "2Gi"
    cpu: "1000m"
```

## Integration Status

### Current State
- **✅ Basic JMeter tests**: Functional with Maven integration
- **🔄 Analytics tests**: Advanced K8s-based testing available
- **❌ CI/CD integration**: Not fully automated
- **❌ Regular execution**: Manual trigger only

### Future Improvements
- Automated CI/CD integration
- Regular performance regression testing
- Integration with monitoring systems
- Automated bottleneck detection
- Performance trend analysis

## Location Information
- **Test Plans**: `test-jmeter/src/test/jmeter/`
- **Test Data**: `test-jmeter/src/test/resources/data/`
- **Results**: `test-jmeter/target/jmeter/results/`
- **Reports**: `test-jmeter/target/jmeter/reports/`
- **Kubernetes Config**: `test-jmeter/kubernetes/`
- **Scripts**: `test-jmeter/scripts/`