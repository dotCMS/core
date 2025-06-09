# dotCMS GraphQL API

A comprehensive GraphQL API implementation for dotCMS that provides flexible content delivery and page rendering capabilities.

## Architecture Overview

<div align="center">

```svg
<svg width="800" height="600" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="clientGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#e3f2fd;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#bbdefb;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="apiGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#f3e5f5;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#e1bee7;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="securityGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#fff3e0;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#ffcc02;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="coreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#e8f5e8;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#c8e6c9;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="dataGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#fce4ec;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#f8bbd9;stop-opacity:1" />
    </linearGradient>
  </defs>
  
  <!-- Background -->
  <rect width="800" height="600" fill="#fafafa"/>
  
  <!-- Title -->
  <text x="400" y="30" text-anchor="middle" font-family="Arial, sans-serif" font-size="18" font-weight="bold" fill="#2c3e50">dotCMS GraphQL API Architecture</text>
  
  <!-- Client Layer -->
  <rect x="50" y="60" width="700" height="60" rx="10" fill="url(#clientGrad)" stroke="#1976d2" stroke-width="2"/>
  <text x="400" y="85" text-anchor="middle" font-family="Arial, sans-serif" font-size="14" font-weight="bold" fill="#1976d2">🌐 CLIENT APPLICATIONS</text>
  <text x="150" y="105" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#1976d2">GraphQL SDK</text>
  <text x="400" y="105" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#1976d2">Web Browser</text>
  <text x="650" y="105" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#1976d2">Mobile Apps</text>
  
  <!-- Arrow down -->
  <path d="M400 130 L400 150" stroke="#333" stroke-width="3" marker-end="url(#arrowhead)"/>
  
  <!-- API Endpoint -->
  <rect x="250" y="160" width="300" height="50" rx="8" fill="url(#apiGrad)" stroke="#7b1fa2" stroke-width="2"/>
  <text x="400" y="180" text-anchor="middle" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="#7b1fa2">🚪 /api/v1/graphql</text>
  <text x="400" y="195" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" fill="#7b1fa2">HTTP Endpoint</text>
  
  <!-- Arrow down -->
  <path d="M400 220 L400 240" stroke="#333" stroke-width="3" marker-end="url(#arrowhead)"/>
  
  <!-- Security Layer -->
  <rect x="200" y="250" width="400" height="50" rx="8" fill="url(#securityGrad)" stroke="#f57c00" stroke-width="2"/>
  <text x="400" y="270" text-anchor="middle" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="#f57c00">🔐 Security & Schema Layer</text>
  <text x="400" y="285" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" fill="#f57c00">Authentication • Permissions • Dynamic Schema</text>
  
  <!-- Arrow down -->
  <path d="M400 310 L400 330" stroke="#333" stroke-width="3" marker-end="url(#arrowhead)"/>
  
  <!-- GraphQL APIs -->
  <rect x="50" y="340" width="700" height="80" rx="10" fill="url(#coreGrad)" stroke="#388e3c" stroke-width="2"/>
  <text x="400" y="360" text-anchor="middle" font-family="Arial, sans-serif" font-size="14" font-weight="bold" fill="#388e3c">🔧 GRAPHQL APIs</text>
  
  <!-- API Boxes -->
  <rect x="70" y="370" width="150" height="40" rx="5" fill="#ffffff" stroke="#388e3c" stroke-width="1"/>
  <text x="145" y="385" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" font-weight="bold" fill="#388e3c">📄 Content API</text>
  <text x="145" y="398" text-anchor="middle" font-family="Arial, sans-serif" font-size="9" fill="#388e3c">BlogCollection</text>
  
  <rect x="240" y="370" width="150" height="40" rx="5" fill="#ffffff" stroke="#388e3c" stroke-width="1"/>
  <text x="315" y="385" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" font-weight="bold" fill="#388e3c">🏗️ Page API</text>
  <text x="315" y="398" text-anchor="middle" font-family="Arial, sans-serif" font-size="9" fill="#388e3c">Templates & Layouts</text>
  
  <rect x="410" y="370" width="150" height="40" rx="5" fill="#ffffff" stroke="#388e3c" stroke-width="1"/>
  <text x="485" y="385" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" font-weight="bold" fill="#388e3c">🧭 Navigation</text>
  <text x="485" y="398" text-anchor="middle" font-family="Arial, sans-serif" font-size="9" fill="#388e3c">DotNavigation</text>
  
  <rect x="580" y="370" width="150" height="40" rx="5" fill="#ffffff" stroke="#388e3c" stroke-width="1"/>
  <text x="655" y="385" text-anchor="middle" font-family="Arial, sans-serif" font-size="10" font-weight="bold" fill="#388e3c">📊 System APIs</text>
  <text x="655" y="398" text-anchor="middle" font-family="Arial, sans-serif" font-size="9" fill="#388e3c">Metadata & Cache</text>
  
  <!-- Arrow down -->
  <path d="M400 430 L400 450" stroke="#333" stroke-width="3" marker-end="url(#arrowhead)"/>
  
  <!-- Data Processing -->
  <rect x="50" y="460" width="700" height="60" rx="10" fill="url(#dataGrad)" stroke="#c2185b" stroke-width="2"/>
  <text x="400" y="480" text-anchor="middle" font-family="Arial, sans-serif" font-size="14" font-weight="bold" fill="#c2185b">⚙️ DATA PROCESSING LAYER</text>
  <text x="180" y="500" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#c2185b">🔍 Data Fetchers</text>
  <text x="400" y="500" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#c2185b">🏭 Type Providers</text>
  <text x="620" y="500" text-anchor="middle" font-family="Arial, sans-serif" font-size="11" fill="#c2185b">💾 Cache Layer</text>
  
  <!-- Arrow down -->
  <path d="M400 530 L400 550" stroke="#333" stroke-width="3" marker-end="url(#arrowhead)"/>
  
  <!-- dotCMS Core -->
  <rect x="150" y="560" width="500" height="30" rx="8" fill="#37474f" stroke="#263238" stroke-width="2"/>
  <text x="400" y="580" text-anchor="middle" font-family="Arial, sans-serif" font-size="12" font-weight="bold" fill="white">💽 dotCMS CORE - Content Types • Pages • Users • Permissions</text>
  
  <!-- Arrow marker definition -->
  <defs>
    <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
      <polygon points="0 0, 10 3.5, 0 7" fill="#333"/>
    </marker>
  </defs>
  
  <!-- Side annotations -->
  <text x="20" y="95" font-family="Arial, sans-serif" font-size="10" fill="#666" transform="rotate(-90 20 95)">External</text>
  <text x="20" y="300" font-family="Arial, sans-serif" font-size="10" fill="#666" transform="rotate(-90 20 300)">GraphQL Layer</text>
  <text x="20" y="500" font-family="Arial, sans-serif" font-size="10" fill="#666" transform="rotate(-90 20 500)">Core System</text>
  
</svg>
```

