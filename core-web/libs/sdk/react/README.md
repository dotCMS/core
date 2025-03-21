# @dotcms/react

`@dotcms/react` is the official set of React components and hooks designed to work seamlessly with dotCMS, making it easy to render dotCMS pages and use the page builder.

## Features

-   A collection of React components and hooks tailored to render dotCMS pages.
-   Streamlined integration with dotCMS page editor.
-   Improved development experience with comprehensive TypeScript typings.

## Installation

Install the package via npm:

```bash
npm install @dotcms/react
```

Or using Yarn:

```bash
yarn add @dotcms/react
```

## Components

### Deprecated: DotcmsLayout

**Important:** The `DotcmsLayout` component is deprecated. Please use the new [`DotCMSLayoutBody`](#DotCMSLayoutBody) component instead.

#### Props

-   **entity**: The context for a dotCMS page.

#### Usage

```javascript
// Deprecated:
import { DotcmsLayout } from '@dotcms/react';

const MyPage = ({ entity }) => {
    return <DotcmsLayout entity={entity} />;
};
```

### `DotCMSLayoutBody`

The `DotCMSLayoutBody` component renders the layout body for a DotCMS page.

#### Props

-   **page**: The DotCMS page asset containing the layout information.
-   **components**: A mapping of custom components for content rendering.
-   **mode** (optional): The renderer mode; defaults to `'production'`.


#### Usage

```javascript
import { DotCMSLayoutBody } from '@dotcms/react';

const MyPage = ({ page }) => {
    return <DotCMSLayoutBody page={page} components={components} />;
};
```


### `BlockEditorRenderer`

The `BlockEditorRenderer` component renders the content of a Block Editor Content Type from dotCMS.
[More information of Block Editor Content Type](https://dev.dotcms.com/docs/block-editor)

#### Props


| Prop | Type | Description |
|------|------|-------------|
| `blocks` | `Block` | The block editor content structure to render. |
| `customRenderers` | `CustomRenderer` | Optional custom renderers for specific block types. |
| `className` | `string` | Optional CSS class name to apply to the container. |
| `style` | `React.CSSProperties` | Optional inline styles to apply to the container. |
| `contentlet` | `DotCMSContentlet` | Contentlet object containing the field to be edited. Required when editable is true. |
| `fieldName` | `string` | Name of the field in the contentlet that contains the block editor content. Required when editable is true. |

For a more in-depth explanation of BlockEditorRenderer, visit the [documentation](./src/lib/deprecated/components/BlockEditorRenderer/BlockEditorRenderer.md).

## Hooks

### `useDotcmsPageContext`

A custom React hook that provides access to the `PageProviderContext`.

#### Returns

-   `PageProviderContext | null`: The context value or `null` if it's not available.

#### Usage

```javascript
import { useDotcmsPageContext } from '@dotcms/react';

const MyComponent = () => {
    const context = useDotcmsPageContext();
    // Use the context
};
```

### `usePageEditor`

A custom React hook that sets up the page editor for a dotCMS page.

#### Parameters

-   **props**: `PageEditorOptions` - The options for the page editor. Includes a `reloadFunction` and a `pathname`.

#### Returns

-   `React.RefObject<HTMLDivElement>[]`: A reference to the rows of the page.

#### Usage

```javascript
import { usePageEditor } from '@dotcms/react';

const MyEditor = () => {
    const rowsRef = usePageEditor({ pathname: '/my-page' });
    // Use the rowsRef
};
```

## Context Providers

### `PageProvider`

A functional component that provides a context for a dotCMS page.

#### Props

-   **entity**: The entity representing the page's data.
-   **children**: The children components.

#### Usage

```javascript
import { PageProvider } from '@dotcms/react';

const MyApp = ({ entity }) => {
    return <PageProvider entity={entity}>{/* children */}</PageProvider>;
};
```

## Contributing

GitHub pull requests are the preferred method to contribute code to dotCMS. Before any pull requests can be accepted, an automated tool will ask you to agree to the [dotCMS Contributor's Agreement](https://gist.github.com/wezell/85ef45298c48494b90d92755b583acb3).

## Licensing

dotCMS comes in multiple editions and as such is dual licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds a number of enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://dotcms.com/cms-platform/features).

## Support

If you need help or have any questions, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository.

## Documentation

Always refer to the official [DotCMS documentation](https://www.dotcms.com/docs/latest/) for comprehensive guides and API references.

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://dotcms.com/videos/)                         |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://dotcms.com/)                                   |
