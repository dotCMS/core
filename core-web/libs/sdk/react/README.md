# @dotcms/react

`@dotcms/react` is the official set of React components and hooks designed to work seamlessly with dotCMS, making it easy to render dotCMS pages an use the page builder.

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

### `DotcmsLayout`

A functional component that renders a layout for a dotCMS page.

#### Props

-   **entity**: The context for a dotCMS page.

#### Usage

```javascript
import { DotcmsLayout } from '@dotcms/react';

const MyPage = ({ entity }) => {
    return <DotcmsLayout entity={entity} />;
};
```

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