</div>

### 🎯 Architecture Flow

**Request Journey**: Client → Security → Schema → APIs → Data Processing → dotCMS Core

| Layer | Purpose | Key Components |
|-------|---------|----------------|
| 🌐 **Clients** | External applications | SDKs, Browsers, Mobile Apps |
| 🚪 **Endpoint** | Single API entry point | `/api/v1/graphql` with HTTP handling |
| 🔐 **Security** | Auth & dynamic schema | JWT, permissions, user-aware schemas |
| 🔧 **APIs** | GraphQL operations | Content, Page, Navigation, System APIs |
| ⚙️ **Processing** | Data resolution | Fetchers, type providers, caching |
| 💽 **Core** | dotCMS foundation | Content types, pages, users, permissions |

### 🎯 Key Features

| Component | Purpose | Examples |
|-----------|---------|----------|
| **🚪 Single Endpoint** | Unified API access | `/api/v1/graphql` for all operations |
| **🔐 Security First** | Permission-aware responses | Field-level access control, user context |
| **📋 Dynamic Schema** | Real-time schema generation | Based on content types and user permissions |
| **📄 Content API** | Query any content type | `BlogCollection`, `ProductCollection` |
| **🏗️ Page API** | Full page rendering | Templates, layouts, containers, HTML output |
| **🧭 Navigation** | Site structure | `DotNavigation` with hierarchical data |
| **💾 Enterprise Caching** | High-performance caching | Query results, schema caching with TTL |

