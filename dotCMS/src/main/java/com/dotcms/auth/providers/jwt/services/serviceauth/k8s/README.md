# Kubernetes Deployment Guide for Service-to-Service Authentication

This guide shows how to deploy dotCMS and external microservices with shared JWT signing keys for secure service-to-service authentication.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Kubernetes Cluster                               │
│                                                                          │
│  ┌─────────────────┐                      ┌─────────────────┐           │
│  │  dotCMS Pod 1   │                      │  wa11y Service  │           │
│  │  (customer-a)   │  JWT Bearer Token    │                 │           │
│  │                 │ ───────────────────▶ │                 │           │
│  └─────────────────┘                      └─────────────────┘           │
│           │                                        │                     │
│           │                                        │                     │
│  ┌─────────────────┐                               │                     │
│  │  dotCMS Pod 2   │                               │                     │
│  │  (customer-b)   │ ─────────────────────────────┘                     │
│  └─────────────────┘                                                     │
│           │                                                              │
│           ▼                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    Shared K8s Secret                             │    │
│  │                   (service-auth-key)                             │    │
│  │                                                                   │    │
│  │   signing-key: "base64-encoded-256-bit-secret"                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Step 1: Generate a Strong Signing Key

```bash
# Generate a 256-bit (32 byte) secret key
openssl rand -base64 32
# Example output: K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=

# Or use a UUID-based approach for easier rotation tracking
uuidgen | shasum -a 256 | cut -d' ' -f1 | head -c 64
```

## Step 2: Create Kubernetes Secret

```yaml
# service-auth-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: service-auth-key
  namespace: dotcms  # or your namespace
  labels:
    app.kubernetes.io/component: service-auth
    app.kubernetes.io/managed-by: kubectl
type: Opaque
stringData:
  # The signing key - same key must be shared with all services
  signing-key: "K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="

  # Optional: Store service-specific config
  wa11y-url: "http://wa11y-service.dotcms.svc.cluster.local:8080"
  analytics-url: "http://analytics-service.dotcms.svc.cluster.local:8080"
```

Apply it:
```bash
kubectl apply -f service-auth-secret.yaml
```

## Step 3: Configure dotCMS Deployment

```yaml
# dotcms-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dotcms
  namespace: dotcms
spec:
  replicas: 3
  selector:
    matchLabels:
      app: dotcms
  template:
    metadata:
      labels:
        app: dotcms
    spec:
      containers:
      - name: dotcms
        image: dotcms/dotcms:latest
        ports:
        - containerPort: 8080
        env:
        # Enable service authentication
        - name: SERVICE_AUTH_ENABLED
          value: "true"

        # JWT signing key from secret
        - name: JSON_WEB_TOKEN_SECRET
          valueFrom:
            secretKeyRef:
              name: service-auth-key
              key: signing-key

        # Optional: Configure JWT settings
        - name: SERVICE_AUTH_JWT_EXPIRATION_SECONDS
          value: "300"
        - name: SERVICE_AUTH_ISSUER
          value: "dotcms-production"

        # Service URLs from secret (optional)
        - name: WA11Y_SERVICE_URL
          valueFrom:
            secretKeyRef:
              name: service-auth-key
              key: wa11y-url

        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

## Step 4: Configure Receiving Service (wa11y Example)

### Node.js/Express Example

```javascript
// wa11y-service/src/auth.js
const jwt = require('jsonwebtoken');

// Get signing key from environment (same as dotCMS)
const SIGNING_KEY = process.env.JWT_SIGNING_KEY;
const EXPECTED_AUDIENCE = 'wa11y-checker';

function validateServiceToken(req, res, next) {
  const authHeader = req.headers['authorization'];

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing or invalid Authorization header' });
  }

  const token = authHeader.substring(7);

  try {
    const decoded = jwt.verify(token, SIGNING_KEY, {
      algorithms: ['HS256'],
      audience: EXPECTED_AUDIENCE,
    });

    // Token is valid - attach claims to request
    req.serviceAuth = {
      serviceId: decoded.sid,
      sourceCluster: decoded.src,
      issuer: decoded.iss,
    };

    next();
  } catch (err) {
    console.error('JWT validation failed:', err.message);
    return res.status(401).json({ error: 'Invalid token: ' + err.message });
  }
}

