# DotCMS Rules Engine - Frontend Developer's Survival Guide

> **Welcome to the Rules Engine!** This used to be the "god-forbidden portlet" that nobody wanted to touch. It's been modernized (Angular 21, PrimeNG, Tailwind), but it's still complex. This guide will help you navigate, debug, and extend it without losing your mind.

---

## 🗺️ What Is This Thing?

The Rules Engine lets users create **if-then rules** for their website:
- **IF** visitor is from New York **AND** it's a weekday **THEN** show special banner
- **IF** user visited 3+ pages **OR** spent 5+ minutes **THEN** trigger exit popup

Think of it like a visual programming interface for conditional business logic.

---

## 📊 The 30,000 Foot View

### The Component Hierarchy (What Talks to What)

```
┌─────────────────────────────────────────────────────────┐
│ DotRulesComponent (entry point)                        │
│ └── Just a router wrapper, nothing to see here         │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ DotRuleEngineContainerComponent (THE BRAIN) 🧠          │
│ • Manages ALL state (rules, loading, saving)            │
│ • Handles ALL API calls                                 │
│ • Coordinates EVERYTHING                                │
│ • Has the dreaded refreshRules() you'll need            │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ DotRuleEngineComponent (dumb UI list)                   │
│ • Just renders the list of rules                        │
│ • "Add Rule" button                                     │
│ • That's it                                             │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ DotRuleComponent (single rule - the big one) 📋         │
│ • One expandable accordion item                         │
│ • Contains: name, enable toggle, fireOn dropdown        │
│ • Wraps all conditions and actions for ONE rule         │
│ • Lots of event handlers                                │
└─────────────────────────────────────────────────────────┘
         ↓                                ↓
┌──────────────────────┐    ┌────────────────────────────┐
│ Condition Groups     │    │ Rule Actions               │
│ (AND/OR logic)       │    │ (what happens)             │
└──────────────────────┘    └────────────────────────────┘
```

---

## 🎯 Component Glossary (What Each One Actually Does)

### 1. **DotRuleEngineContainerComponent** - The Command Center

**Location**: `features/rule-engine/dot-rule-engine-container.component.ts`

**What it does**: Everything. Seriously. This is where all the magic (and pain) happens.

**State it manages**:
```typescript
rules = signal<RuleModel[]>([]);          // All rules
loading = signal(true);                    // Loading spinner
saving = signal(false);                    // Global save state
environments = signal<IPublishEnvironment[]>([]); // Push publish targets
```

**Key methods you'll use**:

| Method | Purpose | When to Use |
|--------|---------|-------------|
| `refreshRules()` | Force UI update | After mutating any rule/condition/action object |
| `patchRule()` | Save rule changes | When rule name, enabled, fireOn changes |
| `patchCondition()` | Save condition | When condition type/params change |
| `patchAction()` | Save action | When action type/params change |
| `onCreateRule()` | Add new rule | "Add Rule" button clicked |
| `onDeleteCondition()` | Remove condition | Delete button clicked |

**The Gotcha**: This component uses **OnPush change detection + signals**. If you mutate objects directly, the UI won't update. You **must** call `refreshRules()`.

```typescript
// ❌ WRONG - UI won't update
rule._conditionGroups.push(newGroup);

// ✅ CORRECT
rule._conditionGroups.push(newGroup);
this.refreshRules(); // Creates new array reference, triggers update
```

---

### 2. **DotRuleComponent** - The Rule Card

**Location**: `features/rule/dot-rule.component.ts`

**What it does**: Renders a single rule with all its conditions and actions. It's a "smart" component that knows how to handle user interactions but delegates all state changes to the container.

**Inputs**:
```typescript
$rule = input.required<RuleModel>();              // The rule data
$ruleActionTypes = input<Record<...>>();          // Available action types
$conditionTypes = input<Record<...>>();           // Available condition types
```

**Outputs** (events it fires):
```typescript
updateName                      // User changed rule name
updateEnabledState             // User toggled on/off switch
updateExpandedState            // User expanded/collapsed rule
createConditionGroup           // User added condition group
deleteCondition                // User deleted a condition
updateRuleActionParameter      // User changed action parameter
// ... and 10 more
```

**The Pattern**: This component is a **pure event dispatcher**. It doesn't save anything itself. It just emits events that bubble up to the container.