### 🔄 Request Flow

1. **Client** sends GraphQL query to `/api/v1/graphql`
2. **Security Layer** authenticates user and checks permissions
3. **Schema Provider** generates user-specific schema
4. **GraphQL Engine** routes query to appropriate API
5. **Data Fetchers** resolve fields with security checks
6. **Cache Layer** stores/retrieves results for performance
7. **dotCMS Core** provides the underlying data

## Overview

The dotCMS GraphQL API enables developers to query content, pages, and other dotCMS resources using GraphQL. It supports both content delivery and page rendering functionality with built-in caching, security, and extensibility features.

## Key Features

- **Content Delivery API**: Query content types, relationships, and metadata
- **Page API**: Retrieve page data, layouts, containers, and rendered HTML
- **Navigation API**: Access site navigation structures
- **Caching**: Enterprise-level query result caching with TTL support
- **Security**: User-aware schema generation with permission-based field visibility
- **Extensibility**: Plugin architecture for custom types and fields
- **Real-time**: Support for live/working content modes and time machine queries

## Architecture

### Core Components

- **GraphQL Schema Provider**: Generates user-aware schemas with proper field visibility
- **Data Fetchers**: Handle field resolution and data retrieval
- **Type Providers**: Generate GraphQL types for content types and system objects
- **Cache Layer**: Configurable caching for query results and schema generation
- **Security Layer**: Permission-aware data access and schema introspection control

### Schema Generation

The API dynamically generates GraphQL schemas based on:
- dotCMS content types and their fields
- Base content types (Page, File, Widget, etc.)
- Custom field types (Binary, Category, Key-Value, etc.)
- User permissions and license level

## Getting Started

### Basic Query Structure

```graphql
query {
  # Search all content
  search(query: "+contentType:Blog", limit: 10) {
    title
    identifier
    modDate
  }
  
  # Query specific content type collection
  BlogCollection(limit: 5) {
    title
    body
    author
  }
  
  # Get page data
  page(url: "/about-us") {
    title
    template {
      title
      layout {
        title
        body {
          rows {
            columns {
              containers {
                identifier
                rendered {
                  uuid
                  render
                }
              }
            }
          }
        }
      }
    }
  }
}
```

### Available Endpoints

- `/api/v1/graphql` - Main GraphQL endpoint
- Supports both GET (with `qid` parameter) and POST requests
- CORS configuration via `api.cors.graphql.*` properties

## Content Types and Fields

### Standard Content Fields

All content types inherit these base fields:
- `identifier`, `inode` - Unique identifiers
- `title`, `modDate`, `creationDate` - Basic metadata
- `live`, `working`, `archived`, `locked` - Status flags
- `conLanguage`, `host`, `folder` - Context information
- `owner`, `modUser` - User references

### Custom Field Types

The API supports various custom field types:

- **Binary**: File and image data with metadata
- **Category**: Hierarchical categorization
- **Site/Folder**: Site and folder references
- **Key-Value**: Structured key-value pairs
- **Language**: Language information
- **User**: User profile data
- **Story Block**: Rich text content blocks

### Field Arguments

Most fields support these arguments:
- `render: Boolean` - Whether to velocity-render the field content
- `query: String` - Filter related content (for relationship fields)
- `limit: Int` - Limit results (for collections and relationships)
- `offset: Int` - Pagination offset
- `sortBy: String` - Sort criteria

## Page API Features

### Page Data Structure

```graphql
type DotPage {
  # Standard content fields
  title: String
  url: String
  
  # Page-specific fields
  template: DotPageTemplate
  layout: DotPageLayout
  containers: [DotPageContainer]
  render: String  # Rendered HTML
  
  # Context information
  viewAs: DotPageViewAs
  vanityUrl: DotPageVanityURL
  runningExperimentId: String
}
```

### Template and Layout

- **Template**: Theme and design information
- **Layout**: Responsive grid structure with rows and columns
- **Containers**: Content areas with rendered output
- **Sidebar**: Optional sidebar configuration

### Page Arguments

