# dot-input-calendar

<!-- Auto Generated Below -->


## Properties

| Property   | Attribute  | Description                                                                           | Type      | Default |
| ---------- | ---------- | ------------------------------------------------------------------------------------- | --------- | ------- |
| `disabled` | `disabled` | (optional) Disables field's interaction                                               | `boolean` | `false` |
| `max`      | `max`      | (optional) Max, maximum value that the field will allow to set, expect a Date Format  | `string`  | `''`    |
| `min`      | `min`      | (optional) Min, minimum value that the field will allow to set, expect a Date Format. | `string`  | `''`    |
| `name`     | `name`     | Name that will be used as ID                                                          | `string`  | `''`    |
| `required` | `required` | (optional) Determine if it is mandatory                                               | `boolean` | `false` |
| `step`     | `step`     | (optional) Step specifies the legal number intervals for the input field              | `string`  | `'1'`   |
| `type`     | `type`     | type specifies the type of <input> element to display                                 | `string`  | `''`    |
| `value`    | `value`    | Value specifies the value of the <input> element                                      | `string`  | `''`    |


## Events

| Event           | Description | Type                                       |
| --------------- | ----------- | ------------------------------------------ |
| `_statusChange` |             | `CustomEvent<DotInputCalendarStatusEvent>` |
| `_valueChange`  |             | `CustomEvent<DotFieldValueEvent>`          |


## Methods

### `reset() => Promise<void>`

Reset properties of the field, clear value and emit events.

#### Returns

Type: `Promise<void>`




## Dependencies

### Used by

 - [dot-date](../dot-date)
 - [dot-date-time](../dot-date-time)
 - [dot-time](../dot-time)

### Graph
```mermaid
graph TD;
  dot-date --> dot-input-calendar
  dot-date-time --> dot-input-calendar
  dot-time --> dot-input-calendar
  style dot-input-calendar fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
