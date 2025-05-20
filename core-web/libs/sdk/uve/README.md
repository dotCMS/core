# dotCMS UVE SDK

A JavaScript library to connect your dotCMS pages with the Universal Visual Editor (UVE) and enable content authors to edit pages in real time.

The UVE SDK is designed as a flexible, high-level library that provides core functionality for custom implementations across any JavaScript project. While you can use this SDK directly in any environment, we also offer framework-specific libraries for popular JavaScript frameworks like Angular and React. These specialized libraries build upon the UVE SDK to provide optimized, streamlined integration with their respective frameworks, reducing development time and ensuring best practices.

Whether you're building a custom solution or working with a specific framework, the UVE SDK provides a robust foundation for creating editable pages with dotCMS.

## Table of Contents

- [Installation](#installation)
- [Examples](#examples)
- [Browser Compatibility](#browser-compatibility)
- [API Reference](#api-reference)
  - [1. Editor Initialization](#1-editor-initialization)
  - [2. Core UVE Functionality](#2-core-uve-functionality)
  - [3. Content Editing](#3-content-editing)
  - [4. Navigation & Layout](#4-navigation--layout)
  - [5. Communication Utilities](#5-communication-utilities)
- [Contributing](#contributing)
- [Licensing](#licensing)

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

## Examples

To help you get started with the UVE SDK, we provide several example projects demonstrating integration with different frameworks:

- [Angular Example](https://github.com/dotCMS/core/tree/main/examples/angular) - Integration with Angular
- [Next.js Example](https://github.com/dotCMS/core/tree/main/examples/nextjs) - Integration with Next.js
- [Astro Example](https://github.com/dotCMS/core/tree/main/examples/astro) - Integration with Astro

These examples show how to initialize the UVE, handle content editing, and implement navigation synchronization within each framework's specific patterns and lifecycle methods.

## Browser Compatibility

The DotCMS UVE SDK is compatible with:

| Browser | Supported Versions |
|---------|-------------------|
| Chrome  | Latest 3 versions |
| Firefox | Latest 3 versions |
| Edge    | Latest 3 versions |

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
      variables: Record<string, string>;
    };
    params?: Record<string, PageAPIParams>;
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
const { destroyUVESubscriptions } = initUVE({ params });

// Later, when you're done with UVE (e.g., component unmount)
destroyUVESubscriptions();
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

### 2. Core UVE Functionality

#### `getUVEState()`

**Description:**  
Gets the current state of the Universal Visual Editor (UVE). This function checks if the code is running inside the DotCMS Universal Visual Editor and returns information about its current state, including the editor mode.

**Parameters:**
- None

**Returns:**
- `UVEState | undefined` - Returns the UVE state object if running inside the editor, undefined otherwise.

The UVE state includes:
- `mode`: The current editor mode (`'preview'`, `'edit'`, `'live'`)
- `languageId`: The language ID of the current page set on the UVE
- `persona`: The persona of the current page set on the UVE
- `variantName`: The name of the current variant
- `experimentId`: The ID of the current experiment
- `publishDate`: The publish date of the current page set on the UVE
- `dotCMSHost`: The host URL of the DotCMS instance

**Example:**

```typescript
import { getUVEState } from '@dotcms/sdk/uve';

// Check if running in edit mode
const editorState = getUVEState();
if (editorState?.mode === 'edit') {
  // Enable editing features
  enableEditableRegions();
}

// Access other state properties
if (editorState?.languageId) {
  console.log(`Current language: ${editorState.languageId}`);
}
```

#### `createUVESubscription(eventType, callback)`

**Description:**  
Creates a subscription to a UVE event. Use this to listen for and respond to various events triggered by the Universal Visual Editor.

**Parameters:**
- `eventType`: `UVEEventType` - The type of event to subscribe to
- `callback`: `Function` - The callback function that will be called when the event occurs

**Returns:**
- `UVEEventSubscription` - An event subscription object with an `unsubscribe()` method

**Example:**

```typescript
import { createUVESubscription, UVEEventType } from '@dotcms/sdk/uve';

// Subscribe to content changes
const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
  console.log('Content changes detected:', changes);
  
  // Update your component/page based on the changes
  if (changes.contentlets) {
    updateContentlets(changes.contentlets);
  }
});

// Later, unsubscribe when no longer needed
subscription.unsubscribe();
```

**Usage in React component:**

```tsx
import { useEffect, useState } from 'react';
import { createUVESubscription, UVEEventType, initUVE } from '@dotcms/uve';
import { DotCMSPageResponse } from '@dotcms/types';  

/**
 * Custom hook for UVE page editing functionality
 * @param config Optional UVE configuration
 */
const useCustomEditablePage = (config?: DotCMSUVEConfig, page?: DotCMSPageResponse) => {
  const [pageData, setPageData] = useState(page);

  useEffect(() => {
    const { destroyUVESubscriptions } = initUVE(config);
    const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (page: DotCMSPageResponse) => {
      setPageData(page);
    });

    return () => {
      destroyUVESubscriptions();
      subscription.unsubscribe();
    };
  }, [config]);

  return { pageData };
};
```

### 3. Content Editing

#### `editContentlet(contentlet)`

**Description:**  
Opens the contentlet edit dialog in the Universal Visual Editor. Use this function to allow content editors to modify contentlets directly from your application.

This function is particularly useful for editing contentlets that don't belong to the main page response. For example, if your application fetches the page content but also separately fetches content for global elements like headers, footers, or sidebars, you can make these external contentlets editable using this function.

**Parameters:**
- `contentlet`: `Contentlet<T>` - The contentlet object to edit

**Returns:**
- None

**Example:**

```typescript
import { editContentlet } from '@dotcms/uve';

// Edit a contentlet when a button is clicked
function handleEditClick(contentlet) {
  editContentlet(contentlet);
}

// Usage in a component
<button onClick={() => handleEditClick(myContentlet)}>
  Edit Content
</button>
```

#### `initInlineEditing(type, data)`

**Description:**  
Initializes inline editing for content in the editor. This function is used to enable various types of inline editing experiences in the Universal Visual Editor.

**Parameters:**
- `type`: `DotCMSInlineEditingType` - The type of inline editing to initialize
  - `'BLOCK_EDITOR'` - For Block Editor field types in dotCMS
  - `'WYSIWYG'` - For WYSIWYG Editor field types in dotCMS
- `data`: `DotCMSInlineEditingPayload` - Data required for the inline editing

**Returns:**
- None

**Example:**

```typescript
import { initInlineEditing } from '@dotcms/uve';
import { DotCMSInlineEditingType } from '@dotcms/types';

// Initialize inline editing for a content element
function enableInlineEditing(elementData) {
  initInlineEditing(DotCMSInlineEditingType.WYSIWYG, {
    inode: elementData.inode,
    languageId: elementData.languageId,
    contentType: elementData.contentType,
    fieldName: 'bodyText',
    content: elementData.bodyText
  });
}

// HTML usage example
<div 
  onClick={() => enableInlineEditing(contentletData)}
  dangerouslySetInnerHTML={{ __html: contentletData.bodyText }}
></div>
```

#### `enableBlockEditorInline(contentlet, fieldName)`

**Description:**  
Initializes the block editor inline editing for a specific contentlet field. This is a specialized helper function that simplifies the process of enabling block editor inline editing.

This function has the same behavior as `initInlineEditing` but is specifically focused on Block Editor fields. It's essentially a convenience wrapper around `initInlineEditing` that automatically sets the type to 'BLOCK_EDITOR' and constructs the required payload from the contentlet and fieldName parameters.

**Parameters:**
- `contentlet`: `DotCMSBasicContentlet` - The contentlet object containing the field to edit
- `fieldName`: `string` - The name of the field to edit with the block editor

**Returns:**
- None

**Example:**

```typescript
import { enableBlockEditorInline } from '@dotcms/uve';

// Enable block editor inline editing for a content field
function handleBlockEditorClick(contentlet) {
  enableBlockEditorInline(contentlet, 'blockContent');
}

// Usage in a component
<div 
  onClick={() => handleBlockEditorClick(contentlet)}
  dangerouslySetInnerHTML={{ __html: contentlet.blockContent }}
></div>
```

**React Component Example:**

```tsx
import React from 'react';
import { enableBlockEditorInline } from '@dotcms/uve';

interface ContentBlockProps {
  contentlet: {
    inode: string;
    languageId: string;
    contentType: string;
    blockContent: string;
  };
}

const ContentBlock: React.FC<ContentBlockProps> = ({ contentlet }) => {
  const handleClick = () => {
    enableBlockEditorInline(contentlet, 'blockContent');
  };

  return (
    <div 
      className="editable-block"
      onClick={handleClick}
      dangerouslySetInnerHTML={{ __html: contentlet.blockContent }}
    />
  );
};
```

### 4. Navigation & Layout

#### `updateNavigation(pathname)`

**Description:**  
Updates the navigation in the Universal Visual Editor. This function allows you to synchronize the UVE's internal navigation state with your application's routing, ensuring that editing tools and context remain consistent when navigation occurs within your single-page application.

**Parameters:**
- `pathname`: `string` - The pathname to update the navigation with

**Returns:**
- None

**Example:**

```typescript
import { updateNavigation } from '@dotcms/uve';

// Update navigation when route changes
function handleRouteChange(newRoute) {
  updateNavigation(newRoute);
}

// Usage with React Router
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

function NavigationSynchronizer() {
  const location = useLocation();
  
  useEffect(() => {
    // Keep UVE navigation in sync with React Router
    updateNavigation(location.pathname);
  }, [location.pathname]);
  
  return null; // This component doesn't render anything
}
```

#### `reorderMenu(config)`

**Description:**  
Reorders the menu based on the provided configuration. This function allows content editors to reorganize the site navigation structure directly within the Universal Visual Editor.

**Parameters:**
- `config`: `DotCMSReorderMenuConfig` (optional) - Configuration for reordering the menu
  - `startLevel`: `number` - The starting level of the menu to reorder (default: 1)
  - `depth`: `number` - The depth of the menu to reorder (default: 2)

**Returns:**
- None

**Example:**

```typescript
import { reorderMenu } from '@dotcms/uve';

// Basic usage with default options
function handleReorderMenuClick() {
  reorderMenu();
}

// Usage with custom configuration
function handleReorderClick() {
  reorderMenu({
    startLevel: 2,  // Start at second level of navigation
    depth: 3        // Go three levels deep
  });
}

// In a React component
function MenuEditorButton() {
  return (
    <button onClick={() => reorderMenu()}>
      Reorder Navigation Menu
    </button>
  );
}
```

### 5. Communication Utilities

#### `sendMessageToUVE(message)`

**Description:**  
Posts a message to the DotCMS Universal Visual Editor. This is a low-level communication function that allows you to send custom messages directly to the UVE. Most developers won't need to use this function directly as the other API functions (like `editContentlet`, `updateNavigation`, etc.) use it internally.

**Parameters:**
- `message`: `DotCMSUVEMessage<T>` - The message to send to the UVE
  ```typescript
  interface DotCMSUVEMessage<T = unknown> {
    action: DotCMSUVEAction;
    payload: T;
  }
  ```

**Returns:**
- None

**Example:**

```typescript
import { sendMessageToUVE } from '@dotcms/uve';
import { DotCMSUVEAction } from '@dotcms/types';

// Send a custom message to the UVE
function sendCustomMessage(data) {
  sendMessageToUVE({
    action: DotCMSUVEAction.CUSTOM_ACTION,
    payload: data
  });
}

// Advanced usage for custom integration
function notifyUVEOfCustomEvent(eventType, eventData) {
  sendMessageToUVE({
    action: DotCMSUVEAction.CUSTOM_EVENT,
    payload: {
      type: eventType,
      data: eventData
    }
  });
}
```

**Note:** This is an advanced utility function primarily intended for internal use or creating custom integrations with the Universal Visual Editor. Most use cases are better served by the higher-level functions documented in previous sections.

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).