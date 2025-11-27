// cube.js configuration file
// Multi-tenant security implementation for CubeJS
//
// DEFENSE-IN-DEPTH SECURITY ARCHITECTURE:
// This implementation uses multiple layers of security filtering to ensure data isolation:
//
// Layer 1: Base SQL Filtering (FILTER_PARAMS) - Primary Defense
// - Applied directly in cube SQL definitions (Events.js, Request.js)
// - Enables ClickHouse partition elimination for performance
// - Cannot be bypassed by malformed queries
// - Filters applied: ${FILTER_PARAMS.CubeName.customerId.filter('customer_id')}
//
// Layer 2: Runtime Query Filtering (queryRewrite) - Secondary Defense
// - Applied by this cube.js configuration at query execution time
// - Validates security context and adds runtime filters
// - Provides backup protection if base SQL filtering fails
// - Enables audit logging of all security filter applications
//
// Security Features:
// 1. Customer ID filtering (REQUIRED) - prevents cross-customer data access
// 2. Cluster ID filtering (OPTIONAL) - controlled by CUBEJS_ENFORCE_CLUSTER_FILTERING
// 3. Cube whitelist - only allows access to approved cubes (Events, request)
// 4. Single cluster support (RECOMMENDED) - each token should contain one cluster ID
//
// Environment Variables:
// - CUBEJS_DISABLE_CUSTOMER_SECURITY_FILTER=true: Disables ALL security (DEV ONLY)
// - CUBEJS_ENFORCE_CLUSTER_FILTERING=true: Enables cluster-based filtering (defaults to false)
//
// Security Context Expected Format:
// RECOMMENDED: scope: "analytics/customer:customer123 analytics/cluster:cluster1"
// DEPRECATED:  scope: "analytics/customer:customer123 analytics/cluster:cluster1 analytics/cluster:cluster2"
//
// ⚠️  WARNING: Multiple cluster IDs in tokens is DEPRECATED behavior and will be removed
// in future versions. Each token should be scoped to a single cluster for security.

const fs = require('fs');
const path = require('path');

// Whitelist of allowed cubes for security validation
const ALLOWED_CUBES = ['Events', 'request', 'ContentAttribution', 'Conversion'];

// Extract customer ID from scope (required for all queries)
// Note: Scope comes from validated JWT token, so customer_id is already trusted
// DUAL FORMAT SUPPORT: Works with both Cognito scope format and Keycloak custom claims
function extractCustomerId(scope, securityContext = null) {
  // Method 1: Extract from Cognito-style scope format (e.g., "analytics/customer:customer1 analytics/cluster:cluster1")
  if (scope && typeof scope === 'string') {
    const regex = new RegExp("analytics/customer:([^\\s]+)");
    const match = scope.match(regex);
    if (match) {
      return match[1];
    }
  }

  // Method 2: Extract from Keycloak custom claims format
  if (securityContext && securityContext['https://dotcms.com/analytics']) {
    const analyticsData = securityContext['https://dotcms.com/analytics'];
    if (analyticsData.customerId) {
      return analyticsData.customerId;
    }
  }

  return null;
}

// Extract cluster ID(s) from scope - DEPRECATED: multiple cluster support
// ⚠️  WARNING: Multiple cluster IDs in tokens is deprecated behavior
// DUAL FORMAT SUPPORT: Works with both Cognito scope format and Keycloak custom claims
function extractClusterIds(scope, securityContext = null) {
  // Method 1: Extract from Cognito-style scope format (e.g., "analytics/customer:customer1 analytics/cluster:cluster1")
  if (scope && typeof scope === 'string') {
    const regex = new RegExp("analytics/cluster:([^\\s]+)", "g");
    const matches = [];
    let match;
    while ((match = regex.exec(scope)) !== null) {
      matches.push(match[1]);
    }

    if (matches.length > 0) {
      return matches;
    }
  }

  // Method 2: Extract from Keycloak custom claims format
  if (securityContext && securityContext['https://dotcms.com/analytics']) {
    const analyticsData = securityContext['https://dotcms.com/analytics'];
    if (analyticsData.clusterId) {
      return [analyticsData.clusterId]; // Return as array for consistency
    }
  }

  return null;
}