```typescript
// User changes rule name
onFireOnChange(value: string): void {
    // Just emit, don't save
    this.updateFireOn.emit({
        type: 'RULE_UPDATE_FIRE_ON',
        payload: { rule: this.$rule(), value }
    });
}
```

**Key UI Elements**:
- **Header**: Rule name (inline editable), enabled toggle, options menu
- **Body** (when expanded): Condition groups + Actions
- **Footer**: Status indicator (Saved / Saving... / Errors)

---

### 3. **DotConditionGroupComponent** - The AND/OR Container

**Location**: `features/conditions/condition-group/dot-condition-group.component.ts`

**What it does**: Wraps a group of conditions with an AND/OR operator selector.

**Visual Structure**:
```
┌─────────────────────────────────────────────────┐
│ [Condition 1]                              [×]  │
│ ─── AND ▼ ────                                  │
│ [Condition 2]                              [×]  │
│ ─────────────                                   │
│ [+ Add Condition]                               │
└─────────────────────────────────────────────────┘
```

**Inputs**:
```typescript
$conditionGroup = input<ConditionGroupModel>(); // The group data
$conditionTypes = input<Record<...>>();         // Available types
```

**Outputs**:
```typescript
createCondition              // Add condition clicked
deleteCondition             // Condition deleted
updateConditionType         // Condition type changed
updateConditionParameter    // Parameter changed
```

**The Pattern**: It's a **pass-through component**. Events from child conditions bubble through it to the parent rule component.

---

### 4. **DotRuleConditionComponent** - Single Condition Row

**Location**: `features/conditions/rule-condition/dot-rule-condition.component.ts`

**What it does**: Renders ONE condition (e.g., "User's Country IS United States").

**Visual Structure**:
```
[Condition Type ▼] [===== parameters rendered here =====] [×]
```

**Special Cases**:
- **Visitors Location**: Shows custom map picker component
- **All other types**: Uses generic `DotServersideConditionComponent`

**The Branch Logic**:
```typescript
@if (isVisitorsLocation()) {
    <dot-visitors-location-container />  // Custom Google Maps UI
} @else {
    <dot-serverside-condition />         // Generic dynamic inputs
}
```

---

### 5. **DotServersideConditionComponent** - The Magic Input Generator 🪄

**Location**: `features/conditions/serverside-condition/dot-serverside-condition.component.ts`

**What it does**: This is the **most complex component**. It dynamically generates form inputs based on server-defined parameter definitions.

**Example**: Backend says "this condition needs 3 parameters: comparison (dropdown), country (dropdown), threshold (number)". This component reads that and renders:
```html
<p-select [options]="comparisonOptions" />
<p-select [options]="countryOptions" />
<input type="number" />
```

**How It Works**:
```typescript
// 1. Backend sends ParameterDefinition[]
const params = [
    { key: 'comparison', inputType: 'dropdown', options: ['is', 'is_not'] },
    { key: 'country', inputType: 'restDropdown', url: '/api/countries' },
    { key: 'threshold', inputType: 'number' }
];

// 2. Component generates InputConfig[]
this.inputs = [
    { name: 'comparison', type: 'dropdown', options$: of([...]) },
    { name: 'country', type: 'restDropdown', options$: http.get(...) },
    { name: 'threshold', type: 'number', control: new FormControl() }
];

// 3. Template loops through inputs
@for (input of inputs; track input.name) {
    @if (input.type === 'dropdown') {
        <p-select [options]="input.options$" />
    } @else if (input.type === 'number') {
        <input type="number" [formControl]="input.control" />
    }
}
```

**The Visibility Trick** (for date ranges):
```typescript
// "between" comparison needs 2 date inputs, "before" needs 1
const comparisonMeta = {
    'between': { rightHandArgCount: 2 },  // Shows 2 inputs
    'before': { rightHandArgCount: 1 }    // Shows 1 input
};

// Inputs marked with argIndex
inputs = [
    { name: 'comparison', type: 'dropdown' },
    { name: 'startDate', type: 'datetime', argIndex: 0 },  // Always visible
    { name: 'endDate', type: 'datetime', argIndex: 1 }     // Only if rightHandArgCount >= 2
];

// Visibility logic
input.argIndex !== null && input.argIndex >= rightHandArgCount
```

**Supported Input Types**:
- `text` → `<input pInputText>`
- `number` → `<input type="number">`
- `dropdown` → `<p-select>`
- `restDropdown` → `<p-select>` with API-loaded options
- `datetime` → `<p-datePicker>`

