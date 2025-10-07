# Migration Guide: @dotcms/angular Alpha to 1.0.X

This guide helps you migrate from the alpha version to the 1.0.X version (`latest`) of the `@dotcms/angular` SDK.

## Breaking Changes Summary

| Change | Alpha Version | 1.0.X Version |
|--------|---------------|----------------|
| Core Layout Component | `DotcmsLayoutComponent` | `DotCMSLayoutBody` |
| Layout Props Structure | `pageAsset`/`editor` | `page`, `components`, `mode` |
| Client Configuration | Manual token injection | `provideDotCMSClient()` |
| Client Injection | `DOTCMS_CLIENT_TOKEN` | `DotCMSClient` |
| Component Registration | `DotCMSPageComponent` | `DynamicComponentEntity` |
| Page Service | Custom implementation | `DotCMSEditablePageService` |

## New Features in 1.0.X Version

| Feature | Purpose | Example Usage |
|---------|---------|---------------|
| `DotCMSEditableText` | Inline editing of text fields in contentlets | `<dotcms-editable-text fieldName="title" [contentlet]="contentlet()" />` |
| `DotCMSBlockEditorRenderer` | Render Block Editor content with custom block support | `<dotcms-block-editor-renderer [blocks]="contentlet.blockEditorField" [customRenderers]="customRenderers()" />` |
| `DotCMSShowWhen` | Conditional rendering based on UVE mode | `<div *dotCMSShowWhen="UVE_MODE.EDIT">...</div>` |
| `DotCMSEditablePageService` | Service for managing page lifecycle and UVE integration | `this.editablePageService.listen(pageResponse)` |
| `provideDotCMSImageLoader` | Angular image optimization integration | `provideDotCMSImageLoader(environment.dotcmsUrl)` |
| Improved TypeScript Support | Better type definitions and type safety | `import type { DotCMSPageResponse } from '@dotcms/types';` |

🚨 For detail information check the [README](./README.md) file.

