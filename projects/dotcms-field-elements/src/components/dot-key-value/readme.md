# dot-key-value



<!-- Auto Generated Below -->


## Properties

| Property           | Attribute           | Description                                                                      | Type      | Default |
| ------------------ | ------------------- | -------------------------------------------------------------------------------- | --------- | ------- |
| `disabled`         | `disabled`          | (optional) Disables field's interaction                                          | `boolean` | `false` |
| `fieldType`        | `field-type`        |                                                                                  | `string`  | `''`    |
| `hint`             | `hint`              | (optional) Hint text that suggest a clue of the field                            | `string`  | `''`    |
| `keyPlaceholder`   | `key-placeholder`   | (optional) Placeholder for the key input text in the add form                    | `string`  | `''`    |
| `label`            | `label`             | (optional) Text to be rendered next to input field                               | `string`  | `''`    |
| `name`             | `name`              | Name that will be used as ID                                                     | `string`  | `''`    |
| `required`         | `required`          | (optional) Determine if it is mandatory                                          | `boolean` | `false` |
| `requiredMessage`  | `required-message`  | (optional) Text that will be shown when required is set and condition is not met | `string`  | `''`    |
| `saveBtnLabel`     | `save-btn-label`    | (optional) Label for the add item button                                         | `string`  | `'Add'` |
| `value`            | `value`             | Value of the field                                                               | `string`  | `''`    |
| `valuePlaceholder` | `value-placeholder` | (optional) Placeholder for the value input text in the add form                  | `string`  | `''`    |


## Events

| Event          | Description | Type                               |
| -------------- | ----------- | ---------------------------------- |
| `statusChange` |             | `CustomEvent<DotFieldStatusEvent>` |
| `valueChange`  |             | `CustomEvent<DotFieldValueEvent>`  |


## Methods

### `reset() => void`

Reset properties of the field, clear value and emit events.

#### Returns

Type: `void`




----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