```graphql
page(
  url: String!           # Page URL (required)
  pageMode: String       # LIVE, WORKING, PREVIEW, EDIT_MODE
  languageId: String     # Language identifier
  persona: String        # Persona for personalization
  fireRules: Boolean     # Whether to execute page rules
  site: String          # Site context
  publishDate: String   # Time machine date
  variantName: String   # A/B test variant
)
```

## Caching

### Query Result Caching (Enterprise)

Configure via properties:
```properties
GRAPHQL_CACHE_RESULTS=true
cache.graphqlquerycache.seconds=300
cache.graphqlquerycache.graceperiod=60
```

### Cache Control Headers

- `dotcachettl: Int` - Cache TTL in seconds (0=bypass, -1=invalidate)
- `dotcachekey: String` - Custom cache key
- `dotcacherefresh: Boolean` - Background refresh

### Schema Caching

- Automatic schema invalidation on content type changes
- Separate caching for anonymous vs authenticated users
- Debounced regeneration to prevent overload

## Security

### Permission-Based Access

- Field-level permission checking
- Anonymous users receive limited schema (no introspection)
- Content filtering based on user permissions
- Secure handling of sensitive fields

### Authentication

Supports multiple authentication methods:
- Session-based authentication
- JWT token authentication
- Anonymous access (with restrictions)

## Naming Conventions for GraphQL Type Providers

When creating new GraphQL type providers, follow these strict naming conventions to ensure compatibility:

### Field and Type Names

All GraphQL types and fields must conform to the regex pattern: `[_A-Za-z][_0-9A-Za-z]*`

**Valid Examples:**
- `BlogPost`
- `user_profile`
- `Product2024`
- `_internal_field`

**Invalid Examples:**
- `2024Product` (starts with number)
- `blog-post` (contains hyphen)
- `user profile` (contains space)
- `product@home` (contains special character)

### Content Type Variables

Content types must have variables that match the naming pattern:
```java
// Validation in ContentAPIGraphQLTypesProvider
DotPreconditions.checkArgument(contentType.variable()
    .matches(TYPES_AND_FIELDS_VALID_NAME_REGEX),
    "Content Type variable does not conform to naming rules");
```

### Collection Field Naming

Collections automatically follow specific patterns based on their type:

#### Content Type Collections
Content type collections use the pattern: `{ContentTypeVariable}Collection`
- Content type `Blog` → `BlogCollection` field
- Content type `Product` → `ProductCollection` field
- Content type `Event` → `EventCollection` field

#### Base Type Collections
Base type collections use the pattern: `{BaseTypeName}Collection`
- Base type `PageBaseType` → `PageBaseTypeCollection` field
- Base type `FileBaseType` → `FileBaseTypeCollection` field

#### System Collections (Non-Content Type)
**CRITICAL NAMING RULE**: All system collections that are not derived from content types **MUST** use the `Dot` prefix to prevent collisions with customer content types.

