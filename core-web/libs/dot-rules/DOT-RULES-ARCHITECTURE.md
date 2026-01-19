# DotCMS Rules Engine - Architecture Documentation

> **Last Updated**: January 2026
> **Legacy Prefix**: `CW` (ContentWeb or ContentWizard - original project codename)

---

## Overview

The Rules Engine is a DotCMS portlet that allows users to create conditional rules that trigger actions based on visitor behavior, page context, or other conditions. It's one of the older modules in the codebase, originally built with AngularJS patterns and progressively migrated to modern Angular.

### Key Capabilities
- Create rules with multiple condition groups (AND/OR logic)
- Define actions to execute when conditions are met
- Support for geolocation-based conditions
- Server-side condition evaluation
- Integration with push publishing for rule distribution

---

## Folder Structure

```
libs/dot-rules/src/lib/
├── dot-rules.module.ts          # Main routing module
├── rule-engine.module.ts        # Feature module with providers
│
├── entry/                       # App entry point
│   ├── dot-rules.component.ts   # Root component
│   └── index.ts                 # Barrel export
│
├── features/                    # Feature components (refactored)
│   ├── index.ts                 # Barrel export
│   │
│   ├── rule-engine/             # Container + List view
│   │   ├── dot-rule-engine-container.component.ts  # Smart component (state)
│   │   └── dot-rule-engine.component.ts            # Presentational (UI)
│   │
│   ├── rule/                    # Single rule management
│   │   └── dot-rule.component.ts
│   │
│   ├── conditions/              # All condition-related components
│   │   ├── condition-group/     # Groups of conditions (AND/OR)
│   │   ├── rule-condition/      # Individual condition row
│   │   ├── serverside-condition/# Generic condition inputs
│   │   └── geolocation/         # Map picker + location condition
│   │
│   └── actions/                 # Action components
│       └── dot-rule-action.component.ts
│
├── models/                      # Data models
│   └── gcircle.model.ts         # Google Maps circle model
│
├── services/                    # Business logic & API
│   ├── api/                     # Backend API services (grouped with tests)
│   │   ├── action/              # Rule actions
│   │   │   ├── Action.ts
│   │   │   ├── Action.spec.ts
│   │   │   └── Action.it-spec.ts
│   │   ├── condition/           # Conditions
│   │   │   ├── Condition.ts
│   │   │   └── Condition.it-spec.ts
│   │   ├── condition-group/     # Condition groups
│   │   │   └── ConditionGroup.ts
│   │   ├── rule/                # Rules + models
│   │   │   ├── Rule.ts
│   │   │   ├── Rule.spec.ts
│   │   │   └── Rule.it-spec.ts
│   │   ├── bundle/              # Push publishing
│   │   │   └── bundle-service.ts
│   │   ├── serverside-field/    # Field model + form control factory
│   │   │   └── ServerSideFieldModel.ts
│   │   └── index.ts             # Barrel export
│   │
│   ├── maps/                    # Google Maps integration
│   │   ├── GoogleMapService.ts  # Google Maps API loader
│   │   └── index.ts             # Barrel export
│   │
│   ├── ui/                      # UI services
│   │   ├── dot-view-rule-service.ts # UI messaging
│   │   └── index.ts             # Barrel export
│   │
│   ├── i18n/                    # Internationalization
│   │   ├── i18n.service.ts      # I18n service
│   │   └── index.ts             # Barrel export
│   │
│   ├── models/                  # Data models
│   │   ├── base.model.ts        # BaseModel (was CwModel)
│   │   ├── event.model.ts       # ChangeEvent (was CwChangeEvent)
│   │   ├── input.model.ts       # InputDefinition, ParameterDefinition
│   │   └── index.ts             # Barrel export
│   │
│   ├── utils/                   # Utilities
│   │   ├── verify.util.ts       # Verify, LazyVerify, Check
│   │   ├── filter.util.ts       # RuleFilter (was CwFilter)
│   │   ├── key.util.ts          # KeyCode enum
│   │   └── index.ts             # Barrel export
│   │
│   └── validators/              # Form validation
│       ├── custom-validators.ts # CustomValidators
│       └── index.ts             # Barrel export
│
└── styles/
    └── rule-engine.scss         # Main styles
```

---

## Component Hierarchy

```
DotRulesComponent (entry)
└── DotRuleEngineContainerComponent (smart/container - manages state)
    └── DotRuleEngineComponent (presentational - rule list UI)
        └── DotRuleComponent (single rule - expandable accordion)
            │
            ├── DotConditionGroupComponent (condition group with AND/OR)
            │   └── DotRuleConditionComponent (single condition row)
            │       ├── DotServersideConditionComponent (generic inputs)
            │       └── DotVisitorsLocationContainerComponent (geolocation)
            │           ├── DotVisitorsLocationComponent
            │           └── DotAreaPickerDialogComponent (Google Maps)
            │
            └── DotRuleActionComponent (action row)
                └── DotServersideConditionComponent (reused for action inputs)
```

