# DotCMS UVE SDK

A JavaScript library to connect your dotCMS pages with the Universal Visual Editor (UVE) and enable content authors to edit pages in real time.

## Installation

The UVE SDK is automatically included in DotCMS installations. For external usage:

```bash
# Using npm
npm install @dotcms/uve-sdk

# Using yarn
yarn add @dotcms/uve-sdk

# Using pnpm
pnpm add @dotcms/uve-sdk
```

## API Reference

### 1. Editor Initialization

#### `initUVE(config?: DotCMSUVEConfig)`

**Description:**  
Initializes the Universal Visual Editor (UVE) with required handlers and event listeners. This is the first function that should be called before using any other UVE functionality, as it establishes the connection between your application and the editor.

**Parameters:**
- `config` (optional): `DotCMSUVEConfig` - Configuration options for the UVE initialization
  ```typescript
  interface DotCMSUVEConfig {
    graphql?: {
      query: string;
      variables: Record<string, unknown>;
    };
    params?: Record<string, unknown>;
  }
  ```

  The `params` object can include the following optional properties:
  - `languageId`: The ID of the language variant to retrieve
  - `personaId`: The ID of the persona variant to retrieve
  - `siteId`: The ID of the site
  - `mode`: The content mode to use
  - `fireRules`: Boolean value indicating whether to fire rules set on the page
  - `depth`: Value between 0-3 to control access to related content

  For more details on these parameters, see the [Page REST API documentation](https://dev.dotcms.com/docs/page-rest-api-layout-as-a-service-laas#Parameters).

**Returns:**
- Object containing:
  - `destroyUVESubscriptions`: Function to clean up all UVE event subscriptions and handlers

**What it does:**
- Sets up empty contentlet styling
- Notifies the editor that the client is ready
- Registers UVE event subscriptions
- Sets up scroll handling
- Configures block editor inline event listening

**Example:**

```typescript
// Basic initialization
const { destroyUVESubscriptions } = initUVE();

// Later, when you're done with UVE (e.g., component unmount)
destroyUVESubscriptions();

// With configuration (optional)
const { destroyUVESubscriptions } = initUVE({
  // Any UVE-specific configuration options
});
```

**Usage in React component:**

```tsx
import { useEffect } from 'react';
import { initUVE } from '@dotcms/sdk/uve';

function MyUVEComponent(dotCMSUVEConfig?: DotCMSUVEConfig) {
  useEffect(() => {
    // Initialize UVE when component mounts
    const { destroyUVESubscriptions } = initUVE(dotCMSUVEConfig);
    
    // Clean up when component unmounts
    return () => {
      destroyUVESubscriptions();
    };
  }, []);
  
  return (
    <div>
      {/* Your UVE-enabled component content */}
    </div>
  );
}
```

