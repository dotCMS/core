# dot-key-value



<!-- Auto Generated Below -->


## Properties

| Property           | Attribute           | Description | Type      | Default     |
| ------------------ | ------------------- | ----------- | --------- | ----------- |
| `disabled`         | `disabled`          |             | `boolean` | `false`     |
| `fieldType`        | `field-type`        |             | `string`  | `undefined` |
| `hint`             | `hint`              |             | `string`  | `undefined` |
| `keyPlaceholder`   | `key-placeholder`   |             | `string`  | `undefined` |
| `label`            | `label`             |             | `string`  | `undefined` |
| `name`             | `name`              |             | `string`  | `undefined` |
| `required`         | `required`          |             | `boolean` | `undefined` |
| `requiredMessage`  | `required-message`  |             | `string`  | `undefined` |
| `saveBtnLabel`     | `save-btn-label`    |             | `string`  | `'Add'`     |
| `value`            | `value`             |             | `string`  | `undefined` |
| `valuePlaceholder` | `value-placeholder` |             | `string`  | `undefined` |


## Events

| Event          | Description | Type                               |
| -------------- | ----------- | ---------------------------------- |
| `statusChange` |             | `CustomEvent<DotFieldStatusEvent>` |
| `valueChange`  |             | `CustomEvent<DotFieldValueEvent>`  |


## Methods

### `reset() => void`

Reset properties of the filed, clear value and emit events.

#### Returns

Type: `void`




----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
