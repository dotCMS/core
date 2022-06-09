# dot-time



<!-- Auto Generated Below -->


## Properties

| Property            | Attribute            | Description                                                                                | Type      | Default                                                |
| ------------------- | -------------------- | ------------------------------------------------------------------------------------------ | --------- | ------------------------------------------------------ |
| `disabled`          | `disabled`           | (optional) Disables field's interaction                                                    | `boolean` | `false`                                                |
| `hint`              | `hint`               | (optional) Hint text that suggest a clue of the field                                      | `string`  | `''`                                                   |
| `label`             | `label`              | (optional) Text to be rendered next to input field                                         | `string`  | `''`                                                   |
| `max`               | `max`                | (optional) Max, maximum value that the field will allow to set. Format should be  hh:mm:ss | `string`  | `''`                                                   |
| `min`               | `min`                | (optional) Min, minimum value that the field will allow to set. Format should be hh:mm:ss  | `string`  | `''`                                                   |
| `name`              | `name`               | Name that will be used as ID                                                               | `string`  | `''`                                                   |
| `required`          | `required`           | (optional) Determine if it is mandatory                                                    | `boolean` | `false`                                                |
| `requiredMessage`   | `required-message`   | (optional) Text that be shown when required is set and condition not met                   | `string`  | `'This field is required'`                             |
| `step`              | `step`               | (optional) Step specifies the legal number intervals for the input field                   | `string`  | `'1'`                                                  |
| `validationMessage` | `validation-message` | (optional) Text that be shown when min or max are set and condition not met                | `string`  | `"The field doesn't comply with the specified format"` |
| `value`             | `value`              | Value format hh:mm:ss e.g., 15:22:00                                                       | `string`  | `''`                                                   |


## Events

| Event             | Description | Type                               |
| ----------------- | ----------- | ---------------------------------- |
| `dotStatusChange` |             | `CustomEvent<DotFieldStatusEvent>` |
| `dotValueChange`  |             | `CustomEvent<DotFieldValueEvent>`  |


## Methods

### `reset() => Promise<void>`

Reset properties of the field, clear value and emit events.

#### Returns

Type: `Promise<void>`




## Dependencies

### Depends on

- [dot-label](../dot-label)
- [dot-input-calendar](../dot-input-calendar)

### Graph
```mermaid
graph TD;
  dot-time --> dot-label
  dot-time --> dot-input-calendar
  style dot-time fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
