# dot-tags



<!-- Auto Generated Below -->


## Properties

| Property          | Attribute          | Description                                                                    | Type                                  | Default                    |
| ----------------- | ------------------ | ------------------------------------------------------------------------------ | ------------------------------------- | -------------------------- |
| `data`            | --                 | Function or array of string to get the data to use for the autocomplete search | `() => string[] \| Promise<string[]>` | `null`                     |
| `debounce`        | `debounce`         | Duraction in ms to start search into the autocomplete                          | `number`                              | `300`                      |
| `disabled`        | `disabled`         | (optional) Disables field's interaction                                        | `boolean`                             | `false`                    |
| `hint`            | `hint`             | (optional) Hint text that suggest a clue of the field                          | `string`                              | `''`                       |
| `label`           | `label`            | (optional) Text to be rendered next to input field                             | `string`                              | `''`                       |
| `name`            | `name`             | Name that will be used as ID                                                   | `string`                              | `''`                       |
| `placeholder`     | `placeholder`      | (optional) text to show when no value is set                                   | `string`                              | `''`                       |
| `required`        | `required`         | (optional) Determine if it is mandatory                                        | `boolean`                             | `false`                    |
| `requiredMessage` | `required-message` | (optional) Text that be shown when required is set and value is not set        | `string`                              | `'This field is required'` |
| `threshold`       | `threshold`        | Min characters to start search in the autocomplete input                       | `number`                              | `0`                        |
| `value`           | `value`            | Value formatted splitted with a comma, for example: tag-1,tag-2                | `string`                              | `''`                       |


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
