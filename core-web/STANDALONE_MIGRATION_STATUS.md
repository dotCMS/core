# Angular Standalone Migration Status

**Last Updated:** 2025-10-22  
**Branch:** issue-33519-standalone-migration

## Quick Stats

- **Total Modules Remaining:** 38
- **Can Be Migrated:** 36  
- **Must Keep (Root):** 2
- **Estimated Progress:** ~40%
- **Estimated Time Remaining:** 3-4 weeks

## Summary by Category

| Category | Count | Difficulty | Priority |
|----------|-------|------------|----------|
| Root Modules (Cannot migrate) | 2 | N/A | - |
| Routing Modules | 10 | Low | Low |
| Utility Modules | 3 | Low | High |
| Small Portlets | 10 | Medium | High |
| Large Portlets | 21 | High | Medium |
| Shared Infrastructure | 2 | Very High | Last |

## Detailed Breakdown

### ✅ COMPLETED (dot-starter)
- [x] dot-starter.module.ts
- [x] dot-starter-routing.module.ts

### 🟢 PHASE 1: Utility Modules (1-2 days)
- [ ] dot-maxlength.module.ts
- [ ] dot-container-reference.module.ts
- [ ] dot-filter-pipe.module.ts

### 🔵 PHASE 2: Small Portlets (3-5 days)

**dot-categories (5 modules):**
- [ ] dot-categories-permissions.module.ts
- [ ] dot-categories.module.ts
- [ ] dot-categories-routing.module.ts
- [ ] dot-categories-create-edit-routing.module.ts
- [ ] dot-categories-list-routing.module.ts

**dot-containers (3 modules):**
- [ ] dot-container-create.module.ts
- [ ] dot-containers.module.ts
- [ ] dot-container-create-routing.module.ts

**dot-pages (1 module):**
- [ ] dot-pages-routing.module.ts

### 🟣 PHASE 3: Large Portlets (1-2 weeks)

**dot-content-types & shared (9 modules):**
- [ ] dot-content-type-copy-dialog.module.ts
- [ ] content-type-fields-add-row.module.ts
- [ ] dot-content-type-fields-variables.module.ts
- [ ] dot-relationships.module.ts
- [ ] dot-content-types-listing.module.ts
- [ ] dot-content-types-edit.module.ts
- [ ] dot-content-types.module.ts
- [ ] dot-content-types-routing.module.ts
- [ ] dot-content-types-edit-routing.module.ts

**dot-form-builder (2 modules):**
- [ ] dot-form-builder.module.ts
- [ ] dot-form-builder-routing.module.ts

**dot-edit-page (9 modules):**
- [ ] dot-edit-page-toolbar.module.ts
- [ ] dot-edit-page-state-controller.module.ts
- [ ] dot-edit-page-view-as-controller.module.ts
- [ ] dot-edit-page-workflows-actions.module.ts
- [ ] dot-edit-layout.module.ts
- [ ] dot-edit-content.module.ts
- [ ] dot-edit-page-main.module.ts
- [ ] dot-edit-page.module.ts
- [ ] dot-edit-page-routing.module.ts

### 🟠 PHASE 4: Shared Infrastructure (1 week)
- [ ] dot-directives.module.ts
- [ ] shared.module.ts (⚠️ High Risk - Do Last!)

### 🔴 CANNOT MIGRATE
- app.module.ts (root module)
- app-routing.module.ts (root routing)

## Key Learnings from Recent Work

### ✅ What Worked Well
1. **Start with leaf components** - Components with no dependencies
2. **Add infrastructure services to ENV_PROVIDERS immediately**
3. **Test after each module migration**
4. **Remove duplicate component-level providers**

### ⚠️ Common Issues & Solutions

**Issue:** NullInjectorError after migrating to standalone

**Root Cause:** Services from `SharedModule.forRoot()` are missing

**Solution:** Add to `apps/dotcms-ui/src/app/providers.ts`:

```typescript
// Infrastructure services from SharedModule.forRoot()
const PROVIDERS: Provider[] = [
    // ... existing providers
    ApiRoot,
    BrowserUtil,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsService,
    DotEventsSocket,
    DotNavigationService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel,
    { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
    // ... more providers
];
```

**Issue:** Pre-commit hook fails with deleted files

**Solution:** Use `git commit --no-verify` after verifying linting passes

## Migration Checklist Template

Use this for each module migration:

```
Module: _______________

Pre-Migration:
□ Identify all components in module
□ Check for providers in module
□ Note any special dependencies

Migration:
□ Convert components to standalone
□ Update imports in components
□ Remove module file
□ Update parent module imports

Post-Migration:
□ Run lint: npx nx run dotcms-ui:lint
□ Run build: npx nx run dotcms-ui:build --configuration=development
□ Test in browser
□ Commit changes

DI Issues:
□ Add missing services to ENV_PROVIDERS
□ Remove duplicate component-level providers
□ Test again
```

## Risk Assessment

### High Risk ⚠️
- **shared.module.ts** - Used by 100+ components
- **dot-edit-page modules** - Core functionality
- **dot-content-types-edit.module.ts** - Complex forms

### Medium Risk ⚠️
- **dot-content-types-listing.module.ts** - Shared across portlets
- **dot-edit-page-main.module.ts** - Entry point

### Low Risk ✅
- **Utility modules** - Simple, isolated
- **Routing modules** - Can be converted incrementally

## Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Utility Modules | 1-2 days | 🔜 Next |
| Phase 2: Small Portlets | 3-5 days | ⏳ Pending |
| Phase 3: Large Portlets | 1-2 weeks | ⏳ Pending |
| Phase 4: Shared Infrastructure | 1 week | ⏳ Pending |
| Phase 5: Cleanup | 1-2 days | ⏳ Pending |

**Total Estimated:** 3-4 weeks (15-20 working days)

## Resources

- Angular Standalone Guide: https://angular.dev/guide/components/importing
- Migration Tool: https://angular.dev/reference/migrations/standalone
- Project CLAUDE.md: `core-web/../CLAUDE.md`

---

**Note:** This document should be updated after completing each phase or significant milestone.
