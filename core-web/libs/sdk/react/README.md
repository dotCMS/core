# @dotcms/react

`@dotcms/react` is the official set of React components and hooks designed to work seamlessly with dotCMS, making it easy to render dotCMS pages and use the page builder.

> **Note:** This SDK is currently in **beta** (v0.0.1-beta.13 or newest).
> 
> For comprehensive documentation, visit our [developer portal](https://dev.dotcms.com/docs/javascript-sdk-react-library).

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
| Safari  | Latest 2 versions | TLS 1.2+ |

## Components

### `DotCMSLayoutBody`

The `DotCMSLayoutBody` component renders the layout body for a DotCMS page.

#### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `page` | Object | Yes | The DotCMS page asset containing the layout information |
| `components` | Object | Yes | A mapping of custom components for content rendering |
| `mode` | String | No | The renderer mode; defaults to `'production'` |

#### Usage

```javascript
import { DotCMSLayoutBody } from '@dotcms/react';

const MyPage = ({ page }) => {
    return <DotCMSLayoutBody page={page} components={components} />;
};
```

### `DotCMSShow`

The `DotCMSShow` component conditionally renders content based on dotCMS conditions. It uses the UVE_MODE from `@dotcms/uve` which can be one of: `EDIT`, `PREVIEW`, or `LIVE`.

#### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `when` | String | Yes | The condition that determines if the content should be shown (EDIT, PREVIEW, LIVE) |
| `children` | ReactNode | Yes | The content to be rendered when the condition is met |

#### Usage

```javascript
import { DotCMSShow } from '@dotcms/react';
import { UVE_MODE } from '@dotcms/uve';

const MyComponent = () => {
    return (
        <DotCMSShow when={UVE_MODE.EDIT}>
            <div>This content is only visible in edit mode</div>
        </DotCMSShow>
    );
};
```

### `BlockEditorRenderer`

The `BlockEditorRenderer` component renders content from a Block Editor Content Type in dotCMS.

#### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `blocks` | Block | Yes | The block editor content structure to render |
| `customRenderers` | CustomRenderer | No | Optional custom renderers for specific block types |
| `className` | String | No | Optional CSS class name to apply to the container |
| `style` | React.CSSProperties | No | Optional inline styles to apply to the container |
| `contentlet` | DotCMSContentlet | Only when editable is true | Contentlet object containing the field to be edited |
| `fieldName` | String | Only when editable is true | Name of the field in the contentlet that contains the block editor content |

## Hooks

### `useDotCMSShowWhen`

A custom hook that provides the same functionality as the `DotCMSShow` component in a hook form. It uses the UVE_MODE from `@dotcms/uve` which can be one of: `EDIT`, `PREVIEW`, or `LIVE`.

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `mode` | String | Yes | The UVE mode to check against (EDIT, PREVIEW, LIVE) |

#### Usage

```javascript
import { useDotCMSShowWhen } from '@dotcms/react';
import { UVE_MODE } from '@dotcms/uve';

const MyComponent = () => {
    const isVisible = useDotCMSShowWhen(UVE_MODE.EDIT);
    
    return isVisible ? <div>Visible content</div> : null;
};
```

### `usePageAsset`

A custom hook that handles the communication with the Universal View Editor (UVE) and updates your page content in real-time when changes are made in the editor.

> **Note:** This hook will be built into the SDK in the stable version - the example below is a temporary workaround for the beta release.
>
> You need to save this hook in your project as a custom hook file.

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `currentPageAsset` | Object | Yes | The initial page asset from the server |

#### Implementation

```javascript
import { useEffect, useState } from 'react';

import { getUVEState, sendMessageToEditor, createUVESubscription} from '@dotcms/uve';
import { DotCMSUVEAction, UVEEventType} from '@dotcms/uve/types';

export const usePageAsset = (currentPageAsset) => {
    const [pageAsset, setPageAsset] = useState(null);
    useEffect(() => {
        if (!getUVEState()) {
            return;
        }

        sendMessageToEditor({ action: DotCMSUVEAction.CLIENT_READY });
        const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES,(pageAsset) => setPageAsset(pageAsset));

        return () => {
            subscription.unsubscribe();
        };
    }, [currentPageAsset]);

    return pageAsset ?? currentPageAsset;
};
```

#### Usage

```javascript
// Import the hook from where you saved it in your project
import { usePageAsset } from './hooks/usePageAsset';

const MyPage = ({ initialPageAsset }) => {
    const pageAsset = usePageAsset(initialPageAsset);
    
    return <DotCMSLayoutBody page={pageAsset} components={components} />;
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