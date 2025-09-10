# DotCMS Type Definition Library

📦 [@dotcms/types on npm](https://www.npmjs.com/package/@dotcms/types)
🛠️ [View source on GitHub](https://github.com/dotCMS/core/tree/main/core-web/libs/sdk/types)

## Installation

```bash
npm install @dotcms/types@latest --save-dev
```

## Overview

This package contains TypeScript type definitions for the dotCMS ecosystem. Use it to enable type safety and an enhanced developer experience when working with dotCMS APIs and structured content.

## Commonly Used Types

```ts
import {
  DotCMSPageAsset,
  DotCMSPageResponse,
  UVEEventType,
  DotCMSInlineEditingPayload
} from '@dotcms/types';
```

## Type Hierarchy (Jump to Definitions)

### Page Types

* [DotCMSPageAsset](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L18)

  * [DotCMSPage](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L515)
  * [DotCMSPageAssetContainer](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L138)
  * [DotCMSSite](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L733)
  * [DotCMSTemplate](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L432)
  * [DotCMSViewAs](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L598)
  * [DotCMSVanityUrl](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L80)
  * [DotCMSURLContentMap](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L41)
  * [DotCMSLayout](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L622)
  * [DotCMSPageContainerContentlets](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1139)
  * [DotCMSPageResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1175)
  * [DotCMSComposedPageAsset](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1189)
  * [DotCMSComposedContent](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1195)
  * [DotCMSComposedPageResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1199)
  * [DotCMSClientPageGetResponse](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/page/public.ts#L1208)

### Block Editor Types

* [BlockEditorContent](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L38)

  * [BlockEditorNode](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L18)
  * [BlockEditorMark](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/components/block-editor-renderer/public.ts#L7)

### Editor & UVE Types

* [UVEState](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L30)

  * [UVE\_MODE](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L55)
* [UVEEventHandler](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L67)
* [UVEEventSubscriber](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L90)

  * [UVEEventSubscription](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L81)
  * [UVEUnsubscribeFunction](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L73)
* [UVEEventType](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L169)

  * [UVEEventPayloadMap](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L199)
* [DotCMSPageRendererMode](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L44)
* [DotCMSUVEAction](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/editor/public.ts#L98)

### Events

* [DotCMSInlineEditingPayload](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/events/public.ts#L13)

  * [DotCMSInlineEditingType](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/events/public.ts#L1)

### Client Request Types

* [DotCMSPageRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L35)

  * [DotCMSGraphQLParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L7)
  * [RequestOptions](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L84)
  * [DotCMSClientConfig](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L89)
  * [DotCMSNavigationRequestParams](https://github.com/dotCMS/core/blob/main/core-web/libs/sdk/types/src/lib/client/public.ts#L119)

## About

This package is maintained as part of the [dotCMS core repository](https://github.com/dotCMS/core).

### Keywords

* dotcms
* typescript
* types
* cms 
* content-management-system