---

## Data Flow

### State Management
The rules engine uses a **container/presentational** pattern with Angular signals:

```typescript
// DotRuleEngineContainerComponent (container)
rules = signal<RuleModel[]>([]);
loading = signal(true);

// Force change detection after mutations
private refreshRules(): void {
    this.rules.update((rules) => [...rules]);
}
```

### Event Flow (Bottom-Up)
```
User Action → Child Component → Output Event → Parent Handler → Service Call → State Update
```

Example: Deleting a condition
```
1. User clicks delete → DotRuleConditionComponent
2. deleteCondition.emit() → DotConditionGroupComponent
3. deleteCondition.emit() → DotRuleComponent
4. onDeleteCondition() → DotRuleEngineContainerComponent
5. _conditionService.remove() → API call
6. refreshRules() → UI update
```

---

## Key Services

### RuleService (`services/Rule.ts`)
Central service managing rules, condition types, and action types.

```typescript
class RuleService {
    // Observable streams
    conditionTypes$: Observable<ServerSideTypeModel[]>;
    _conditionTypes: Record<string, ServerSideTypeModel>;
    _ruleActionTypes: Record<string, ServerSideTypeModel>;

    // CRUD operations
    loadRules(): Observable<RuleModel[]>;
    createRule(rule: RuleModel): Observable<RuleModel>;
    updateRule(key: string, rule: RuleModel): Observable<RuleModel>;
    deleteRule(key: string): Observable<void>;
}
```

### ServerSideFieldModel (`services/ServerSideFieldModel.ts`)
Factory for creating form controls from server-defined parameter definitions.

```typescript
class ServerSideFieldModel extends BaseModel {
    type: ServerSideTypeModel;
    parameters: Record<string, ParameterModel>;

    static createNgControl(instance, paramKey): UntypedFormControl;
    getParameterDef(key: string): ParameterDefinition;
    setParameter(key: string, value: string): void;
}
```

### I18nService (`services/i18n/i18n.service.ts`)
Handles internationalization with caching.

```typescript
class I18nService {
    get(key: string, defaultValue?: string): Observable<string>;
    getForLocale(locale: string, key: string, defaultValue?: string): Observable<string>;
}
```

---

## Input Types

The `DotServersideConditionComponent` dynamically renders inputs based on server-defined types:

| Type | Component | Description |
|------|-----------|-------------|
| `text` | `<input pInputText>` | Text input |
| `number` | `<input type="number">` | Numeric input |
| `dropdown` | `<p-select>` | Single selection |
| `restDropdown` | `<p-select>` or `<p-multiSelect>` | Options from REST API |
| `datetime` | `<p-datePicker>` | Date/time picker |

### Visibility Control (rightHandArgCount)
Comparison operators define how many inputs appear:

```typescript
// Example: "between" shows 2 date pickers, "less than" shows 1
const comparisonOptions = {
    'less_than': { rightHandArgCount: 1 },
    'greater_than': { rightHandArgCount: 1 },
    'between': { rightHandArgCount: 2 }
};
```

---

## Models

### RuleModel
```typescript
class RuleModel {
    key: string;
    name: string;
    enabled: boolean;
    fireOn: 'EVERY_PAGE' | 'ONCE_PER_VISIT' | 'ONCE_PER_VISITOR' | 'EVERY_REQUEST';
    priority: number;

    _conditionGroups: ConditionGroupModel[];
    _ruleActions: ActionModel[];

    // UI state
    _expanded: boolean;
    _saving: boolean;
    _saved: boolean;
    _errors: Record<string, string>;
}
```

### ConditionGroupModel
```typescript
class ConditionGroupModel {
    key: string;
    operator: 'AND' | 'OR';
    priority: number;
    conditions: Record<string, boolean>;
    _conditions: ConditionModel[];
}
```

### ConditionModel
```typescript
class ConditionModel extends ServerSideFieldModel {
    conditionlet: string;  // Type key
    operator: 'AND' | 'OR';
    priority: number;
}
```

### ActionModel
```typescript
class ActionModel extends ServerSideFieldModel {
    actionlet: string;  // Type key
    priority: number;
    _owningRule: RuleModel;
}
```

---

## Known Issues & Technical Debt

### 1. Legacy CSS Prefixes
- **`cw-` CSS Classes**: 147 occurrences across 9 files still use the `cw-` prefix
- These are in HTML templates and the main `rule-engine.scss` file
- Consider gradual migration to `dot-rule-` prefix

