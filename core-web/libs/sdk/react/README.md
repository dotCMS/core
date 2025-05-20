# dotCMS React SDK

`@dotcms/react` is the official set of React components and hooks designed to work seamlessly with dotCMS, making it easy to render dotCMS pages and use the page builder.

> **Note:** This SDK is currently in **beta** (v0.0.1-beta.13 or newest).
>
> For comprehensive documentation, visit our [developer portal](https://dev.dotcms.com/docs/javascript-sdk-react-library).

> **⚠️ IMPORTANT:** Versions published under the `next` tag (`npm install @dotcms/react@next`) are experimental, in beta, and not code complete. For the current stable and functional version, please use `latest` (`npm install @dotcms/react@latest`). Once we release the stable version, we will provide a migration guide from the alpha to stable version. The current alpha version (under `latest`) will continue to work, allowing you to migrate progressively at your own pace.

## Table of Contents

- [What's New](#whats-new)
- [What's Being Deprecated](#whats-being-deprecated)
- [Installation](#installation)
- [Dependencies](#dependencies)
- [Browser Compatibility](#browser-compatibility)
- [Components](#components)
  - [DotCMSLayoutBody](#dotcmslayoutbody)
  - [DotCMSShow](#dotcmsshow)
  - [BlockEditorRenderer](#blockeditorrenderer)
- [Hooks](#hooks)
  - [useDotCMSShowWhen](#usedotcmsshowwhen)
  - [usePageAsset](#usepageasset)
  - [useEditableDotCMSPage](#useeditabledotcmspage)
- [Making Your Page Editable](#making-your-page-editable)
- [Contributing](#contributing)
- [Licensing](#licensing)
- [Support](#support)
- [Documentation](#documentation)

## What's New?

- **Refactored Components (v0.0.1-beta.13):** Improved structure for better maintainability and performance.
- **New `DotCMSLayoutBody` Component (v0.0.1-beta.13):** Replaces `DotcmsLayout`, providing a more flexible approach to rendering page layouts.
- **Enhanced Block Editor Support (v0.0.1-beta.13):** The `BlockEditorRenderer` now supports advanced custom renderers.
- **Improved TypeScript Support (v0.0.1-beta.13):** Comprehensive typings for better developer experience.

Install the latest version with:

```bash
npm install @dotcms/react@0.0.1-beta.13
# or use the newest version
npm install @dotcms/react@latest
```

## What's Being Deprecated?

- **`DotcmsLayout` Component:** Now replaced by `DotCMSLayoutBody`.
- **`useDotcmsPageContext` Hook:** No longer needed with the new component architecture.
- **`Context Providers`:** These are being phased out in favor of a more direct approach.

> **Note:** Deprecated items will continue to work and be supported, but won't receive new features or improvements. This approach allows users upgrading from alpha to beta or stable versions to update their codebase progressively without immediate breaking changes.

## Installation

Install the package via npm:

```bash
npm install @dotcms/react
```

Or using Yarn:

```bash
yarn add @dotcms/react
```

## Dependencies

This package has the following peer dependencies that you'll need to install in your project:

| Dependency | Version | Description |
|------------|---------|-------------|
| `@dotcms/uve` | * | Required for page editing functionality |
| `react` | >=16.8.0 | React library |
| `react-dom` | >=16.8.0 | React DOM library |

Install peer dependencies:

```bash
npm install @dotcms/uve react react-dom
```

## Browser Compatibility

The @dotcms/react package is compatible with the following browsers:

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

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `page` | `DotCMSPageAsset` | Yes | The DotCMS page asset containing the layout information |
| `components` | `Object` | Yes | A mapping of custom components for content rendering. Keys should match content types in dotCMS |
| `mode` | `String` | No | The renderer mode; defaults to `'production'`. Can be either `'production'` or `'development'` |

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

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `children` | `React.ReactNode` | Yes | The content to be rendered when the condition is met |
| `when` | `UVE_MODE` | No | The UVE mode in which the children should be rendered. Can be `UVE_MODE.EDIT`, `UVE_MODE.PREVIEW`, or `UVE_MODE.LIVE`. Defaults to `UVE_MODE.EDIT` |

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

## Making Your Page Editable

To make your page editable in dotCMS, you need to use the `usePageAsset` hook described above. This hook synchronizes your page with the Universal View Editor (UVE) and ensures that any changes made in the editor are reflected in your React application in real-time.

You need to save the hook implementation in your project (for example, in a file like `hooks/usePageAsset.js`) and import it where needed.

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [DotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references. For specific React library documentation, visit our [developer portal](https://dev.dotcms.com/docs/javascript-sdk-react-library).