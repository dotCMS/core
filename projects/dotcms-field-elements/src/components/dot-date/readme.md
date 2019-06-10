# dot-date



<!-- Auto Generated Below -->


## Properties

| Property            | Attribute            | Description                                                                                 | Type      | Default                                                |
| ------------------- | -------------------- | ------------------------------------------------------------------------------------------- | --------- | ------------------------------------------------------ |
| `disabled`          | `disabled`           | (optional) Disables field's interaction                                                     | `boolean` | `false`                                                |
| `hint`              | `hint`               | (optional) Hint text that suggest a clue of the field                                       | `string`  | `''`                                                   |
| `label`             | `label`              | (optional) Text to be rendered next to input field                                          | `string`  | `''`                                                   |
| `max`               | `max`                | (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd | `string`  | `''`                                                   |
| `min`               | `min`                | (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd | `string`  | `''`                                                   |
| `name`              | `name`               | Name that will be used as ID                                                                | `string`  | `''`                                                   |
| `required`          | `required`           | (optional) Determine if it is mandatory                                                     | `boolean` | `false`                                                |
| `requiredMessage`   | `required-message`   | (optional) Text that be shown when required is set and condition not met                    | `string`  | `'This field is required'`                             |
| `step`              | `step`               | (optional) Step specifies the legal number intervals for the input field                    | `string`  | `'1'`                                                  |
| `validationMessage` | `validation-message` | (optional) Text that be shown when min or max are set and condition not met                 | `string`  | `"The field doesn't comply with the specified format"` |
| `value`             | `value`              | Value format yyyy-mm-dd  e.g., 2005-12-01                                                   | `string`  | `''`                                                   |


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
