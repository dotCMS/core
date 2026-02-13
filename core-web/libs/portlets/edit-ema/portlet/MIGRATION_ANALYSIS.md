# UVE Store Migration Analysis

**Date**: 2026-02-12
**Scope**: Refactoring store properties to use computed signals from `withPageAsset`

## Executive Summary

Analyzed all TypeScript files in the edit-ema portlet to identify components accessing the 8 store properties:
- `page`, `site`, `containers`, `template`, `viewAs`, `vanityUrl`, `urlContentMap`, `numberContents`

**Key Findings**:
- **100+ direct property accesses** across 15 main files
- **Migration complexity**: Medium (mostly straightforward computed signal conversions)
- **Estimated effort**: 20-30 hours total

---

## Property Usage Breakdown

### 1. `page()` - 40+ occurrences ⭐⭐⭐ HIGH COMPLEXITY
**Most accessed property** - Used for page metadata, permissions, and content operations

**Primary consumers:**
- `edit-ema-editor.component.ts` (8 occurrences) - Page URLs, protocol/hostname
- `dot-ema-shell.component.ts` (10 occurrences) - Menu items, SEO params, permissions
- `withEditor.ts` (5 occurrences) - Page data transformations, iframe URLs
- `dot-uve-toolbar.component.ts` (3 occurrences) - Workflow actions, translation

**Usage patterns:**
```typescript
// Direct property access
this.uveStore.page().pageURI

// Null-safe access
this.uveStore.page()?.canRead ?? false

// Nested computed
computed(() => this.uveStore.page().inode)
```

### 2. `viewAs()` - 15+ occurrences ⭐⭐⭐ HIGH COMPLEXITY
**ViewAs context** - Language, persona, variant information

**Primary consumers:**
- `withEditor.ts` (4 occurrences) - Number contents calculation
- `withUVEToolbar.ts` (6 occurrences) - Persona selector, variant handling
- `withView.ts` (4 occurrences) - View transformation logic
- `dot-ema-shell.component.ts` (2 occurrences) - Language ID for SEO

**Usage patterns:**
```typescript
// Nested property access
store.viewAs()?.language.id

// Persona checks
viewAs?.persona?.identifier === DEFAULT_PERSONA.identifier

// Variant detection
!getIsDefaultVariant(store.viewAs()?.variantId)
```

### 3. `site()` - 12 occurrences ⭐⭐ MEDIUM COMPLEXITY
**Site context** - Hostname, identifier, site-specific operations

**Primary consumers:**
- `edit-ema-editor.component.ts` (3 occurrences) - Site hostname/identifier
- `dot-ema-shell.component.ts` (2 occurrences) - SEO params site ID
- `dot-uve-toolbar.component.ts` (2 occurrences) - Bookmark URL construction
- `dot-uve-actions-handler.service.ts` (1 occurrence) - Reorder menu

### 4. `containers()` - 8 occurrences ⭐⭐ MEDIUM COMPLEXITY
**Container structure** - Contentlet operations and data building

**Primary consumers:**
- `edit-ema-editor.component.ts` (1 occurrence) - Finding contentlet in structure
- `withEditor.ts` (2 occurrences) - Content type records
- `withLayout.ts` (1 occurrence) - Layout data transformation

### 5. `urlContentMap()` - 5 occurrences ⭐ LOW COMPLEXITY
**URL content mapping** - URL-to-content resolution

**Primary consumers:**
- `edit-ema-editor.component.ts` (2 occurrences) - Navigation URL resolution
- `dot-ema-shell.component.ts` (1 occurrence) - Save page event
- `withUVEToolbar.ts` (2 occurrences) - Info display

### 6. `template()` - 5 occurrences ⭐ LOW COMPLEXITY
**Template metadata** - Layout permissions and configuration

**Primary consumers:**
- `dot-ema-shell.component.ts` (2 occurrences) - Menu item disabled state
- `withLayout.ts` (1 occurrence) - Layout data transformation
- `withSave.ts` (1 occurrence) - Save operations

### 7. `vanityUrl()` - 2 occurrences ⭐ LOW COMPLEXITY
**Vanity URL handling** - Optional URL rewriting

**Primary consumers:**
- `withEditor.ts` (1 occurrence) - Iframe URL computation

