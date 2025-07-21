# Migration Guide: @dotcms/angular from Alpha to 1.0.X

This guide will help you migrate your Angular applications from the alpha version of `@dotcms/angular` to version 1.0.X. The new version introduces significant architectural changes, new components, better TypeScript support, and enhanced Universal Visual Editor (UVE) integration.

## Table of Contents

- [Overview of Changes](#overview-of-changes)
- [Breaking Changes](#breaking-changes)
- [Installation & Dependencies](#installation--dependencies)
- [Configuration Changes](#configuration-changes)
- [Component Migrations](#component-migrations)
- [New Features](#new-features)
- [Service Changes](#service-changes)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview of Changes

The 1.0.X version represents a major rewrite of the `@dotcms/angular` library with:

- **New Architecture**: Simplified component structure and better separation of concerns
- **Enhanced UVE Integration**: Improved Universal Visual Editor support with real-time editing
- **Better TypeScript Support**: Comprehensive type definitions and improved developer experience
- **New Components**: Additional components for better content rendering and editing
- **Simplified Configuration**: Streamlined setup process with provider functions

## Breaking Changes

### 1. Main Layout Component

**Old (Alpha):**
```typescript
import { DotcmsLayoutComponent } from '@dotcms/angular';

// Usage in template
<dotcms-layout
    [pageAsset]="pageAsset"
    [components]="components()"
    [editor]="editorConfig()"
/>
```

**New (1.0.X):**
```typescript
import { DotCMSLayoutBody } from '@dotcms/angular';

// Usage in template
<dotcms-layout-body
    [page]="pageAsset()"
    [components]="components()"
    [mode]="'development'"
/>
```

**Key Changes:**
- `DotcmsLayoutComponent` → `DotCMSLayoutBody`
- `pageAsset` prop → `page` prop
- `editor` prop removed (UVE integration now handled automatically)
- New optional `mode` prop for development/production rendering

### 2. Client Configuration

**Old (Alpha):**
```typescript
// app.config.ts
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

// Component usage
export class YourComponent {
    private readonly client = inject(DOTCMS_CLIENT_TOKEN);
}
```

**New (1.0.X):**
```typescript
// app.config.ts
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

// Component usage
import { DotCMSClient } from '@dotcms/angular';

export class YourComponent {
    private readonly client = inject(DotCMSClient);
}
```

**Key Changes:**
- Use `provideDotCMSClient()` function instead of manual token creation
- Direct import of `DotCMSClient` for injection
- Simplified configuration approach

## Installation & Dependencies

### Update Dependencies

**Remove old dependencies:**
```bash
npm uninstall @dotcms/client
```

**Install new dependencies:**
```bash
npm install @dotcms/angular@latest
```

The new version automatically includes:
- `@dotcms/client`: Core client functionality
- `@dotcms/uve`: Universal Visual Editor integration
- `@dotcms/types`: TypeScript type definitions

### Package.json Changes

**Old (Alpha):**
```json
{
  "dependencies": {
    "@dotcms/angular": "alpha",
    "@dotcms/client": "latest"
  }
}
```

**New (1.0.X):**
```json
{
  "dependencies": {
    "@dotcms/angular": "latest",
    "@dotcms/types": "latest",
    "@dotcms/uve": "latest"
  }
}
```

## Configuration Changes

### 1. Image Optimization Setup

**New Feature in 1.0.X:**
```typescript
// app.config.ts
import { provideDotCMSImageLoader } from '@dotcms/angular';

export const appConfig: ApplicationConfig = {
    providers: [
        // ... other providers
        provideDotCMSImageLoader(environment.dotcmsUrl)
    ]
};
```

### 2. Proxy Configuration

Update your `proxy.conf.json` to include API endpoints:

**Old (Alpha):**
```json
{
    "/dA": {
        "target": "http://localhost:8080",
        "secure": false,
        "changeOrigin": true
    }
}
```

**New (1.0.X):**
```json
{
    "/api": {
        "target": "http://localhost:8080",
        "secure": false
    },
    "/dA": {
        "target": "http://localhost:8080",
        "secure": false
    }
}
```

## Component Migrations

### 1. Page Component Structure

**Old (Alpha):**
```typescript
@Component({
    selector: 'app-pages',
    templateUrl: './pages.component.html',
})
export class PagesComponent {
    DYNAMIC_COMPONENTS: DotCMSPageComponent = {
        Activity: import('../content-types/activity/activity.component').then(
            (c) => c.ActivityComponent
        ),
    };

    components = signal(this.DYNAMIC_COMPONENTS);
    editorConfig = signal<EditorConfig>({ params: { depth: 2 } });
    pageAsset: DotCMSPageAsset;
}
```

**New (1.0.X):**
```typescript
@Component({
    selector: 'app-pages',
    standalone: true,
    imports: [DotCMSLayoutBody],
    providers: [DotCMSEditablePageService],
    template: `
        @if (pageAsset()) {
            <dotcms-layout-body
                [page]="pageAsset()"
                [components]="components()"
            />
        } @else {
            <div>Loading...</div>
        }
    `
})
export class PagesComponent {
    private readonly dotCMSClient = inject(DotCMSClient);
    private readonly editablePageService = inject(DotCMSEditablePageService);

    readonly components = signal(DYNAMIC_COMPONENTS);
    readonly pageAsset = signal<DotCMSPageAsset | null>(null);

    ngOnInit() {
        this.dotCMSClient.page
            .get({ url: '/my-page' })
            .then(({ pageAsset }) => {
                if(getUVEState()) {
                   this.#subscribeToPageUpdates(response);
                   return;
               }
                this.pageAsset.set(pageAsset);
            });
    }

    #subscribeToPageUpdates(response: DotCMSPageResponse) {
        this.editablePageService
            .listen(response)
            .subscribe(({ pageAsset }) => this.pageAsset.set(pageAsset));
    }
}
```

### 2. Component Registration

**Old (Alpha):**
```typescript
const DYNAMIC_COMPONENTS: DotCMSPageComponent = {
    Activity: import('./activity.component').then(c => c.ActivityComponent),
    Banner: import('./banner.component').then(c => c.BannerComponent)
};
```

**New (1.0.X):**
```typescript
import { DynamicComponentEntity } from '@dotcms/angular';

const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
    Activity: import('./activity.component').then(c => c.ActivityComponent),
    Banner: import('./banner.component').then(c => c.BannerComponent)
};
```

## New Features

### 1. DotCMSEditableText Component

**New in 1.0.X:**
```typescript
import { DotCMSEditableText } from '@dotcms/angular';

@Component({
    template: `
        <h2>
            <dotcms-editable-text
                fieldName="title"
                [contentlet]="contentlet()"
                mode="full"
                format="html"
            />
        </h2>
    `
})
```

**Props:**
- `contentlet`: The contentlet containing the editable field
- `fieldName`: Name of the field to edit
- `mode`: `'plain'` or `'full'` (with styling controls)
- `format`: `'text'` or `'html'`

### 2. DotCMSBlockEditorRenderer Component

**New in 1.0.X:**
```typescript
import { DotCMSBlockEditorRenderer } from '@dotcms/angular';

@Component({
    template: `
        <dotcms-block-editor-renderer
            [blocks]="contentlet.myBlockEditorField"
            [customRenderers]="customRenderers()"
        />
    `
})
export class MyComponent {
    readonly customRenderers = signal({
        customBlock: import('./custom-block.component').then(c => c.CustomBlockComponent)
    });
}
```

### 3. DotCMSShowWhen Directive

**New in 1.0.X:**
```typescript
import { DotCMSShowWhen, UVE_MODE } from '@dotcms/angular';

@Component({
    template: `
        <div *dotCMSShowWhen="UVE_MODE.EDIT">
            Only visible in edit mode
        </div>
        <div *dotCMSShowWhen="UVE_MODE.PREVIEW">
            Only visible in preview mode
        </div>
    `
})
```

### 4. DotCMSEditablePageService

**New in 1.0.X:**
```typescript
import { DotCMSEditablePageService } from '@dotcms/angular';

@Component({
    providers: [DotCMSEditablePageService]
})
export class PageComponent {
    private readonly editablePageService = inject(DotCMSEditablePageService);

    ngOnInit() {
        this.client.page.get({ url: '/page' }).then((pageResponse) => {
            if (getUVEState()) {
                this.editablePageService
                    .listen(pageResponse)
                    .subscribe(({ pageAsset }) => {
                        this.pageAsset.set(pageAsset);
                    });
            }
        });
    }
}
```

## Service Changes

### Page Service Migration

**Old (Alpha):**
```typescript
// Manual service implementation required
```

**New (1.0.X):**
```typescript
// Built-in EditablePageService handles page lifecycle
@Injectable()
export class EditablePageService<T extends DotCMSExtendedPageResponse> {
    initializePage(extraParams: DotCMSPageRequestParams = {}): Signal<PageState<T>> {
        // Handles routing, loading, and UVE integration automatically
    }
}
```

## Best Practices

### 1. Use Standalone Components

**Recommended approach:**
```typescript
@Component({
    selector: 'app-my-component',
    standalone: true,
    imports: [DotCMSLayoutBody, DotCMSEditableText]
})
export class MyComponent {}
```

### 2. Leverage Angular Signals

**Use signals for reactive state:**
```typescript
export class PageComponent {
    readonly pageAsset = signal<DotCMSPageAsset | null>(null);
    readonly components = signal(DYNAMIC_COMPONENTS);
    readonly isLoading = computed(() => !this.pageAsset());
}
```

### 3. Implement Proper Error Handling

```typescript
ngOnInit() {
    this.client.page
        .get({ url: '/page' })
        .then(({ pageAsset }) => {
            this.pageAsset.set(pageAsset);
        })
        .catch((error) => {
            console.error('Failed to load page:', error);
            // Handle error appropriately
        });
}
```

### 4. Use Development Mode During Development

```typescript
<dotcms-layout-body
    [page]="pageAsset()"
    [components]="components()"
    mode="development"
/>
```

This provides:
- Visual indicators for unmapped components
- Console logs for debugging
- Default components for missing mappings

## Troubleshooting

### Common Migration Issues

1. **Import Errors**
   ```typescript
   // ❌ Old imports
   import { DotcmsLayoutComponent } from '@dotcms/angular';

   // ✅ New imports
   import { DotCMSLayoutBody } from '@dotcms/angular';
   ```

2. **Client Injection Issues**
   ```typescript
   // ❌ Old approach
   private readonly client = inject(DOTCMS_CLIENT_TOKEN);

   // ✅ New approach
   private readonly client = inject(DotCMSClient);
   ```

3. **Component Registration Issues**
   ```typescript
   // ❌ Missing DynamicComponentEntity type
   const COMPONENTS = { ... };

   // ✅ Proper typing
   const COMPONENTS: { [key: string]: DynamicComponentEntity } = { ... };
   ```

4. **UVE Integration Issues**
   - Ensure `DotCMSEditablePageService` is provided at component level
   - Check that `getUVEState()` is called to detect edit mode
   - Verify UVE app configuration in dotCMS admin

### Performance Considerations

1. **Lazy Loading**: Use dynamic imports for components
2. **Signal Usage**: Leverage Angular signals for better change detection
3. **Image Optimization**: Use the provided image loader with `NgOptimizedImage`

### Getting Help

If you encounter issues during migration:

1. Check the [GitHub repository](https://github.com/dotCMS/core) for issues
2. Review the [dotCMS documentation](https://dev.dotcms.com/docs)
3. Join the [community forum](https://community.dotcms.com/)

## Conclusion

The migration to `@dotcms/angular` 1.0.X brings significant improvements in developer experience, TypeScript support, and UVE integration. While there are breaking changes, the new architecture provides a more maintainable and feature-rich foundation for your dotCMS Angular applications.

Take time to test thoroughly in a development environment before deploying to production, and consider migrating incrementally if you have a large application.
