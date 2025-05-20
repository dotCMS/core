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

## Table of Contents

- [What is it?](#what-is-it)
- [How To Install](#how-to-install)
- [Dependencies](#dependencies)
- [Browser Compatibility](#browser-compatibility)
- [Detailed API Documentation](#detailed-api-documentation)
  - [Components](#components)
    - [DotCMSLayoutBody](#dotcmslayoutbody)
    - [DotCMSShow](#dotcmsshow)
    - [DotCMSBlockEditorRenderer](#dotcmsblockeditoenderer)
    - [DotCMSEditableText](#dotcmseditabletext)
  - [Hooks](#hooks)
    - [useDotCMSShowWhen](#usedotcmsshowwhen)
    - [useEditableDotCMSPage](#useeditabledotcmspage)
- [Documentation](#documentation)
- [FAQ](#faq)
- [How To Contribute](#how-to-contribute)
- [Licensing Information](#licensing-information)
- [dotCMS Support](#dotcms-support)

## How To Install

The React SDK is automatically included in DotCMS installations. For external usage:

```bash
# Using npm
npm install @dotcms/react

# Using yarn
yarn add @dotcms/react

# Using pnpm
pnpm add @dotcms/react
```

## Dependencies

This package has the following peer dependencies that you'll need to install in your project:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/uve` | latest | Required for page editing functionality |
| `@dotcms/client` | latest | Required for page fetching functionality |
| `@tinymce/tinymce-react` | ^6.0.0 | Required for TinyMCE integration |

Install peer dependencies:

```bash
# Using npm
npm install @dotcms/uve @dotcms/client @tinymce/tinymce-react

# Using yarn
yarn add @dotcms/uve @dotcms/client @tinymce/tinymce-react

# Using pnpm
pnpm add @dotcms/uve @dotcms/client @tinymce/tinymce-react
```

## Browser Compatibility

The `@dotcms/react` package is compatible with the following browsers:

| Browser | Minimum Version | TLS Version |
|---------|----------------|-------------|
| Chrome  | Latest 2 versions | TLS 1.2+ |
| Edge    | Latest 2 versions | TLS 1.2+ |
| Firefox | Latest 2 versions | TLS 1.2+ |

## Detailed API Documentation

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

The `DotCMSEditableText` component allows inline editing of text content pulled from dotCMS API using the TinyMCE editor. This component is specifically designed for edit mode and falls back to a simple display in other modes.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `contentlet` | `object` | Yes | --- | The contentlet object containing the field to be edited. |
| `fieldName` | `string` | Yes | --- | Name of the field in the contentlet that contains the text content. |
| `mode` | `string` | No | `'plain'` | Editor mode. Can be `'plain'` or `'full'`. |
| `format` | `string` | No | `'text'` | Content format. Can be `'text'` or `'html'`. |

##### Usage

```jsx
import { DotCMSEditableText } from '@dotcms/react/next';

const MyContentletWithTitle = ({ contentlet }) => (
  <h2>
    <DotCMSEditableText contentlet={contentlet} fieldName="title" />
  </h2>
);
```

### Hooks

#### `useDotCMSShowWhen`

The `useDotCMSShowWhen` hook provides the same functionality as the `DotCMSShow` component but in hook form. It determines if the current Universal Visual Editor (UVE) mode matches a specified mode.

##### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `when` | `UVE_MODE` | Yes | --- | The UVE mode to check against. Can be `UVE_MODE.EDIT`, `UVE_MODE.PREVIEW`, or `UVE_MODE.LIVE`. |

##### Usage

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

#### `useEditableDotCMSPage`

The `useEditableDotCMSPage` hook handles the communication with the Universal Visual Editor (UVE) and updates your page content in real-time when changes are made in the editor. This is the core hook for making your dotCMS pages editable in a React application.

##### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `pageResponse` | `DotCMSPageResponse` | Yes | --- | The initial page data returned from `client.page.get()`. |

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
    <main>
      <DotCMSLayoutBody page={pageAsset} components={components} />
    </main>
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

## Documentation

For more information about working with the dotCMS React SDK:

* **Getting Started**: Visit our [React SDK Quick Start Guide](https://dev.dotcms.com/docs/javascript-sdk-react-library)
* **API Reference**: Browse the complete [API documentation](https://dev.dotcms.com/docs/javascript-sdk)
* **Example Projects**: Check out our [sample applications](https://github.com/dotCMS/core/tree/main/examples/nextjs) built with the React SDK

Always refer to the official [dotCMS documentation](https://dev.dotcms.com/) for comprehensive guides and API references.

## FAQ

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

Enterprise customers can access premium support through the [dotCMS Support Portal](https://support.dotcms.com/).
