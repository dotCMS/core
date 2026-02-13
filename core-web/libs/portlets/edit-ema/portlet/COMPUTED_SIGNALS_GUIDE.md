# UVE Store Computed Signals - Developer Guide

**Quick reference for using the new computed signals pattern**

---

## 🎯 Overview

The UVE store now provides computed signals for accessing pageAsset properties. This eliminates duplication and ensures a single source of truth.

---

## 📖 Available Computed Signals

All signals are prefixed with `$` to indicate they are computed/derived values.

```typescript
// Page and site
$page: Signal<DotCMSPage | null>
$site: Signal<DotCMSSite | null>

// Content structure
$containers: Signal<Record<string, Record<string, Container>> | null>
$template: Signal<DotCMSTemplate | null>
$layout: Signal<DotCMSLayout | null>

// Content mapping
$urlContentMap: Signal<Record<string, DotCMSContentlet> | null>
$numberContents: Signal<number | null>

// Context
$viewAs: Signal<{ language: any; persona?: DotPersona; visitor?: any } | null>
$vanityUrl: Signal<VanityUrl | null>

// Client integration
$clientResponse: Signal<ClientResponse | null>
```

---

## ✅ Correct Usage Patterns

### In Components

```typescript
import { DotUveStore } from '../store/dot-uve.store';

@Component({
    selector: 'dot-my-component',
    standalone: true
})
export class MyComponent {
    readonly uveStore = inject(DotUveStore);

    // ✅ CORRECT: Use computed signals
    readonly pageTitle = computed(() => {
        return this.uveStore.$page()?.title ?? 'Untitled';
    });

    readonly siteHostname = computed(() => {
        return this.uveStore.$site()?.hostname ?? '';
    });

    // ✅ CORRECT: Null-safe access
    readonly canEdit = computed(() => {
        return this.uveStore.$page()?.canEdit ?? false;
    });

    // ✅ CORRECT: Use in methods
    doSomething() {
        const page = this.uveStore.$page();
        if (page?.identifier) {
            // Use page data
        }
    }
}
```

### In Store Features

```typescript
export function withMyFeature() {
    return signalStoreFeature(
        { state: type<UVEState>() },
        withComputed((store) => ({
            // ✅ CORRECT: Build on computed signals
            $myComputed: computed(() => {
                const page = store.$page();
                const site = store.$site();

                return {
                    pageId: page?.identifier,
                    siteId: site?.identifier
                };
            })
        }))
    );
}
```

### In Templates

```html
<!-- ✅ CORRECT: Use computed signals in templates -->
<div>
    <h1>{{ uveStore.$page()?.title }}</h1>
    <p>Site: {{ uveStore.$site()?.hostname }}</p>

    @if (uveStore.$page()?.canEdit) {
        <button>Edit</button>
    }
</div>
```

---

## ❌ Avoid These Patterns

### Don't Use Old Direct Property Access

```typescript
// ❌ WRONG: Direct property access (deprecated pattern)
const page = this.uveStore.page();
const site = this.uveStore.site();

// ✅ CORRECT: Use computed signals
const page = this.uveStore.$page();
const site = this.uveStore.$site();
```

### Don't Access pageAssetResponse Directly

```typescript
// ❌ WRONG: Accessing internal state
const page = this.uveStore.pageAssetResponse()?.pageAsset?.page;

// ✅ CORRECT: Use computed signal
const page = this.uveStore.$page();
```

### Don't Mix Old and New Patterns

```typescript
// ❌ WRONG: Mixing patterns
const page = this.uveStore.page();      // Old
const site = this.uveStore.$site();     // New

// ✅ CORRECT: Consistent pattern
const page = this.uveStore.$page();     // New
const site = this.uveStore.$site();     // New
```

---

## 🔄 Migration Examples

### Example 1: Simple Property Access

```typescript
// BEFORE
readonly pageURL = computed(() => {
    const page = this.uveStore.page();
    const site = this.uveStore.site();

    if (!page?.pageURI) return '';
    return `${site?.hostname}${page.pageURI}`;
});

// AFTER
readonly pageURL = computed(() => {
    const page = this.uveStore.$page();
    const site = this.uveStore.$site();

    if (!page?.pageURI) return '';
    return `${site?.hostname}${page.pageURI}`;
});
```