---

### 6. **DotRuleActionComponent** - What Happens When Rule Fires

**Location**: `features/actions/dot-rule-action.component.ts`

**What it does**: Renders ONE action (e.g., "Redirect to /promo-page").

**Visual Structure**:
```
[Action Type ▼] [===== parameters rendered here =====] [×]
```

**The Trick**: It **reuses** `DotServersideConditionComponent` to render action parameters! Actions and conditions use the same dynamic input system.

```typescript
<p-select
    [options]="typeDropdownOptions$"
    (onChange)="onTypeChange($event.value)">
</p-select>

<dot-serverside-condition
    [componentInstance]="$action()"
    (parameterValueChange)="onParameterValueChange($event)">
</dot-serverside-condition>
```

---

### 7. **DotVisitorsLocationComponent + Dialog** - Google Maps Integration

**Location**: `features/conditions/geolocation/`

**What it does**: Custom UI for "User is within X miles of Y location" condition.

**Components**:
- `DotVisitorsLocationContainerComponent` - State management
- `DotVisitorsLocationComponent` - Input fields (lat, lng, radius)
- `DotAreaPickerDialogComponent` - Google Maps dialog

**Why special**: This condition type needs a map interface. It can't use generic inputs.

**The Flow**:
```typescript
// User clicks "Select on Map"
→ Opens DotAreaPickerDialogComponent
→ Loads Google Maps API (lazy loaded)
→ User searches address / drags circle
→ Dialog emits { latitude, longitude, radius, unit }
→ Component updates condition parameters
→ Container saves to backend
```

---

## 🔄 Data Flow (The Complete Picture)

### Creating a New Rule

```
USER: Clicks "Add Rule" button
  ↓
DotRuleEngineComponent: Emits createRule event
  ↓
DotRuleEngineContainerComponent.onCreateRule()
  ├─ Creates new RuleModel with stub condition + action
  ├─ Prepends to rules array: [newRule, ...existingRules]
  └─ Updates signal: this.rules.set([...])
  ↓
UI: Re-renders with new rule at top (collapsed)
```

### Editing a Condition

```
USER: Changes condition type dropdown
  ↓
DotRuleConditionComponent: Emits updateConditionType
  ↓
DotConditionGroupComponent: Passes through (adds conditionGroup)
  ↓
DotRuleComponent: Passes through (adds rule)
  ↓
DotRuleEngineContainerComponent.onUpdateConditionType()
  ├─ Creates NEW ConditionModel (to force change detection)
  ├─ Replaces in array: group._conditions[idx] = newCondition
  ├─ Calls patchCondition()
  │   ├─ Validates condition.isValid()
  │   ├─ If new: POST to /api/.../conditions
  │   ├─ If existing: PUT to /api/.../conditions/{id}
  │   └─ Updates rule._saving / rule._saved
  └─ Calls refreshRules() to trigger UI update
  ↓
UI: Shows "Saving..." → "Saved" status
```

### The refreshRules() Mystery 🔍

**Why does it exist?**

Angular's OnPush change detection + signals don't detect **nested object mutations**:

```typescript
// This mutates a nested array but doesn't change the rules signal
this.rules()[0]._conditionGroups.push(newGroup);
// Angular: "rules signal didn't change, no re-render"

// refreshRules() forces a new reference
private refreshRules(): void {
    this.rules.update(rules => [...rules]); // New array reference!
    // Angular: "oh, rules changed, re-render!"
}
```

