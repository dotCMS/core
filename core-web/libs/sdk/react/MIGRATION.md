# Migration Guide: @dotcms/react Alpha to 1.0.X

This guide helps you migrate from the alpha version to the 1.0.X version (`latest`) of the `@dotcms/react` SDK.

## Breaking Changes Summary

| Change | Alpha Version | 1.0.X Version |
|--------|---------------|----------------|
| Core Layout Component | `DotcmsLayout` | `DotCMSLayoutBody` |
| Layout Props Structure | `pageContext`/`config` | `page`, `components`, `mode` |
| Editable Text Component | `DotEditableText` | `DotCMSEditableText` |
| Block Editor Renderer | `BlockEditorRenderer` | `DotCMSBlockEditorRenderer` |
| Conditional Rendering | Custom logic | `DotCMSShow` / `useDotCMSShowWhen` |
| Page Asset Hook | `usePageAsset(pageAsset)` | `useEditableDotCMSPage(pageResponse)` |

## New Features in 1.0.X Version

| Feature | Purpose | Example Usage |
|---------|---------|---------------|
| `DotCMSEditableText` | Inline editing of text fields in contentlets | `<DotCMSEditableText contentlet={contentlet} fieldName="title" />` |
| `DotCMSBlockEditorRenderer` | Render Block Editor content with custom block support | `<DotCMSBlockEditorRenderer blocks={contentlet.blockEditorField} customRenderers={{ customBannerBlock: MyCustomBannerBlock }} />` |
| `DotCMSShow` | Conditional rendering based on UVE mode | `<DotCMSShow when={UVE_MODE.EDIT}>...</DotCMSShow>` |
| `useDotCMSShowWhen` | Hook for conditional logic based on UVE mode | `const isEditMode = useDotCMSShowWhen(UVE_MODE.EDIT);` |
| Improved TypeScript Support | Better type definitions and type safety | `import type { DotCMSPageResponse } from '@dotcms/types';` |

🚨 For detail information chech the [README](./README.md) file.

## Step-by-Step Migration Process
Before starting, if you are using the `@dotcms/client` library in your React project, please refer to the [client documentation](https://www.npmjs.com/package/@dotcms/client) for more information.

### Step 1: Update Dependencies

Update your `package.json`:

```json
{
    "dependencies": {
        "@dotcms/client": "1.0.0",
        "@dotcms/react": "1.0.0",
        "@dotcms/types": "1.0.0",
        "@dotcms/uve": "1.0.0"
    }
}
```

### Step 2: Update Component Imports

Replace all instances of:
- `DotcmsLayout` → `DotCMSLayoutBody`
- `DotEditableText` → `DotCMSEditableText`
- `BlockEditorRenderer` → `DotCMSBlockEditorRenderer`

### Step 3: Update Component Usage

**Before:**
```jsx
<DotcmsLayout
    pageContext={{
        pageAsset,
        components: componentsMap,
    }}
    config={{
        pathname,
        editor: {
            params: {
                depth: 3,
            },
        },
    }}
/>
```

**After:**
```jsx
<DotCMSLayoutBody
    page={pageAsset}
    components={componentsMap}
    mode="development"
/>
```

### Step 4: Update Hook Usage

**Before:**
```jsx
import { usePageAsset } from '../hooks/usePageAsset';

function MyPage({ pageAsset, nav }) {
    pageAsset = usePageAsset(pageAsset);
    // ...
}
```

**After:**
```jsx
import { useEditableDotCMSPage } from '@dotcms/react';

function MyPage({ pageResponse, nav }) {
    const { pageAsset } = useEditableDotCMSPage(pageResponse);
    // ...
}
```

### Step 5: Update Client Configuration

**Before:**
```jsx
import { DotCmsClient } from '@dotcms/client';

export const client = DotCmsClient.init({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id"
});
```

**After:**
```jsx
import { createDotCMSClient } from '@dotcms/client';

export const client = createDotCMSClient({
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST,
    authToken: process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN,
    siteId: "your-site-id"
});
```

### Step 6: Update Page Data Fetching

**Before:**
```javascript
const pageData = await client.page.get({
    path: '/your-page-path',
    language_id: 1, // underscore naming
    personaId: 'optional-persona-id'
});
```

**After (1.0.X):**
```javascript
const { pageAsset } = await client.page.get('/your-page-path', {
    languageId: 1, // camelCase naming
    personaId: 'optional-persona-id'
});
```

> [!IMPORTANT]
> For detail information please refer to the [client documentation](https://www.npmjs.com/package/@dotcms/client) for more information.


## TypeScript Support

The 1.0.X version provides better TypeScript support with improved type definitions. Make sure to import types from `@dotcms/types`:

```typescript
import type { DotCMSPageResponse, DotCMSPageAsset } from '@dotcms/types';
import { UVE_MODE } from '@dotcms/types';
```

## Common Migration Issues

### 1. Hook Not Working

**Issue:** Page updates not working in edit mode.

**Solution:** Use `useEditableDotCMSPage` instead of the custom `usePageAsset` hook.

### 2. Types Not Found

**Issue:** TypeScript errors about missing types.

**Solution:** Install and import from `@dotcms/types`:
```bash
npm install @dotcms/types
```

## Testing Your Migration

1. **Development Mode:** Test your application in development mode to see detailed error messages
2. **Edit Mode:** Test the Universal Visual Editor functionality
3. **Production Mode:** Test in production mode for performance optimization

## Additional Resources

- [dotCMS React SDK Documentation](https://dev.dotcms.com/docs/react-sdk)
- [Universal Visual Editor Guide](https://dev.dotcms.com/docs/uve-headless-config)
- [Next.js Example Project](https://github.com/dotCMS/core/tree/main/examples/nextjs)

## Support

If you encounter issues during migration:
- Check the [GitHub Issues](https://github.com/dotCMS/core/issues)
- Join the [Community Forum](https://community.dotcms.com/)
- For enterprise customers: [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)
