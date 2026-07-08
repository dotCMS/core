# @dotcms/vue

Official **Vue 3** components and composables for rendering [dotCMS](https://www.dotcms.com/) pages, with full support for the **Universal Visual Editor (UVE)**.

This is the Vue counterpart of [`@dotcms/react`](../react) and [`@dotcms/angular`](../angular). It renders a dotCMS page layout, maps content types to your own Vue components, and keeps the page live-editable inside the UVE.

## Installation

```bash
npm install @dotcms/vue @dotcms/client @dotcms/uve @dotcms/types
```

Requires **Vue 3.4+** (peer dependency).

## Quick start

```ts
// dotCMSClient.ts
import { createDotCMSClient } from '@dotcms/client';

export const client = createDotCMSClient({
    dotcmsUrl: import.meta.env.VITE_DOTCMS_HOST,
    authToken: import.meta.env.VITE_DOTCMS_AUTH_TOKEN,
    siteId: import.meta.env.VITE_DOTCMS_SITE_ID,
    // UVE needs fresh data so in-context edits are reflected immediately.
    requestOptions: { cache: 'no-cache' }
});
```

```vue
<!-- Page.vue -->
<script setup lang="ts">
import { DotCMSLayoutBody, useEditableDotCMSPage } from '@dotcms/vue';

import { pageComponents } from './content-types';

// `pageResponse` comes from `client.page.get(path, { ... })`.
const props = defineProps<{ pageResponse: unknown }>();
const page = useEditableDotCMSPage(props.pageResponse);
</script>

<template>
    <DotCMSLayoutBody
        v-if="page.pageAsset"
        :page="page.pageAsset"
        :components="pageComponents" />
</template>
```

```ts
// content-types/index.ts — keys MUST match the dotCMS content type variable name.
import Banner from './Banner.vue';
import Product from './Product.vue';

export const pageComponents = {
    Banner,
    Product
    // CustomNoComponent: Fallback   // optional fallback for unmapped types
};
```

## API

### Components

| Component | Purpose |
| --- | --- |
| `DotCMSLayoutBody` | Renders a page's layout (rows → columns → containers → contentlets) and dispatches each contentlet to the mapped component. Props: `page`, `components`, `mode`. |
| `DotCMSBlockEditorRenderer` | Renders a Block Editor field to native markup. Props: `blocks`, `customRenderers`, `className`, `style`, `isDevMode`. Custom renderers are Vue components receiving a `node` prop and a default slot with rendered children. |
| `DotCMSEditableText` | Inline text editing of a contentlet field inside the UVE (TinyMCE). Props: `contentlet`, `fieldName`, `mode`, `format`. |
| `DotCMSShow` | Conditionally renders its slot based on the current UVE mode. Prop: `when`. |

### Composables

| Composable | Purpose |
| --- | --- |
| `useEditableDotCMSPage(pageResponse)` | Returns a reactive `Ref` to the page response that live-updates while editing in the UVE. |
| `useDotCMSShowWhen(mode)` | Returns a reactive `Ref<boolean>` — whether the current UVE mode matches. |

### Context

`provideDotCMSPageContext` / `useDotCMSPageContext` expose the page context for advanced/custom rendering.

## Universal Visual Editor

`useEditableDotCMSPage` initializes the UVE bridge, keeps navigation in sync, and re-renders the page when an editor makes changes. Outside the editor it is a no-op pass-through. Editor actions (`editContentlet`, `reorderMenu`, `enableBlockEditorInline`, …) come from `@dotcms/uve`.

## Development

```bash
nx build sdk-vue    # build the library (Vite + vite-plugin-dts)
nx test sdk-vue     # run unit tests (Vitest + @vue/test-utils)
nx lint sdk-vue
```