### Example 2: Nested Computed

```typescript
// BEFORE
readonly canDeleteContent = computed(() => {
    const numberContents = this.uveStore.numberContents();
    const viewAs = this.uveStore.viewAs();
    const persona = viewAs?.persona;

    return numberContents > 1 || !persona || isDefaultPersona(persona);
});

// AFTER
readonly canDeleteContent = computed(() => {
    const numberContents = this.uveStore.$numberContents();
    const viewAs = this.uveStore.$viewAs();
    const persona = viewAs?.persona;

    return numberContents > 1 || !persona || isDefaultPersona(persona);
});
```

### Example 3: Component Method

```typescript
// BEFORE
buildBookmarkUrl() {
    const page = this.uveStore.page();
    const site = this.uveStore.site();

    return `/pages/${page?.identifier}?host=${site?.identifier}`;
}

// AFTER
buildBookmarkUrl() {
    const page = this.uveStore.$page();
    const site = this.uveStore.$site();

    return `/pages/${page?.identifier}?host=${site?.identifier}`;
}
```

---

## 🎨 State Updates (For Store Features Only)

**Note**: Most components should NOT update state directly. Use store methods instead.

### Correct Update Pattern

```typescript
// ✅ CORRECT: In store features only
export function withLoad(deps: WithLoadDeps) {
    return signalStoreFeature(
        withMethods((store) => ({
            loadPage: rxMethod(
                pipe(
                    switchMap(() => dotPageApiService.get(params)),
                    tap((pageAsset) => {
                        // Update single source of truth
                        deps.setPageAssetResponse({ pageAsset });

                        // Computed signals automatically update
                        // No manual property extraction needed
                    })
                )
            )
        }))
    );
}
```

### What Happens Automatically

```typescript
// When you call:
deps.setPageAssetResponse({ pageAsset });

// All computed signals automatically update:
$page() → pageAsset.page
$site() → pageAsset.site
$containers() → pageAsset.containers
$template() → pageAsset.template
// ... and all other computed signals
```

---

## 🧪 Testing

### Test Setup

```typescript
// Mock the store state
const mockPageAsset: DotCMSPageAsset = {
    page: { identifier: '123', title: 'Test Page' },
    site: { identifier: '456', hostname: 'localhost' },
    // ... other properties
};

TestBed.configureTestingModule({
    providers: [
        {
            provide: DotUveStore,
            useValue: {
                $page: signal(mockPageAsset.page),
                $site: signal(mockPageAsset.site),
                // ... other signals
            }
        }
    ]
});
```

### Test Assertions

```typescript
// ✅ CORRECT: Test computed signals
it('should display page title', () => {
    const page = component.uveStore.$page();
    expect(page?.title).toBe('Test Page');
});

// ✅ CORRECT: Test null safety
it('should handle null page gracefully', () => {
    const title = component.pageTitle();
    expect(title).toBe('Untitled'); // Default value
});
```

---

## 📚 Related Documentation

- **REFACTORING_SUMMARY.md** - Complete refactoring details
- **MIGRATION_ANALYSIS.md** - Component migration roadmap
- **withPageAsset.ts** - Source code with JSDoc comments

---

## 🆘 Common Issues

### Issue: TypeScript error "Property does not exist"

```typescript
// Error: Property '$page' does not exist
const page = this.uveStore.$page();
```

**Solution**: Make sure `withPageAsset` is included in the store composition.

### Issue: Signal returns undefined

```typescript
// Returns undefined unexpectedly
const page = this.uveStore.$page();
```

**Solution**: Check that `pageAssetResponse` has been set in the store. The page might not be loaded yet.

### Issue: Tests failing with signal errors

```typescript
// TypeError: Cannot read property of undefined
```

**Solution**: Mock the computed signals in your test setup, not the raw state properties.

---

## ✨ Benefits Summary

**Why use computed signals?**

1. ✅ **Single source of truth** - No duplication
2. ✅ **Type-safe** - Compile-time checks
3. ✅ **Auto-updating** - No manual sync needed
4. ✅ **Testable** - Easy to mock
5. ✅ **Maintainable** - Change once, update everywhere
6. ✅ **Performance** - Memoized, only recompute when needed

---

**Last Updated**: 2026-02-12
**Status**: Production Ready