### 8. `numberContents()` - 1 occurrence ⭐ LOW COMPLEXITY
**Content count** - Used for deletion permissions

**Primary consumers:**
- `withEditor.ts` (1 occurrence) - Allow content delete computed

---

## Migration Strategy

### Phase 1: Foundation (COMPLETED ✅)
- [x] Create `withPageAsset` feature with computed signals
- [x] Add to store composition
- [x] Analyze component usage patterns

### Phase 2: Eliminate Store Duplication (IN PROGRESS 🔄)
- [ ] Refactor `withLoad` to remove manual property extraction
- [ ] Refactor `withSave` to remove manual property extraction
- [ ] Update `withLayout` if needed

### Phase 3: Component Migration (FUTURE)
**Migration order** (simple → complex):

1. **Low-Impact Properties** (4-6 hours)
   - `numberContents` → `$numberContents` (1 occurrence)
   - `vanityUrl` → `$vanityUrl` (2 occurrences)
   - `template` → `$template` (5 occurrences)

2. **Medium-Impact Properties** (8-12 hours)
   - `containers` → `$containers` (8 occurrences)
   - `urlContentMap` → `$urlContentMap` (5 occurrences)
   - `site` → `$site` (12 occurrences)

3. **High-Impact Properties** (10-15 hours)
   - `viewAs` → `$viewAs` (15+ occurrences)
   - `page` → `$page` (40+ occurrences)

### Phase 4: Cleanup
- [ ] Remove duplicate top-level properties from state
- [ ] Rename client state properties (remove "GraphQL" prefix)
- [ ] Update time machine to track `pageAsset`
- [ ] Update all test files
- [ ] Documentation updates

---

## Edge Cases & Considerations

### 1. Computed Signal Dependencies
Many components use nested computed that depend on these properties.

**Migration pattern:**
```typescript
// BEFORE
readonly $pageURL = computed((): string => {
    const site = this.uveStore.site();
    const page = this.uveStore.page();
    // ...
});

// AFTER
readonly $pageURL = computed((): string => {
    const site = this.uveStore.$site();
    const page = this.uveStore.$page();
    // ...
});
```

### 2. Null Safety Patterns
Must preserve all null-safe operators and fallback values:
```typescript
this.uveStore.$page()?.canRead ?? false
```

### 3. Cross-Feature Dependencies
Multiple features access the same properties - need coordinated migration:
- `page()` used in: withEditor, withSave, withUVEToolbar, withView
- Migrate feature-by-feature, update all internal references simultaneously

### 4. Test File Updates
**10+ spec files affected** - need comprehensive test updates

---

## Risk Assessment

### Low Risk ✅
- Properties with <5 usages
- Simple data access patterns
- Well-isolated components

### Medium Risk ⚠️
- Properties with 5-15 usages
- Cross-feature dependencies
- Complex computed chains

### High Risk 🚨
- `page()` with 40+ usages
- Deep integration across features
- Critical path operations (URL building, permissions)

**Recommendation**: Use progressive rollout strategy, starting with low-risk properties.

---

## Timeline Estimate

**Total estimated effort**: 20-30 hours
- Phase 1 (Foundation): ✅ COMPLETE
- Phase 2 (Duplication): 🔄 IN PROGRESS (4-6 hours)
- Phase 3 (Component migration): 18-24 hours
- Phase 4 (Cleanup & testing): 3-5 hours

---

## Files to Modify

### Store Features
- [x] `with-page-asset.ts` (new)
- [ ] `with-load.ts`
- [ ] `with-save.ts`
- [ ] `with-layout.ts`
- [ ] `with-client.ts`
- [ ] `withEditor.ts`
- [ ] `withUVEToolbar.ts`
- [ ] `withView.ts`

### Components
- [ ] `edit-ema-editor.component.ts`
- [ ] `dot-ema-shell.component.ts`
- [ ] `dot-uve-toolbar.component.ts`
- [ ] Various toolbar subcomponents

### Services
- [ ] `dot-uve-actions-handler.service.ts`

### Tests
- [ ] All spec files (10+ files)

---

**Last Updated**: 2026-02-12
**Status**: Phase 2 in progress (refactoring withLoad and withSave)
