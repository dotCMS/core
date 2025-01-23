# Edit Content Bridge

A bridge library that enables form interoperability between Angular and Dojo environments within the Edit Contentlet interface.

## What it does

This bridge provides a unified API to synchronize form fields between:

-   The new Angular-based Edit Contentlet UI
-   The legacy Dojo implementation

It handles:

-   Bidirectional data sync between frameworks
-   Field value getting/setting
-   Change detection and propagation
-   Automatic cleanup

## Basic Usage

# DotFormBridge for Custom Fields

The DotFormBridge (exposed as `DotCustomFieldApi`) enables interaction with form fields in the Edit Contentlet interface.

## ⚠️ Critical: Initialize Inside Ready Handler

All custom field logic MUST be inside `DotCustomFieldApi.ready()`:

```javascript
DotCustomFieldApi.ready(() => {
    // All your custom field logic goes here
    // This ensures the bridge is ready to use
});
```

## Available Methods

Inside the ready handler, you can use these methods:

### 1. Get Field Value

```javascript
// Get value from any field
const value = DotCustomFieldApi.get('variableField');
```

### 2. Set Field Value

```javascript
// Update any field value
DotCustomFieldApi.set('variableField', 'newValue');
```

### 3. Watch Field Changes

```javascript
// React to changes in any field
DotCustomFieldApi.onChangeField('variableField', (value) => {
    console.log('Field changed to:', value);
});
```

## Example: Complete Custom Field

Here's a practical example combining all methods:

```javascript
DotCustomFieldApi.ready(() => {
    // 1. Initialize with saved value
    const savedValue = DotCustomFieldApi.get('myField');
    document.getElementById('displayValue').textContent = savedValue;

    // 2. Watch another field for changes
    DotCustomFieldApi.onChangeField('sourceField', (value) => {
        // Process the change
        const processed = processValue(value);

        // 3. Update our field
        DotCustomFieldApi.set('myField', processed);

        // Update display
        document.getElementById('displayValue').textContent = processed;
    });
});
```

## Important Notes

-   ✅ All logic MUST be inside `DotCustomFieldApi.ready()`
-   🔑 Field IDs must match content type field variables
-   🔄 Changes are automatically synced across the form
-   ⚡ The API only works in Edit Contentlet context