### 2. Change Detection Challenges
With Angular's OnPush strategy and signals, object mutations don't trigger updates:

```typescript
// ❌ Won't trigger change detection
rule._conditionGroups.push(newGroup);

// ✅ Correct approach
rule._conditionGroups = [...rule._conditionGroups, newGroup];
this.refreshRules();
```

### 3. Complex Input Generation
`DotServersideConditionComponent` dynamically generates inputs based on server definitions. The `argIndex` system controls visibility:

```typescript
// Input visibility based on comparison selection
[hidden]="input.argIndex !== null && input.argIndex >= rightHandArgCount"
```

### 4. Google Maps Integration
Geolocation conditions require Google Maps API. The `GoogleMapService` lazily loads the API:

```typescript
class GoogleMapService {
    mapsApi$: Observable<void>;  // Completes when API is loaded
}
```

---

## Recent Refactoring (January 2026)

### Completed
- ✅ Migrated to Angular signal-based inputs (`input()`, `output()`)
- ✅ Reorganized folder structure (`features/`, `entry/`)
- ✅ Fixed change detection issues with `refreshRules()`
- ✅ Fixed date picker visibility bug (`argIndex` initialization)
- ✅ Replaced Material Icons with PrimeNG Icons
- ✅ Added `skip:test` and `skip:lint` tags to project
- ✅ **Reorganized services structure** (full reorganization):
  - API services → `api/` (Action, Condition, ConditionGroup, Rule, bundle-service, ServerSideFieldModel)
  - Google Maps → `maps/` (GoogleMapService)
  - UI services → `ui/` (dot-view-rule-service)
  - `system/locale/` → `i18n/`
  - `util/` → `utils/` + `models/`
  - `validation/` → `validators/`
- ✅ **Renamed legacy utilities** (with deprecation aliases):
  - `CwModel` → `BaseModel`
  - `CwChangeEvent` → `ChangeEvent`
  - `CwFilter` → `RuleFilter`
  - `CwInputDefinition` → `InputDefinition`
  - `CwDropdownInputModel` → `DropdownInputModel`
- ✅ **Removed unused files**:
  - `CwAction.ts`, `CwComponent.ts` (legacy utilities)
  - `routing-private-auth-service.ts` (unused auth service)
- ✅ No legacy flex layout directives found (already migrated)

### Pending Improvements
- [ ] Rename CSS classes from `cw-*` to `dot-rule-*` (147 occurrences)
- [ ] Add comprehensive unit tests

---

## API Endpoints

The rules engine communicates with these backend endpoints:

```
GET    /api/v1/sites/{siteId}/ruleengine/rules
POST   /api/v1/sites/{siteId}/ruleengine/rules
PUT    /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}
DELETE /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}

GET    /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/conditiongroups
POST   /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/conditiongroups
PUT    /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/conditiongroups/{groupId}
DELETE /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/conditiongroups/{groupId}

GET    /api/v1/sites/{siteId}/ruleengine/conditiongroups/{groupId}/conditions
POST   /api/v1/sites/{siteId}/ruleengine/conditiongroups/{groupId}/conditions
PUT    /api/v1/sites/{siteId}/ruleengine/conditions/{conditionId}
DELETE /api/v1/sites/{siteId}/ruleengine/conditions/{conditionId}

GET    /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/ruleactions
POST   /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/ruleactions
PUT    /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/ruleactions/{actionId}
DELETE /api/v1/sites/{siteId}/ruleengine/rules/{ruleId}/ruleactions/{actionId}

GET    /api/v1/ruleengine/conditionlets        # Available condition types
GET    /api/v1/ruleengine/ruleactionlets       # Available action types
```

---

## Usage

### Importing the Module
```typescript
import { DotRulesModule } from '@dotcms/dot-rules';

@NgModule({
    imports: [DotRulesModule]
})
export class MyModule {}
```

### Route Configuration
```typescript
{
    path: 'rules',
    loadChildren: () => import('@dotcms/dot-rules').then(m => m.DotRulesModule)
}
```

---

## Contributing

When working on this module:

1. **Avoid direct object mutations** - Always create new references
2. **Call `refreshRules()`** after any data changes
3. **Initialize `argIndex: null`** in new input types
4. **Use signals** for new inputs/outputs
5. **Add to barrel exports** when creating new components
6. **Use new import paths**:
   - `services/i18n/i18n.service` for I18nService
   - `services/models/` for BaseModel, ChangeEvent, InputDefinition
   - `services/utils/` for Verify, RuleFilter, KeyCode
   - `services/validators/` for CustomValidators
