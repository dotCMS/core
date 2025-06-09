# Direct Analytics Platform Load Testing

This directory contains load testing configuration for the **Direct Analytics Platform** endpoint, which bypasses the DotCMS API and posts events directly to the analytics platform.

## 🎯 **Overview**

- **Target Endpoint:** `http://localhost:8001/api/v1/event` (via kubectl port-forward)
- **Purpose:** Test maximum events per second and response times for direct analytics ingestion
- **Event Format:** Enhanced analytics event with additional metadata fields
- **Authentication:** Analytics key passed both in URL query parameter and event body

## 📋 **Prerequisites**

### kubectl Port Forward Setup

The analytics service runs in Kubernetes with ingress security restrictions. You must set up kubectl port forwarding:

```bash
# Switch to the correct cluster context
kubectl config use-context k8s-internal-rd

# Set up port forwarding (run in background)
kubectl port-forward -n analytics-dev service/jitsu-api 8001:8001 &

# Verify connection with working payload
# Replace placeholder values with your actual configuration:
# - YOUR_ANALYTICS_KEY_HERE: Your analytics key
# - your-dotcms-instance.dotcms.cloud: Your DotCMS instance hostname
# - example-customer: Your customer name
# - example-cluster: Your cluster name
curl "http://localhost:8001/api/v1/event?token=YOUR_ANALYTICS_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "utc_time": "2025-06-05T12:40:00.000Z",
    "event_type": "PAGE_REQUEST",
    "key": "YOUR_ANALYTICS_KEY_HERE",
    "url": "/test",
    "doc_host": "your-dotcms-instance.dotcms.cloud",
    "environment_name": "auth",
    "sessionId": "TEST123",
    "request_id": "test-123",
    "user": {"identifier": "test.user", "email": "test@dotcms.com"},
    "customer_name": "example-customer",
    "cluster": "example-cluster",
    "src": "dotAnalytics",
    "type": "track",
    "timestamp": "2025-06-05T12:40:00.000Z"
  }' \
  --max-time 5
```

**Expected Response:** `{"status":"ok"}`

### ⚠️ **Current Status**

- **✅ kubectl port-forward**: Working
- **✅ Authentication**: Working (token in URL + payload)
- **✅ Service connectivity**: Working
- **🔧 JMeter payload**: Needs JSON format correction

The JMeter test configuration is set up for localhost:8001 but the JSON payload format needs to be updated to match the working curl example above.

## 📁 **Files** 