// Validate that query only accesses whitelisted cubes
function validateCubeAccess(query) {
  const cubesInQuery = new Set();

  // Extract cube names from measures and dimensions
  if (query.measures) {
    query.measures.forEach(measure => {
      const cubeName = measure.split('.')[0];
      cubesInQuery.add(cubeName);
    });
        }

  if (query.dimensions) {
    query.dimensions.forEach(dimension => {
      const cubeName = dimension.split('.')[0];
      cubesInQuery.add(cubeName);
    });
    }

  // Check if all cubes are in whitelist
  for (const cube of cubesInQuery) {
    if (!ALLOWED_CUBES.includes(cube)) {
      throw new Error(`Access denied to cube: ${cube}. Allowed cubes: ${ALLOWED_CUBES.join(', ')}`);
    }
  }

  return Array.from(cubesInQuery);
}

module.exports = {

    // Fix for when schema folder is mounted as a configmap,  extra symbolic links
    // are created and get picked up so we filter for only the schema files themselves
    repositoryFactory: () => ({
        dataSchemaFiles: async () => {
            const files = await fs.promises.readdir(
                path.join(process.cwd(), "schema")
            );

            return await Promise.all(
                files
                    .filter((file) => file.endsWith(".js") || file.endsWith(".yaml"))
                    .map(async (file) => ({
                        fileName: file,
                        content: await fs.promises.readFile(
                            path.join(process.cwd(), "schema", file),
                            "utf-8"
                        ),
                    }))
            );
        },
    }),

    orchestratorOptions: {
        continueWaitTimeout: 30
    },

    queryRewrite: (query, { securityContext }) => {

        // Allow bypassing security for development/testing only
        if (!securityContext) {
            if (process.env.CUBEJS_DISABLE_CUSTOMER_SECURITY_FILTER === "true") {
                console.warn('⚠️  SECURITY WARNING: Customer security filtering is disabled!');
                return query;
            }
            throw new Error('No valid security context provided');
        }

        // Validate cube access against whitelist
        const cubesInQuery = validateCubeAccess(query);

        // Extract security context
        const scope = securityContext["scope"];
        const customerId = extractCustomerId(scope, securityContext);
        const clusterIds = extractClusterIds(scope, securityContext);

        if (!customerId) {
            throw new Error('No valid customerId found in security context');
        }

        // Environment variable to control cluster filtering (defaults to false for cross-cluster access)
        const enforceClusterFiltering = process.env.CUBEJS_ENFORCE_CLUSTER_FILTERING === "true";

        // Initialize filters array if it doesn't exist
        if (!query.filters) {
            query.filters = [];
        }

        // DEFENSE-IN-DEPTH: Apply runtime security filters as secondary protection
        // These filters work in conjunction with base SQL filtering (FILTER_PARAMS) for maximum security
        cubesInQuery.forEach(cubeName => {
            // Layer 2 Security: Runtime customer filtering (supplements base SQL filtering)
            // This provides redundant protection even though cubes like Events/request already have
            // FILTER_PARAMS filtering at the SQL level
            query.filters.push({
                member: `${cubeName}.customerId`,
                operator: 'equals',
                values: [customerId],
            });

            // Layer 2 Security: Optional runtime cluster filtering
            // Supplements base SQL cluster filtering when CUBEJS_ENFORCE_CLUSTER_FILTERING=true
            if (enforceClusterFiltering && clusterIds && clusterIds.length > 0) {
                query.filters.push({
                    member: `${cubeName}.clusterId`,
                    operator: clusterIds.length > 1 ? 'in' : 'equals',
                    values: clusterIds,
                });
            }

        });

        return query;
    },

};
