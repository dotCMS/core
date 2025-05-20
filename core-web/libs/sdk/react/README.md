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

##### Usage

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

#### `DotCMSShow`

The `DotCMSShow` component conditionally renders its children based on the Universal Visual Editor (UVE) mode. This is useful for displaying different content in different editing modes.

##### Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `children` | `React.ReactNode` | Yes | --- | The content to be rendered when the condition is met. |
| `when` | `UVE_MODE` | No | `UVE_MODE.EDIT` | The UVE mode in which the children should be rendered. Can be `UVE_MODE.EDIT`, `UVE_MODE.PREVIEW`, or `UVE_MODE.LIVE`. |

##### Usage

```jsx
import { DotCMSShow } from '@dotcms/react/next';
import { UVE_MODE } from '@dotcms/uve';
import { editContentlet } from '@dotcms/uve';

// This component creates an edit button for any contentlet, even if it doesn't belong to the current page
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

// Usage example in a component that displays related content
const RelatedArticle = ({ article }) => {
  return (
    <div className="related-article">
      <h3>{article.title}</h3>
      <p>{article.summary}</p>
      <EditButton contentlet={article} />
    </div>
  );
};
```

#### `DotCMSBlockEditorRenderer`

The `DotCMSBlockEditorRenderer` component renders content from a Block Editor Content Type in dotCMS. It supports custom renderers for different block types, allowing for flexible content display.

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

> **Note:** For advanced usage including custom renderers, inline editing capabilities, and best practices, please refer to the [detailed Block Editor documentation](https://dev.dotcms.com/docs/javascript-sdk-react-library/block-editor).

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