## Step-by-Step Migration Process
Before starting, if you are using the `@dotcms/client` library in your Angular project, please refer to the [client documentation](https://www.npmjs.com/package/@dotcms/client) for more information.

### Step 1: Update Dependencies

Update your `package.json`:

```json
{
    "dependencies": {
        "@dotcms/angular": "1.0.0",
        "@dotcms/client": "1.0.0",
        "@dotcms/types": "1.0.0",
        "@dotcms/uve": "1.0.0"
    }
}
```

### Step 2: Update Component Imports

Replace all instances of:
- `DotcmsLayoutComponent` → `DotCMSLayoutBody`
- Import from `@dotcms/angular` instead of separate packages

### Step 3: Update Component Usage

**Before:**
```typescript
<dotcms-layout
    [pageAsset]="pageAsset"
    [components]="components()"
    [editor]="editorConfig()"
/>
```

**After:**
```typescript
<dotcms-layout-body
    [page]="pageAsset()"
    [components]="components()"
    mode="development"
/>
```

### Step 4: Update Client Configuration

**Before:**
```typescript
import { InjectionToken } from '@angular/core';
import { ClientConfig, DotCmsClient } from '@dotcms/client';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

export const appConfig: ApplicationConfig = {
    providers: [
        {
            provide: DOTCMS_CLIENT_TOKEN,
            useValue: DotCmsClient.init(DOTCMS_CLIENT_CONFIG),
        }
    ],
};
```

**After:**
```typescript
import { provideDotCMSClient } from '@dotcms/angular';

export const appConfig: ApplicationConfig = {
    providers: [
        provideDotCMSClient({
            dotcmsUrl: environment.dotcmsUrl,
            authToken: environment.authToken,
            siteId: environment.siteId
        })
    ],
};
```

### Step 5: Update Client Injection

**Before:**
```typescript
export class YourComponent {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);
}
```

**After:**
```typescript
import { DotCMSClient } from '@dotcms/angular';

export class YourComponent {
    private readonly client = inject(DotCMSClient);
}
```

### Step 6: Update Page Data Fetching

**Before:**
```typescript
const pageData = await this.client.page.get({
    path: '/your-page-path',
    language_id: 1, // underscore naming
    personaId: 'optional-persona-id'
});
```

**After (1.0.X):**
```typescript
const { pageAsset } = await this.client.page.get('/your-page-path', {
    languageId: 1, // camelCase naming
    personaId: 'optional-persona-id'
});
```

> [!IMPORTANT]
> For detail information please refer to the [client documentation](https://www.npmjs.com/package/@dotcms/client) for more information.

### Step 7: Update Page Service Implementation

**Before:**
```typescript
// Custom service implementation required
export class PagesComponent {
    pageAsset: DotCMSPageAsset;

    ngOnInit() {
        // Manual page loading and UVE setup
    }
}
```

**After:**
```typescript
import { DotCMSEditablePageService } from '@dotcms/angular';

@Component({
    providers: [DotCMSEditablePageService]
})
export class PagesComponent {
    private readonly editablePageService = inject(DotCMSEditablePageService);
    readonly pageAsset = signal<DotCMSPageAsset | null>(null);

    ngOnInit() {
        this.client.page.get({ url: '/page' }).then((pageResponse) => {
            if (getUVEState()) {
                this.editablePageService
                    .listen(pageResponse)
                    .subscribe(({ pageAsset }) => {
                        this.pageAsset.set(pageAsset);
                    });
            } else {
                this.pageAsset.set(pageResponse.pageAsset);
            }
        });
    }
}
```

### Step 8: Update Component Registration

**Before:**
```typescript
const DYNAMIC_COMPONENTS: DotCMSPageComponent = {
    Activity: import('./activity.component').then(c => c.ActivityComponent),
    Banner: import('./banner.component').then(c => c.BannerComponent)
};
```

**After:**
```typescript
import { DynamicComponentEntity } from '@dotcms/angular';

const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
    Activity: import('./activity.component').then(c => c.ActivityComponent),
    Banner: import('./banner.component').then(c => c.BannerComponent)
};
```

## TypeScript Support

The 1.0.X version provides better TypeScript support with improved type definitions. Make sure to import types from `@dotcms/types`:

```typescript
import type { DotCMSPageResponse, DotCMSPageAsset } from '@dotcms/types';
import { UVE_MODE } from '@dotcms/types';
```

## Common Migration Issues

### 1. Service Not Working

**Issue:** Page updates not working in edit mode.

**Solution:** Use `DotCMSEditablePageService` and provide it at component level.

### 2. Client Injection Errors

**Issue:** `NullInjectorError` when injecting the client.

**Solution:** Use `provideDotCMSClient()` in your app configuration and inject `DotCMSClient` directly.

### 3. Component Registration Issues

**Issue:** Components not rendering or TypeScript errors.

**Solution:** Use proper typing with `DynamicComponentEntity` and ensure all components are standalone or properly declared.

### 4. Types Not Found

**Issue:** TypeScript errors about missing types.

**Solution:** Install and import from `@dotcms/types`:
```bash
npm install @dotcms/types
```

## Testing Your Migration

1. **Development Mode:** Use `mode="development"` in `DotCMSLayoutBody` to see detailed error messages and debugging information
2. **Edit Mode:** Test the Universal Visual Editor functionality with `DotCMSEditablePageService`
3. **Production Mode:** Test with `mode="production"` for performance optimization

## Additional Resources

- [dotCMS Angular SDK Documentation](https://dev.dotcms.com/docs/angular-sdk)
- [Universal Visual Editor Guide](https://dev.dotcms.com/docs/uve-headless-config)
- [Angular Example Project](https://github.com/dotCMS/core/tree/main/examples/angular)

## Support

If you encounter issues during migration:
- Check the [GitHub Issues](https://github.com/dotCMS/core/issues)
- Join the [Community Forum](https://community.dotcms.com/)
- For enterprise customers: [dotCMS Support Portal](https://helpdesk.dotcms.com/support/)
