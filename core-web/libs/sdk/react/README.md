# dotCMS React SDK

`@dotcms/react` is the official React integration library for dotCMS, designed to empower React developers to build powerful, editable websites and applications with minimal effort.


## What is it?

This SDK bridges the gap between React's component-based architecture and dotCMS's powerful content management capabilities. It provides a suite of ready-to-use components and hooks that enable you to:

- **Build editable pages** that work seamlessly with dotCMS's Universal Visual Editor (UVE)
- **Render dynamic content** from your dotCMS instance with minimal boilerplate code
- **Create interactive editing experiences** that content editors will love
- **Focus on your application logic** rather than API integration details

Whether you're building a simple marketing site or a complex web application, the dotCMS React SDK streamlines your development process, allowing you to launch faster while maintaining full editability within dotCMS.

> **See it in action:** Check out our [live demo of a Next.js site](https://nextjs-example-sigma-five.vercel.app/) built with the dotCMS React SDK.


## Getting Started

The quickest way to start using the dotCMS React SDK is to explore our [Next.js example project](https://github.com/dotCMS/core/tree/main/examples/nextjs). This repository includes:

* Complete working example with React and Next.js
* Step-by-step instructions for setup and configuration
* Practical implementations of all core components
* Best practices for integrating with dotCMS

You can setup a NextJS project with this starter project by running the following command:

```bash
# Using npm
npx create-next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs

# Using Yarn
yarn create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs

# Using pnpm
pnpm create next-app dotcms-nextjs-demo --example https://github.com/dotCMS/core/tree/main/examples/nextjs
```


## Table of Contents

- [What is it?](#what-is-it)
- [Getting Started](#getting-started)
- [How To Install](#how-to-install)
- [Dependencies](#dependencies)
- [Browser Compatibility](#browser-compatibility)
- [API Reference](#detailed-api-documentation)
  - [Components](#components)
    - [DotCMSLayoutBody](#dotcmslayoutbody)
    - [DotCMSShow](#dotcmsshow)
    - [DotCMSBlockEditorRenderer](#dotcmsblockeditoenderer)
    - [DotCMSEditableText](#dotcmseditabletext)
  - [Hooks](#hooks)
    - [useDotCMSShowWhen](#usedotcmsshowwhen)
    - [useEditableDotCMSPage](#useeditabledotcmspage)
- [Additional Resources](#additional-resources)
- [FAQ](#faq)
- [dotCMS Support](#dotcms-support)
- [How To Contribute](#how-to-contribute)
- [Licensing Information](#licensing-information)


## How To Install

The React SDK is automatically included in DotCMS installations. For external usage:

```bash
# Using npm
npm install @dotcms/react@next

# Using yarn
yarn add @dotcms/react@next

# Using pnpm
pnpm add @dotcms/react@next
```


## Dependencies

This package has the following peer dependencies that you'll need to install in your project:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/uve` | latest | Required for page editing functionality |
| `@dotcms/client` | latest | Required for page fetching functionality |
| `@dotcms/types` | latest | Required for type definitions |
| `@tinymce/tinymce-react` | ^6.0.0 | Required for TinyMCE integration |

Install peer dependencies:

```bash
# Using npm
npm install @dotcms/uve@next @dotcms/client@next @dotcms/types @tinymce/tinymce-react

# Using yarn
yarn add @dotcms/uve@next @dotcms/client@next @dotcms/types @tinymce/tinymce-react

# Using pnpm
pnpm add @dotcms/uve@next @dotcms/client@next @dotcms/types @tinymce/tinymce-react
```


## Browser Compatibility

The `@dotcms/react` package is compatible with the following browsers:

| Browser | Minimum Version | TLS Version |
|---------|----------------|-------------|
| Chrome  | Latest 2 versions | TLS 1.2+ |
| Edge    | Latest 2 versions | TLS 1.2+ |
| Firefox | Latest 2 versions | TLS 1.2+ |


## API Reference

This section provides detailed documentation for all components, hooks, and types in the @dotcms/react Next API.


### Components

#### `DotCMSLayoutBody`

The `DotCMSLayoutBody` component renders the layout body for a DotCMS page. It utilizes the dotCMS page asset's layout body to render the page structure with rows and columns.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `page` | `DotCMSPageAsset` | Yes | --- | The DotCMS page asset containing the layout information. |
| `components` | `Record<string, React.ComponentType>` | Yes | --- | A mapping of custom components for content rendering. Keys should match content types in dotCMS. |
| `mode` | `string` | No | `'production'` | The renderer mode. Can be either `'production'` or `'development'`. |

> **Note:** Using `'development'` mode enhances troubleshooting by showing missing components and empty containers in your page. This helps identify issues with page composition. When your page is opened in the dotCMS editor, the development mode is automatically applied regardless of what you've explicitly set.

##### Implementation Details

The `DotCMSLayoutBody` component works by parsing the dotCMS page asset structure, which contains:

- **Layout information**: Rows, columns, and their sizing/positioning
- **Container references**: Identifiers that link to content containers
- **Content mapping**: How containers map to actual content

The component renders the page in this order:
1. Parses the layout body from the page asset
2. Renders each row in the layout
3. Renders columns within each row according to specified widths
4. Identifies containers within each column
5. Maps content from the page's contentlets to these containers
6. Renders each content item using the appropriate component from your `components` prop

When a container has no matching component for a content type, the component will:
- In `development` mode: Show a warning message with the missing content type
- In `production` mode: Silently skip rendering that content

##### Integration with dotCMS Page Editor

When used with the dotCMS Universal Visual Editor (UVE):

1. The component automatically detects when it's being rendered inside the editor
2. It adds necessary markup for the editor to identify editable regions
3. When content is edited in the UVE, changes are reflected in real-time
4. The editor can rearrange the layout structure, and the component will adapt accordingly

For this integration to work correctly, make sure to:
- Use the `useEditableDotCMSPage` hook to make the page editable
- Register all content types used in your page in the `components` prop
- Avoid manually modifying the layout structure received from the API

##### Basic Usage

```jsx
import { DotCMSLayoutBody } from '@dotcms/react/next';

// Your custom components mapped to content types
const components = {
  'Blog': BlogComponent,
  'Product': ProductComponent,
  // Add more components as needed
};

const MyPage = ({ pageData }) => {
  return (
    <DotCMSLayoutBody 
      page={pageData} 
      components={components} 
    />
  );
};

export default MyPage;
```

##### Advanced Usage

```jsx
import { useState, useEffect } from 'react';
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/react/next';
import { createDotCMSClient } from '@dotcms/client';

// Dynamically register components based on content type
const getComponents = () => {
  // Core components
  const baseComponents = {
    'Blog': BlogComponent,
    'Product': ProductComponent,
    'Banner': BannerComponent
  };
  
  // Conditionally add components
  if (process.env.FEATURE_EVENTS_ENABLED) {
    return {
      ...baseComponents,
      'CalendarEvent': EventComponent,
      'EventRegistration': EventRegistrationComponent
    };
  }
  
  return baseComponents;
};

// Custom wrapper component for specific content type
const ProductWithAnalytics = (props) => {
  useEffect(() => {
    // Track product impressions
    analytics.trackImpression(props.identifier);
  }, [props.identifier]);
  
  return <ProductComponent {...props} />;
};

const MyAdvancedPage = ({ initialPageData }) => {
  // Register components with special handling for certain content types
  const [components] = useState(() => ({
    ...getComponents(),
    // Override standard Product component with analytics-enhanced version
    'Product': ProductWithAnalytics
  }));
  
  // Make the page editable with UVE
  const editablePage = useEditableDotCMSPage(initialPageData);
  
  return (
    <div className="page-wrapper">
      <DotCMSLayoutBody 
        page={editablePage.pageAsset} 
        components={components}
        mode={process.env.NODE_ENV}
      />
    </div>
  );
};

export default MyAdvancedPage;
```

> **Note:** For a more detailed explanation of how to build React applications with dotCMS, see the [developer tutorial on using dotCMS with React](https://www.dotcms.com/blog/developer-tutorial-how-to-use-dotcms-and-react-to-build-single-page-apps).

##### Common Pitfalls and Limitations

1. **Missing Content Type Components**
   - If your `components` object doesn't have a mapping for a content type used in the page, that content won't render.
   - Always ensure all content types used in your pages have corresponding components registered.

2. **Layout Structure Modifications**
   - Modifying the layout structure directly may break the editor's ability to track changes.
   - Always use the layout data as provided by the API without structural modifications.

3. **Performance Considerations**
   - For pages with many content items, consider implementing component lazy loading.
   - Avoid expensive operations in content type components as they may render multiple times.

4. **Sidebar Rendering**
   - The sidebar content requires special handling if using custom grid systems.
   - Sidebar location ('left', 'right') and width ('small', 'medium', 'large') should be translated to your CSS framework.


#### `DotCMSShow`

The `DotCMSShow` component conditionally renders its children based on the Universal Visual Editor (UVE) mode. This allows you to create UI elements that only appear in specific contexts, such as edit controls in the editor or special preview-only content.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `children` | `React.ReactNode` | Yes | --- | The content to be rendered when the condition is met. |
| `when` | `UVE_MODE` | No | `UVE_MODE.EDIT` | The UVE mode in which the children should be rendered. Can be `UVE_MODE.EDIT`, `UVE_MODE.PREVIEW`, or `UVE_MODE.LIVE`. |

##### UVE Mode Integration

The `DotCMSShow` component integrates directly with the dotCMS Universal Visual Editor's mode system to conditionally render content:

1. **EDIT Mode (`UVE_MODE.EDIT`)**
   - Content is visible only when editing a page within the dotCMS editor's draft mode
   - Perfect for edit controls, guidelines, and administrative UI elements
   - Appears only to content editors with appropriate permissions

2. **PREVIEW Mode (`UVE_MODE.PREVIEW`)**
   - Content is visible in the editor's preview mode
   - Useful for showing preview-specific messages or UI elements
   - Can be used to highlight content that is staged but not yet published

3. **LIVE Mode (`UVE_MODE.LIVE`)**
   - Content is visible in the editor's "Published View" mode
   - This mode lets editors see how content appears to end users
   - Note: This is NOT for content that should only appear on the actual published site

The component automatically detects the current UVE mode and handles the conditional rendering accordingly, making it easy to create context-specific UI without manual mode checking.

##### Usage

```jsx
import { DotCMSShow } from '@dotcms/react/next';
import { UVE_MODE } from '@dotcms/uve';
import { editContentlet } from '@dotcms/uve';

// This component creates an edit button for any contentlet, even if it doesn't belong to the current page
// Component that only appears in edit mode
export function EditButton({ contentlet }) {
  return (
    <DotCMSShow when={UVE_MODE.EDIT}>
      <button
        className="dotcms-edit-button"
        onClick={() => editContentlet(contentlet)}>
        Edit Contentlet
      </button>
    </DotCMSShow>
  );
}

// Mode-specific content for different viewing contexts
const ModeAwareContent = ({ article }) => {
  return (
    <div className="article-container">
      {/* Content visible in all modes */}
      <h2>{article.title}</h2>
      
      {/* Edit mode only: editing tools */}
      <DotCMSShow when={UVE_MODE.EDIT}>
        <div className="edit-tools">
          <EditButton contentlet={article} />
          <span className="edit-hint">Click to edit this article</span>
        </div>
      </DotCMSShow>
      
      {/* Preview mode only: status information */}
      <DotCMSShow when={UVE_MODE.PREVIEW}>
        <div className="preview-banner">
          {article.live ? 
            <span className="status published">Published</span> : 
            <span className="status draft">Draft - Not Yet Published</span>
          }
        </div>
      </DotCMSShow>
      
      {/* Live view mode only: simulation of published view */}
      <DotCMSShow when={UVE_MODE.LIVE}>
        <div className="live-view-indicator">
          <span>Viewing as published content</span>
        </div>
      </DotCMSShow>
      
      {/* Content visible in all modes */}
      <div className="article-body">{article.body}</div>
    </div>
  );
};
```

##### Common Pitfalls and Limitations

1. **UVE Mode Detection Timing**
   - The UVE mode is detected when the component mounts
   - Mode changes while the component is mounted will not trigger re-rendering
   - For dynamic mode response, use the `useDotCMSShowWhen` hook instead

2. **Nesting Considerations**
   - Deeply nested `DotCMSShow` components may impact performance
   - Consider consolidating mode-specific UI into dedicated components

3. **Content Editor Experience**
   - Be mindful of how conditional content affects the editing experience
   - Too many edit-only UI elements can make the page cluttered for editors

4. **Mode Clarification**
   - Remember that `UVE_MODE.LIVE` is just a view mode within the editor
   - To target the actual published site outside the editor, use additional rendering logic
   - For functionality that should only appear on the real published site (like analytics), use environment detection


#### `DotCMSBlockEditorRenderer`

The `DotCMSBlockEditorRenderer` component renders rich content created with the Block Editor Content Type in dotCMS. It handles various block types out of the box and supports custom renderers for advanced use cases.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `blocks` | `BlockEditorContent` | Yes | --- | The block editor content structure to render. |
| `customRenderers` | `CustomRenderer` | No | `{}` | Custom renderers for specific block types. |
| `className` | `string` | No | `''` | CSS class name to apply to the container. |
| `style` | `React.CSSProperties` | No | `{}` | Inline styles to apply to the container. |

##### Basic Usage

```jsx
import { DotCMSBlockEditorRenderer } from '@dotcms/react/next';

function ContentDisplay({ contentlet }) {
  return (
    <DotCMSBlockEditorRenderer 
      blocks={contentlet.blockEditorField}
      className="rich-text-content"
    />
  );
}
```

> **Note:** This component has a comprehensive dedicated documentation file with examples, custom renderers, and best practices. Please refer to the [component's README](https://github.com/dotCMS/core/tree/master/core-web/libs/sdk/react/src/lib/next/components/DotCMSBlockEditorRenderer) for full documentation.


#### `DotCMSEditableText`

The `DotCMSEditableText` component allows inline editing of text content pulled from dotCMS API using the TinyMCE editor. This component enables content editors to directly edit text without opening the full content editor dialog.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `contentlet` | `object` | Yes | --- | The contentlet object containing the field to be edited. |
| `fieldName` | `string` | Yes | --- | Name of the field in the contentlet that contains the text content. |
| `mode` | `string` | No | `'plain'` | Editor mode. Can be `'plain'` or `'full'`. |
| `format` | `string` | No | `'text'` | Content format. Can be `'text'` or `'html'`. |

##### Implementation Notes

This component can be used with multiple types of dotCMS fields:

- **Text fields**: Simple text input fields (`mode="plain"`, `format="text"`)
- **Text area fields**: Multi-line text fields (`mode="plain"`, `format="text"`)
- **WYSIWYG fields**: Rich text editor fields (`mode="full"`, `format="html"`)

The component behavior changes based on the field type:

1. In non-edit modes, it renders the field content as plain text or HTML
2. In edit mode, it transforms into an editable TinyMCE field when clicked
3. For WYSIWYG fields, it provides full formatting controls when `mode="full"` is specified

##### Integration with dotCMS Editor

When used within the dotCMS Universal Visual Editor (UVE):

1. The component automatically detects when it's in edit mode
2. It adds necessary markup for the editor to identify editable regions
3. When a user clicks on text rendered by this component:
   - The text becomes editable directly on the page
   - Changes are automatically saved when the user clicks away
   - No need to open the full content editor dialog
4. It handles multi-language content correctly, editing the content in the current language

This creates a seamless editing experience that feels more like editing a document than a database record.

##### Basic Usage

```jsx
import { DotCMSEditableText } from '@dotcms/react/next';

// Simple title field (plain text)
const MyContentletWithTitle = ({ contentlet }) => (
  <h2>
    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
  </h2>
);
```

##### Advanced Usage

```jsx
import { DotCMSEditableText } from '@dotcms/react/next';

// WYSIWYG content with full rich text editing 
const ArticleBody = ({ article }) => (
  <div className="article-content">
    <h1 className="article-title">
      <DotCMSEditableText 
        contentlet={article} 
        fieldName="title" 
      />
    </h1>
    
    <div className="article-body">
      <DotCMSEditableText 
        contentlet={article} 
        fieldName="body" 
        mode="full" 
        format="html" 
      />
    </div>
    
    <div className="article-footnote">
      <small>
        <DotCMSEditableText 
          contentlet={article} 
          fieldName="footnote"
        />
      </small>
    </div>
  </div>
);
```

##### Limitations

1. **Field Type Matching**: The `mode` and `format` props must match the actual field type in dotCMS
   - Using `mode="full"` with a plain text field will cause errors
   - Using `format="html"` with a non-WYSIWYG field may cause unpredictable results
   
2. **Incompatible with Block Editor Fields**: 
   - This component should NOT be used with Block Editor fields
   - For Block Editor content, use the `DotCMSBlockEditorRenderer` component instead
   
3. **Styling Considerations**: 
   - The component inherits styles from its parent elements
   - When in edit mode, some parent styles may be overridden by the editor

4. **Content Limits**:
   - Very large content fields may experience performance issues
   - For large WYSIWYG content, consider breaking it into smaller sections


### Hooks

#### `useDotCMSShowWhen`

The `useDotCMSShowWhen` hook provides the same core functionality as the `DotCMSShow` component but in hook form, allowing for more programmatic usage. It returns a boolean indicating if the current Universal Visual Editor (UVE) mode matches the specified mode.

##### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `when` | `UVE_MODE` | Yes | --- | The UVE mode to check against. Can be `UVE_MODE.EDIT`, `UVE_MODE.PREVIEW`, or `UVE_MODE.LIVE`. |

##### Implementation Details

Unlike the `DotCMSShow` component which only affects rendering, this hook:

- Returns a boolean value that can be used in any JavaScript expression
- Can be used inside event handlers, other hooks, or conditional logic
- Allows for more complex mode-based behavior beyond simple conditional rendering
- Works outside of the render tree, enabling programmatic responses to mode changes

The hook uses `getUVEState()` internally to check the current mode of the Universal Visual Editor.

##### Basic Usage

```jsx
import { useDotCMSShowWhen } from '@dotcms/react/next';
import { UVE_MODE } from '@dotcms/uve';

const MyConditionalComponent = ({ children }) => {
  // Check if we're in edit mode
  const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
  
  // Apply different styles or behaviors based on edit mode
  return (
    <div className={isEditMode ? 'edit-mode-container' : 'view-mode-container'}>
      {children}
      {isEditMode && (
        <div className="edit-controls">
          <span className="edit-hint">Edit content in this section</span>
        </div>
      )}
    </div>
  );
};
```

##### Advanced Usage

```jsx
import { useEffect } from 'react';
import { useDotCMSShowWhen } from '@dotcms/react/next';
import { UVE_MODE } from '@dotcms/uve';

const EnhancedContentViewer = ({ contentlet }) => {
  const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);
  const isLiveMode = useDotCMSShowWhen(UVE_MODE.LIVE);
  
  // Use the mode in event handlers
  const handleClick = () => {
    if (isEditMode) {
      // Show edit options
      openEditorPanel(contentlet);
    } else {
      // Show visitor interaction
      trackContentInteraction(contentlet.identifier);
    }
  };
  
  // Use with useEffect for side effects
  useEffect(() => {
    if (isLiveMode) {
      // In live view mode, simulate what users will see
      highlightNewContent(contentlet.inode);
    }
  }, [isLiveMode, contentlet]);
  
  return (
    <div 
      className={`content-viewer ${isEditMode ? 'editable' : ''}`}
      onClick={handleClick}
    >
      <h2>{contentlet.title}</h2>
      <div>{contentlet.body}</div>
    </div>
  );
};
```


#### `useEditableDotCMSPage`

The `useEditableDotCMSPage` hook handles the communication with the Universal Visual Editor (UVE) and updates your page content in real-time when changes are made in the editor. This is the core hook for making your dotCMS pages editable in a React application.

##### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `pageResponse` | `DotCMSPageResponse` | Yes | --- | The initial page data returned from `client.page.get()`. |

##### Implementation Details

This hook works by:

1. Taking the page asset from the `pageResponse` parameter
2. Initializing the Universal Visual Editor (UVE) with the parameters from the page asset
3. Setting up event listeners for the UVE to detect content changes
4. Retrieving and returning the updated page asset whenever changes are made in the editor

Important note: This hook only has an effect when your application is running inside the dotCMS editor. When running outside the editor (e.g., on the published site), it simply returns the original page asset without any additional processing.

##### Integration with dotCMS

The `useEditableDotCMSPage` hook is a critical integration point with dotCMS:

1. **Enables Real-Time Editing**: Allows content editors to see changes instantly reflected in the page without refreshing
2. **Handles Layout Management**: Processes all aspects of dotCMS page structure:
   - Rows and their configurations
   - Columns and their widths
   - Container positions and references
   - Content positioning within the page layout
3. **Simplifies Development**: Developers only need to focus on creating components for their content types, rather than implementing the entire editing infrastructure

When combined with `DotCMSLayoutBody`, this hook provides a complete solution for rendering and editing dotCMS pages in React applications.

##### Usage

```jsx
import { useEditableDotCMSPage, DotCMSLayoutBody } from '@dotcms/react/next';
import { createDotCMSClient } from '@dotcms/client';

// Create the client and fetch the page in your data layer
const client = createDotCMSClient({
  dotcmsURL: 'https://your-dotcms-instance.com',
  authToken: 'your-auth-token' // Optional, only needed for authenticated requests
});
  
// Your custom components for rendering different content types
const components = {
  'Product': ProductComponent,
  'Blog': BlogComponent,
  // Add more components as needed
};

const MyPage = ({ initialPageData }) => {
  // Use the hook to get an editable version of the page
  const editablePage = useEditableDotCMSPage(initialPageData);
  
  // Extract page asset for rendering
  const { pageAsset } = editablePage;

  return (
    <div>
      <DotCMSLayoutBody 
        page={pageAsset}
        components={components}
        mode="development"
      />
    </div>
  );
};

// In your data fetching function or page component
export async function getServerSideProps() {
  const pageData = await client.page.get('/', {
    languageId: '1'
  });
  
  return {
    props: {
      initialPageData: pageData
    }
  };
}
```

##### Limitations and Pitfalls

1. **@dotcms/client Dependency**: 
   - This hook is designed specifically to work with page data retrieved via the `@dotcms/client` SDK
   - If you fetch page data using a different method or directly from dotCMS endpoints, the hook may not function correctly
   - The expected structure from `client.page.get()` is required for proper UVE integration

2. **Editor-Only Functionality**:
   - This hook only provides additional functionality when your app is running inside the dotCMS editor
   - In production/published environments, it acts as a pass-through for the page data

3. **Language Handling**:
   - The hook maintains the language ID of the initial page response
   - To support multi-language editing, ensure you fetch the page with the correct language ID

For custom implementations or alternative approaches to making pages editable, please refer to the [FAQ section](#faq) which includes examples of creating custom hooks for editable pages.


## Additional Resources

For more information about working with the dotCMS React SDK:

* **Quick Start Guide**: Visit our [React SDK documentation](https://dev.dotcms.com/docs/javascript-sdk-react-library)
* **API Reference**: Browse the complete [API documentation](https://dev.dotcms.com/docs/javascript-sdk)

Always refer to the official [dotCMS documentation](https://dev.dotcms.com/) for comprehensive guides and API references.


## FAQ

### How do I use dotCMS React components with Next.js App Router?

> **IMPORTANT:** When using the dotCMS React SDK with Next.js App Router, you must use the `'use client'` directive for components like `DotCMSLayoutBody` which are React class components.

In Next.js App Router, all components are Server Components by default. However, React class components (including `DotCMSLayoutBody` and other dotCMS React components) can only be rendered in Client Components.

#### Solution: Create a Client Component for Rendering DotCMS Pages

The recommended pattern is to create a dedicated client component with the `'use client'` directive:

```jsx
// pages/dotCMSPage.js
'use client'

import { DotCMSLayoutBody } from '@dotcms/react/next';

// Define your content type components
const components = {
  Banner: ({ title, description, imageUrl }) => (
    <div className="banner">
      <h2>{title}</h2>
      <p>{description}</p>
      {imageUrl && <img src={imageUrl} alt={title} />}
    </div>
  ),
  // Add more components as needed
};

export default function DotCMSPage({ pageAsset }) {
  if (!pageAsset) {
    return <div className="p-8">No page data available.</div>;
  }

  return (
    <div>
      <DotCMSLayoutBody 
        page={pageAsset}
        components={components}
        mode="development"
      />
    </div>
  );
}
```

Then use this Client Component in your Server Component page:

```jsx
// app/page.js (Server Component)
import dotCMSClient from "../utils/dotCMSClient";
import DotCMSPage from "../pages/dotCMSPage";

export default async function Home() {
  // Fetch the page data on the server
  const { pageAsset } = await dotCMSClient.page.get('/index');
  
  // Pass the fetched page data to the Client Component
  return <DotCMSPage pageAsset={pageAsset} />;
}
```

#### What's Happening:

1. The server component (page.js) fetches the page data using the dotCMS client
2. The page data is passed as a prop to the Client Component (dotCMSPage.js)
3. The Client Component handles all the React class components from dotCMS
4. This pattern maintains the benefits of both Server Components (data fetching) and Client Components (interactivity)

#### Common Mistakes to Avoid:

- Do not try to use `DotCMSLayoutBody` or other class components directly in a Server Component
- Do not move all page logic to a Client Component, as you'll lose the benefits of server-side data fetching
- Remember that all props passed to Client Components must be serializable

Learn more about the distinction between Server and Client Components in the [Next.js documentation](https://nextjs.org/docs/app/building-your-application/rendering/client-components).


### How do I ensure my page renders correctly AND is editable in the Universal Visual Editor?

When integrating with the Universal Visual Editor (UVE), you need to use both the correct component structure AND the `useEditableDotCMSPage` hook. A common mistake is passing only the `pageAsset` to your client component instead of the full `pageResponse`.

#### Incorrect Implementation:
```jsx
// Server component (app/page.js)
export default async function Home() {
  const { pageAsset } = await dotCMSClient.page.get('/index');
  return <DotCMSPage pageAsset={pageAsset} />; // WRONG: only passing pageAsset
}

// Client component (pages/dotCMSPage.js)
export default function DotCMSPage({ pageAsset }) {
  // No editability hook
  return <DotCMSLayoutBody page={pageAsset} components={componentsMap} />;
}
```

#### Correct Implementation:
```jsx
// Server component (app/page.js)
export default async function Home() {
  const pageResponse = await dotCMSClient.page.get('/index');
  return <DotCMSPage pageResponse={pageResponse} />; // RIGHT: passing full pageResponse
}

// Client component (pages/dotCMSPage.js)
export default function DotCMSPage({ pageResponse }) {
  // Make the page editable with UVE
  const editablePage = useEditableDotCMSPage(pageResponse);
  const { pageAsset } = editablePage;
  
  return <DotCMSLayoutBody page={pageAsset} components={componentsMap} />;
}
```

The key differences are:
1. Pass the complete `pageResponse` (not just `pageAsset`)
2. Use the `useEditableDotCMSPage` hook to enable real-time editing
3. Extract the updated `pageAsset` from the hook's result

### How do I properly handle images in dotCMS components?

A common issue with dotCMS image handling is not checking for image existence before constructing URLs. Always verify the image property exists before creating image URLs:

```jsx
function ContentTypeComponent({ contentlet }) {
  // IMPORTANT: Always check if image exists first
  const hasImage = !!contentlet.image;
  
  // Only construct image URL if image exists
  const imageUrl = hasImage ? `/dA/${contentlet.inode}` : null;
  
  return (
    <div>
      <h2>{contentlet.title}</h2>
      {hasImage && (
        <img 
          src={imageUrl}
          alt={contentlet.title || 'Content image'}
          className="content-image"
        />
      )}
    </div>
  );
}
```

Remember to set up a proxy in your Next.js configuration to redirect image requests:

```js
// next.config.mjs
export default {
  async rewrites() {
    return [
      {
        source: '/dA/:path*',
        destination: 'http://localhost:8080/dA/:path*', // Adjust to your dotCMS URL
      },
    ];
  },
};
```


## dotCMS Support

We offer multiple channels to get help with the dotCMS React SDK:

* **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.
* **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions.
* **Stack Overflow**: Use the tag `dotcms-react` when posting questions.

When reporting issues, please include:
- SDK version you're using
- React version
- Minimal reproduction steps
- Expected vs. actual behavior

Enterprise customers can access premium support through the [dotCMS Support Portal](https://dev.dotcms.com/docs/help).


## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the DotCMS UVE SDK! If you'd like to contribute, please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing style and includes appropriate tests.


## Licensing Information

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).