**When to call it**:
- After mutating `_conditionGroups`, `_conditions`, or `_ruleActions` arrays
- After changing properties on condition/action objects
- After API calls that modify rule structure
- When in doubt, call it (it's cheap)

---

## 🏗️ The Data Models (What You're Actually Working With)

### RuleModel

```typescript
class RuleModel {
    key: string;              // Backend ID (null if not saved)
    name: string;             // Rule name
    enabled: boolean;         // On/off toggle
    priority: number;         // Sort order (higher = first)
    fireOn: string;           // EVERY_PAGE | ONCE_PER_VISIT | ...

    // Nested collections (the tricky parts)
    _conditionGroups: ConditionGroupModel[];  // Array of AND/OR groups
    _ruleActions: ActionModel[];              // Array of actions

    // UI-only state (not saved to backend)
    _expanded: boolean;       // Is accordion open?
    _saving: boolean;         // Show "Saving..." indicator
    _saved: boolean;          // Show "Saved" checkmark
    _errors: Record<string, string>;  // Validation errors

    // Methods
    isPersisted(): boolean {  // Has backend ID?
        return this.key != null;
    }

    isValid(): boolean {      // Can be saved?
        return !!this.name && this.name.trim().length > 0;
    }
}
```

### ConditionGroupModel

```typescript
class ConditionGroupModel {
    key: string;              // Backend ID
    operator: 'AND' | 'OR';   // How to combine conditions
    priority: number;         // Sort order within rule

    _conditions: ConditionModel[];  // The actual conditions

    conditions: Record<string, boolean>;  // Backend format {id: true}
}
```

### ConditionModel

```typescript
class ConditionModel extends ServerSideFieldModel {
    key: string;              // Backend ID
    conditionlet: string;     // Type (e.g., "VisitorsCurrentURLConditionlet")
    operator: 'AND' | 'OR';   // How it combines with next condition
    priority: number;         // Sort order within group

    type: ServerSideTypeModel;  // Metadata (parameters, i18n, etc.)
    parameters: Record<string, { value: string, priority: number }>;

    // Methods
    setParameter(key: string, value: string): void;
    getParameterValue(key: string): string;
    isValid(): boolean;  // All required params filled?
}
```

### ActionModel

```typescript
class ActionModel extends ServerSideFieldModel {
    key: string;              // Backend ID
    actionlet: string;        // Type (e.g., "SetResponseHeaderActionlet")
    priority: number;         // Sort order

    type: ServerSideTypeModel;
    parameters: Record<string, { value: string }>;

    _owningRule: RuleModel;   // Back-reference to parent
}
```

### ServerSideTypeModel (Condition/Action Metadata)

```typescript
class ServerSideTypeModel {
    key: string;              // Unique ID (e.g., "UsersCountryConditionlet")
    i18nKey: string;          // Translation key
    _opt: { label: string, value: string };  // For dropdowns

    parameters: ParameterDefinition[];  // What inputs to show

    // Example parameter
    {
        key: 'comparison',
        inputType: 'dropdown',
        required: true,
        options: [
            { value: 'is', i18nKey: '...', rightHandArgCount: 1 },
            { value: 'between', i18nKey: '...', rightHandArgCount: 2 }
        ]
    }
}
```

---

## 🔧 Common Development Tasks

### Task 1: Adding a New Input Type

**Scenario**: Backend added a "color picker" parameter type.

**Steps**:
```typescript
// 1. Update input type union
// File: services/models/input.model.ts
export interface ParameterDefinition {
    inputType: 'text' | 'dropdown' | 'datetime' | 'color';  // Add 'color'
}

// 2. Update InputConfig interface
// File: features/conditions/serverside-condition/dot-serverside-condition.component.ts
interface InputConfig {
    type?: 'text' | 'dropdown' | 'datetime' | 'color';  // Add 'color'
    colorValue?: string;  // Add new property
}

// 3. Add input generation logic
private buildInputs(componentInstance: ServerSideFieldModel): void {
    // ... existing code ...

    if (paramDef.inputType === 'color') {
        const colorVal = componentInstance.getParameterValue(key) || '#000000';
        inputs.push({
            control: control,
            name: key,
            type: 'color',
            colorValue: colorVal,
            argIndex: null
        });
    }
}

// 4. Add template markup
// File: features/conditions/serverside-condition/dot-serverside-condition.component.html
@if (input.type === 'color') {
    <input
        type="color"
        [formControl]="input.control"
        [value]="input.colorValue"
        (change)="onInputChange($event.target.value, input)"
    />
}

// 5. Test with backend condition that has color parameter
```

### Task 2: Debugging "UI Not Updating"

**Problem**: You changed a condition parameter but the UI still shows old value.

**Checklist**:
```typescript
// 1. Did you call refreshRules()?
rule._conditionGroups[0]._conditions[0].setParameter('key', 'value');
this.refreshRules();  // ← This line!

// 2. Did you update the signal or mutate directly?
// ❌ WRONG
this.rules()[0].name = 'New Name';

// ✅ CORRECT
const updatedRules = this.rules();
updatedRules[0].name = 'New Name';
this.rules.set([...updatedRules]);

// 3. Check change detection strategy
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush  // ← Requires immutable updates
})

// 4. Use browser DevTools
const component = ng.getComponent($0);  // $0 = selected element
component.rules();  // Read signal value
ng.applyChanges($0);  // Force change detection
```

### Task 3: Adding a New Rule Property

**Scenario**: Add "description" field to rules.

**Steps**:
```typescript
// 1. Update model interfaces
// File: services/api/rule/Rule.ts
export interface IRule {
    // ... existing
    description?: string;  // Add property
}

export class RuleModel {
    // ... existing
    description: string;

    constructor(iRule: IRule) {
        Object.assign(this, iRule);
        this.description = iRule.description || '';
    }
}

// 2. Update backend transformation
static fromClientRuleTransformFn(rule: RuleModel): IRule {
    const sendRule = Object.assign({}, DEFAULT_RULE, rule);
    sendRule.description = rule.description;  // Include in API payload
    // ... rest
    return sendRule;
}

// 3. Add UI control
// File: features/rule/dot-rule.component.html
<textarea
    pInputTextarea
    [(ngModel)]="$rule().description"
    (ngModelChange)="onDescriptionChange($event)"
    placeholder="Rule description">
</textarea>

// 4. Add event handler
// File: features/rule/dot-rule.component.ts
onDescriptionChange(value: string): void {
    this.updateDescription.emit({
        type: 'RULE_UPDATE_DESCRIPTION',
        payload: { rule: this.$rule(), value }
    });
}

// Add output
readonly updateDescription = output<RuleActionEvent>();

// 5. Handle in container
// File: features/rule-engine/dot-rule-engine-container.component.ts
onUpdateDescription(event: RuleActionEvent): void {
    event.payload.rule.description = event.payload.value;
    this.patchRule(event.payload.rule, false);
}

// Wire up in template
<dot-rule
    (updateDescription)="onUpdateDescription($event)">
</dot-rule>
```

### Task 4: Fixing a Date Picker That Won't Show

**Problem**: Date inputs hidden when they should be visible.

**Debug Steps**:
```typescript
// 1. Check rightHandArgCount
// File: features/conditions/serverside-condition/dot-serverside-condition.component.ts
console.log('rightHandArgCount:', this.rightHandArgCount);
// Should be 1 for single date, 2 for date range

// 2. Check argIndex values
console.log('Inputs:', this.inputs.map(i => ({
    name: i.name,
    type: i.type,
    argIndex: i.argIndex
})));
// argIndex should be null for non-date inputs
// argIndex should be 0, 1, 2... for date inputs

// 3. Check visibility logic
// Template: features/conditions/serverside-condition/dot-serverside-condition.component.html
@if (!(input.argIndex !== null && input.argIndex >= rightHandArgCount)) {
    <!-- Input should show -->
}

// If argIndex = 1 and rightHandArgCount = 1, input is hidden (correct)
// If argIndex = 0 and rightHandArgCount = 1, input shows (correct)

// 4. Check if input was created properly
// Ensure argIndex is set to null initially:
inputs.push({
    // ...
    argIndex: null,  // ← Must be null, not undefined!
    // ...
});
```

---

### Reading the Event Flow

Pick any user action and trace backwards:

```typescript
// Example: User changes condition type

// 1. Template (where event starts)
// features/conditions/rule-condition/dot-rule-condition.component.html
<p-select
    (onChange)="onTypeChange($event.value)">
</p-select>

// 2. Component handler
// features/conditions/rule-condition/dot-rule-condition.component.ts
onTypeChange(value: string): void {
    this.updateConditionType.emit({  // Emit event up
        type: RULE_CONDITION_UPDATE_TYPE,
        payload: { condition: this.$condition(), value, index: this.$index() }
    });
}

// 3. Parent catches and re-emits (pass-through)
// features/conditions/condition-group/dot-condition-group.component.html
<dot-rule-condition
    (updateConditionType)="updateConditionType.emit($event)">
</dot-rule-condition>

// 4. Rule component catches and adds context
// features/rule/dot-rule.component.ts
onUpdateConditionType(event, conditionGroup: ConditionGroupModel): void {
    this.updateConditionType.emit({
        payload: Object.assign({ conditionGroup, rule: this.$rule() }, event.payload),
        type: RULE_CONDITION_UPDATE_TYPE
    });
}

// 5. Container handles the actual logic
// features/rule-engine/dot-rule-engine-container.component.ts
onUpdateConditionType(event: ConditionActionEvent): void {
    const condition = event.payload.condition;
    const group = event.payload.conditionGroup;
    const rule = event.payload.rule;
    const idx = event.payload.index;
    const type = this._ruleService._conditionTypes[event.payload.value];

    // Create NEW condition (forces change detection)
    const newCondition = new ConditionModel({
        _type: type,
        id: condition.key,
        operator: condition.operator,
        priority: condition.priority
    });

    group._conditions[idx] = newCondition;
    this.patchCondition(rule, group, newCondition);  // Save to API
}
```

### Common Pitfalls

**1. Forgetting `refreshRules()`**
```typescript
// This won't update the UI:
rule._conditionGroups.push(newGroup);

// Always add:
this.refreshRules();
```

**2. Using `undefined` instead of `null`**
```typescript
// This will break visibility logic:
argIndex: undefined  // ❌

// Use:
argIndex: null  // ✅
```

**3. Mutating signals directly**
```typescript
// Won't trigger updates:
this.rules()[0].name = 'New';  // ❌

// Use update/set:
this.rules.update(rules => {  // ✅
    rules[0].name = 'New';
    return [...rules];
});
```

**4. Not handling API errors**
```typescript
// API call fails silently:
this._ruleService.updateRule(id, rule).subscribe();  // ❌

// Always handle errors:
this._ruleService.updateRule(id, rule).subscribe({  // ✅
    next: () => this.ruleUpdated(rule),
    error: (e) => this.ruleUpdated(rule, { invalid: e.message })
});
```

---

## 🚑 Emergency Debugging

### The Nuclear Option (When Nothing Makes Sense)

```typescript
// Force Angular to re-render EVERYTHING
import { ApplicationRef } from '@angular/core';

constructor(private appRef: ApplicationRef) {}

// In your method:
this.appRef.tick();  // Triggers global change detection
```

### Inspecting Live State in Browser

```javascript
// Open DevTools, select a component element, then:

// Get component instance
const comp = ng.getComponent($0);

// Read signals
comp.rules();
comp.loading();

// Check specific rule
comp.rules()[0]._conditionGroups;

// Force update
comp.refreshRules();
ng.applyChanges($0);
```

### Network Debugging

```bash
# In Browser Network Tab:
# Filter: "ruleengine"

# Common endpoints you'll see:
GET    /api/v1/sites/{siteId}/ruleengine/rules              # Load all rules
POST   /api/v1/sites/{siteId}/ruleengine/rules              # Create rule
PUT    /api/v1/sites/{siteId}/ruleengine/rules/{id}         # Update rule
DELETE /api/v1/sites/{siteId}/ruleengine/rules/{id}         # Delete rule

POST   /api/v1/sites/{siteId}/ruleengine/rules/{id}/conditiongroups
PUT    /api/v1/sites/{siteId}/ruleengine/conditions/{id}
POST   /api/v1/sites/{siteId}/ruleengine/rules/{id}/ruleactions
```

**Check request payload** (PUT/POST):
- Is `parameters` object correct?
- Are required fields present?
- Is the structure matching backend expectations?

---

## 📚 File Index (Quick Reference)

### Core Components
- `features/rule-engine/dot-rule-engine-container.component.ts` - The brain, all state management
- `features/rule/dot-rule.component.ts` - Single rule card
- `features/conditions/serverside-condition/dot-serverside-condition.component.ts` - Dynamic input generator

### Services
- `services/api/rule/Rule.ts` - Rule CRUD, model definitions
- `services/api/condition/Condition.ts` - Condition CRUD
- `services/api/action/Action.ts` - Action CRUD
- `services/i18n/i18n.service.ts` - Translations

### Models
- `services/models/input.model.ts` - ParameterDefinition, InputDefinition
- `services/api/serverside-field/ServerSideFieldModel.ts` - Base for Condition/Action

### Utilities
- `services/utils/verify.util.ts` - Validation helpers
- `services/validators/custom-validators.ts` - Form validators

---

## 🎬 Next Steps

### If You Need To:

**Add a feature** → Start in `DotRuleComponent` (for UI) or `DotRuleEngineContainerComponent` (for logic)

**Fix a bug** → Use browser DevTools + check the event flow section

**Understand the data** → Read `RuleModel` class and trace it through components

**Extend input types** → Look at `DotServersideConditionComponent.buildInputs()`

**Debug save issues** → Check `patchRule()` / `patchCondition()` / `patchAction()`

---

**Good luck, and remember**: When in doubt, call `refreshRules()` 😄