**Examples:**
- ✅ `DotNavigation` - Correct (prevents collision with potential "Navigation" content type)
- ✅ `DotMetrics` - Correct (prevents collision with potential "Metrics" content type)
- ✅ `DotAnalytics` - Correct (prevents collision with potential "Analytics" content type)
- ❌ `Navigation` - **WRONG** (could collide with customer's "Navigation" content type)
- ❌ `Search` - **WRONG** (could collide with customer's "Search" content type)

**Implementation Example:**
```java
// In NavigationFieldProvider
public static final String DOT_NAVIGATION = "DotNavigation";

@Override
public Collection<GraphQLFieldDefinition> getFields() throws DotDataException {
    return Set.of(newFieldDefinition()
            .name(DOT_NAVIGATION)  // Uses "DotNavigation", not "Navigation"
            .type(outputType)
            .dataFetcher(new NavigationDataFetcher()).build());
}
```

**Rationale**: Customer environments may have content types with names like "Navigation", "Search", "Analytics", etc. By prefixing system collections with "Dot", we ensure no naming conflicts occur when the GraphQL schema is generated.

### Custom Type Provider Implementation

When implementing new type providers:

1. **Implement GraphQLTypesProvider interface**:
```java
public enum MyCustomTypeProvider implements GraphQLTypesProvider {
    INSTANCE;
    
    @Override
    public Collection<? extends GraphQLType> getTypes() throws DotDataException {
        // Ensure all type names follow naming convention
        // Use "Dot" prefix for system types to avoid customer content type collisions
        return customTypes;
    }
}
```

2. **Register with GraphqlAPIImpl**:
```java
// Add to constructor
typesProviders.add(MyCustomTypeProvider.INSTANCE);
```

3. **Validate field names**:
```java
// For custom fields, validate names before adding
if (!fieldName.matches(TYPES_AND_FIELDS_VALID_NAME_REGEX)) {
    Logger.warn("Invalid field name: " + fieldName);
    return; // Skip invalid fields
}
```

### Field Compatibility

When adding fields to existing types, ensure compatibility:
```java
// Check if field variable is compatible with inherited fields
public boolean isFieldVariableGraphQLCompatible(String variable, Field field) {
    // Implementation checks against ContentFields.getContentFields()
    // Ensures no conflicts with base content type fields
}
```

### Best Practices

1. **Use PascalCase** for type names: `BlogPost`, `UserProfile`
2. **Use camelCase** for field names: `firstName`, `publishDate`
3. **Use "Dot" prefix** for system collections: `DotNavigation`, `DotMetrics`
4. **Avoid reserved words**: Don't use GraphQL or Java reserved keywords
5. **Be descriptive**: Use clear, meaningful names
6. **Maintain consistency**: Follow existing patterns in the codebase
7. **Check for conflicts**: Always consider potential customer content type names

### Collision Prevention Strategy

The naming convention prevents these potential conflicts:

| System Feature | Correct Name | Potential Collision | Customer Content Type |
|----------------|--------------|---------------------|----------------------|
| Navigation API | `DotNavigation` | ❌ `Navigation` | Customer's "Navigation" content type |
| Search API | `DotSearch` | ❌ `Search` | Customer's "Search" content type |
| Analytics | `DotAnalytics` | ❌ `Analytics` | Customer's "Analytics" content type |
| Metrics | `DotMetrics` | ❌ `Metrics` | Customer's "Metrics" content type |

## Configuration

### Key Properties

```properties
# Enable/disable GraphQL caching
GRAPHQL_CACHE_RESULTS=true

# Schema generation
GRAPHQL_PRINT_SCHEMA=false
GRAPHQL_SCHEMA_DEBOUNCE_DELAY_MILLIS=5000
GRAPHQL_REMOVE_DUPLICATED_TYPES=false

# Cache settings
cache.graphqlquerycache.seconds=15
cache.graphqlquerycache.graceperiod=60

# CORS configuration
api.cors.graphql.access-control-allow-origin=*
api.cors.graphql.access-control-allow-methods=GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH
```

## Advanced Features

### Special Fields

- `_map`: Access contentlet properties with custom arguments
- `widgetCodeJSON`: Parsed widget code for dynamic widgets
- `render`: Get rendered HTML for pages
- Relationship fields with query filtering

### Metadata and Pagination

```graphql
query {
  BlogCollection(limit: 10) {
    title
    body
  }
  
  # Get result metadata
  QueryMetadata {
    fieldName
    totalCount
  }
  
  # Get pagination info
  Pagination {
    fieldName
    totalPages
    currentPage
    hasNextPage
  }
}
```

### Navigation API

```graphql
query {
  DotNavigation(
    uri: "/"
    depth: 2
    languageId: 1
  ) {
    title
    href
    order
    children {
      title
      href
    }
  }
}
```

## Error Handling

The API provides structured error responses:
- **ValidationError**: Invalid input or language
- **PermissionDenied**: Access denied
- **ResourceNotFound**: Content not found
- **DataFetchingException**: General data retrieval errors

## Development

### Extending the API

1. **Custom Type Providers**: Implement `GraphQLTypesProvider`
2. **Custom Field Providers**: Implement `GraphQLFieldsProvider`
3. **Custom Data Fetchers**: Extend `DataFetcher<T>`
4. **Custom Field Generators**: Implement `GraphQLFieldGenerator`

### Testing

- Unit tests for data fetchers and type providers
- Integration tests for schema generation
- Performance tests for caching behavior

For more information, visit the [dotCMS documentation](https://www.dotcms.com/docs/).