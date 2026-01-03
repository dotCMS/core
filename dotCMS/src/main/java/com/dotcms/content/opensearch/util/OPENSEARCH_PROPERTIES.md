# OpenSearch Client Configuration Properties

This document describes all available configuration properties for the DotOpenSearchClientProvider.

## Connection Properties

### Endpoints Configuration
```properties
# Multiple endpoints (comma-separated)
OS_ENDPOINTS=https://node1.opensearch.com:9200,https://node2.opensearch.com:9200

# OR individual endpoint components (when OS_ENDPOINTS is not set)
OS_HOSTNAME=localhost
OS_PROTOCOL=https
OS_PORT=9200
```

## Authentication Properties

### Basic Authentication
```properties
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=password123
```

### JWT Token Authentication
```properties
OS_AUTH_TYPE=JWT
OS_AUTH_JWT_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Certificate Authentication
```properties
OS_AUTH_TYPE=CERT
OS_TLS_CLIENT_CERT=/path/to/client.pem
OS_TLS_CLIENT_KEY=/path/to/client.key
```

## TLS/SSL Properties

### Enable TLS
```properties
OS_TLS_ENABLED=true
OS_TLS_TRUST_SELF_SIGNED=false
OS_TLS_CA_CERT=/path/to/ca-cert.pem
```

## Connection Pool Properties

### Timeouts (in milliseconds)
```properties
OS_CONNECTION_TIMEOUT=10000    # 10 seconds
OS_SOCKET_TIMEOUT=30000        # 30 seconds
```

### Connection Limits
```properties
OS_MAX_CONNECTIONS=100
OS_MAX_CONNECTIONS_PER_ROUTE=50
```

## Usage Examples

### Local Development (HTTP)
```properties
OS_HOSTNAME=localhost
OS_PROTOCOL=http
OS_PORT=9200
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
```

### Production HTTPS with Basic Auth
```properties
OS_ENDPOINTS=https://opensearch.prod.com:9200
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=prod_user
OS_AUTH_BASIC_PASSWORD=secure_password
OS_TLS_ENABLED=true
OS_TLS_TRUST_SELF_SIGNED=false
```

### Cluster with JWT Authentication
```properties
OS_ENDPOINTS=https://os-node1:9200,https://os-node2:9200,https://os-node3:9200
OS_AUTH_TYPE=JWT
OS_AUTH_JWT_TOKEN=your_jwt_token_here
OS_TLS_ENABLED=true
OS_TLS_TRUST_SELF_SIGNED=true
```

### Certificate-based Authentication
```properties
OS_ENDPOINTS=https://secure-opensearch:9200
OS_AUTH_TYPE=CERT
OS_TLS_ENABLED=true
OS_TLS_CLIENT_CERT=/opt/certs/client.pem
OS_TLS_CLIENT_KEY=/opt/certs/client.key
OS_TLS_CA_CERT=/opt/certs/ca.pem
```

## Property Defaults

| Property | Default Value | Description |
|----------|---------------|-------------|
| OS_HOSTNAME | localhost | OpenSearch hostname |
| OS_PROTOCOL | https | Protocol (http/https) |
| OS_PORT | 9200 | OpenSearch port |
| OS_AUTH_TYPE | BASIC | Authentication type |
| OS_TLS_ENABLED | false | Enable TLS/SSL |
| OS_TLS_TRUST_SELF_SIGNED | false | Trust self-signed certificates |
| OS_CONNECTION_TIMEOUT | 10000 | Connection timeout (ms) |
| OS_SOCKET_TIMEOUT | 30000 | Socket timeout (ms) |
| OS_MAX_CONNECTIONS | 100 | Max total connections |
| OS_MAX_CONNECTIONS_PER_ROUTE | 50 | Max connections per route |

## Configuration Priority

1. **OS_ENDPOINTS** - If set, overrides individual hostname/protocol/port
2. **OS_HOSTNAME/OS_PROTOCOL/OS_PORT** - Used when OS_ENDPOINTS is not set
3. Default values - Used when properties are not configured

## Programmatic Configuration

You can also configure the client programmatically using the builder pattern:

```java
OpenSearchClientConfig config = OpenSearchClientConfig.builder()
    .endpoints("https://node1:9200", "https://node2:9200")
    .username("admin")
    .password("password")
    .tlsEnabled(true)
    .trustSelfSigned(false)
    .connectionTimeout(Duration.ofSeconds(15))
    .build();

DotOpenSearchClientProvider provider = new DotOpenSearchClientProvider(config);
OpenSearchClient client = provider.getClient();
```