module.exports = { validateServiceToken };
```

```javascript
// wa11y-service/src/app.js
const express = require('express');
const { validateServiceToken } = require('./auth');

const app = express();
app.use(express.json());

// Protected endpoint - requires valid service JWT
app.post('/api/v1/check', validateServiceToken, async (req, res) => {
  const { url, standard } = req.body;

  console.log(`Accessibility check requested by ${req.serviceAuth.sourceCluster}`);

  // Perform accessibility check...
  const results = await checkAccessibility(url, standard);

  res.json(results);
});

// Health check - no auth required
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(8080);
```

### wa11y Kubernetes Deployment

```yaml
# wa11y-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wa11y-service
  namespace: dotcms
spec:
  replicas: 2
  selector:
    matchLabels:
      app: wa11y-service
  template:
    metadata:
      labels:
        app: wa11y-service
    spec:
      containers:
      - name: wa11y
        image: your-registry/wa11y-service:latest
        ports:
        - containerPort: 8080
        env:
        # Same signing key as dotCMS
        - name: JWT_SIGNING_KEY
          valueFrom:
            secretKeyRef:
              name: service-auth-key
              key: signing-key

        # Service identity
        - name: SERVICE_ID
          value: "wa11y-checker"

        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
---
apiVersion: v1
kind: Service
metadata:
  name: wa11y-service
  namespace: dotcms
spec:
  selector:
    app: wa11y-service
  ports:
  - port: 8080
    targetPort: 8080
```

## Step 5: Network Policy (Optional but Recommended)

Restrict which pods can communicate with your services:

```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: wa11y-service-policy
  namespace: dotcms
spec:
  podSelector:
    matchLabels:
      app: wa11y-service
  policyTypes:
  - Ingress
  ingress:
  # Only allow traffic from dotCMS pods
  - from:
    - podSelector:
        matchLabels:
          app: dotcms
    ports:
    - protocol: TCP
      port: 8080
```

## Key Rotation

When rotating the signing key:

1. **Generate new key:**
   ```bash
   NEW_KEY=$(openssl rand -base64 32)
   ```

2. **Update secret:**
   ```bash
   kubectl create secret generic service-auth-key-v2 \
     --from-literal=signing-key="$NEW_KEY" \
     -n dotcms
   ```

3. **Rolling update** - Update deployments to use new secret:
   ```bash
   kubectl set env deployment/dotcms JSON_WEB_TOKEN_SECRET- \
     --from=secret/service-auth-key-v2 -n dotcms
   kubectl set env deployment/wa11y-service JWT_SIGNING_KEY- \
     --from=secret/service-auth-key-v2 -n dotcms
   ```

4. **Verify** and **delete old secret:**
   ```bash
   kubectl delete secret service-auth-key -n dotcms
   ```

## Troubleshooting

### Check if service auth is enabled
```bash
curl https://your-dotcms.com/api/v1/service-auth/status
```

### Validate a token manually
```bash
curl -X POST https://your-dotcms.com/api/v1/service-auth/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGc...", "expectedAudience": "wa11y-checker"}'
```

### Debug JWT issues
```bash
# Decode JWT payload (doesn't verify signature)
echo "eyJhbGc..." | cut -d'.' -f2 | base64 -d | jq .
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `SERVICE_AUTH_ENABLED=false` | Not configured | Set env var to `true` |
| `Invalid signature` | Key mismatch | Verify same key in all services |
| `Token expired` | Clock skew or old token | Check NTP sync, reduce token TTL |
| `Audience mismatch` | Wrong service ID | Check `expectedAudience` matches `serviceId` |

## Security Best Practices

1. **Short token TTL** - Keep `SERVICE_AUTH_JWT_EXPIRATION_SECONDS` low (300s default)
2. **Network policies** - Restrict pod-to-pod communication
3. **TLS everywhere** - Use service mesh or ingress TLS
4. **Audit logging** - Monitor `SecurityLogger` output
5. **Rotate keys** - Establish regular key rotation schedule
6. **Least privilege** - Each service gets its own audience claim
