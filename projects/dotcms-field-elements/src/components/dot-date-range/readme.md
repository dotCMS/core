# dot-date-range



<!-- Auto Generated Below -->


## Properties

| Property          | Attribute          | Description                                                                             | Type                                 | Default                                                                                                                                                                                                                                                                                                                                                                                        |
| ----------------- | ------------------ | --------------------------------------------------------------------------------------- | ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `disabled`        | `disabled`         | (optional) Disables field's interaction                                                 | `boolean`                            | `false`                                                                                                                                                                                                                                                                                                                                                                                        |
| `displayFormat`   | `display-format`   | (optional) Date format used by the field when displayed                                 | `string`                             | `'Y-m-d'`                                                                                                                                                                                                                                                                                                                                                                                      |
| `hint`            | `hint`             | (optional) Hint text that suggest a clue of the field                                   | `string`                             | `''`                                                                                                                                                                                                                                                                                                                                                                                           |
| `label`           | `label`            | (optional) Text to be rendered next to input field                                      | `string`                             | `''`                                                                                                                                                                                                                                                                                                                                                                                           |
| `max`             | `max`              | (optional) Max value that the field will allow to set                                   | `string`                             | `''`                                                                                                                                                                                                                                                                                                                                                                                           |
| `min`             | `min`              | (optional) Min value that the field will allow to set                                   | `string`                             | `''`                                                                                                                                                                                                                                                                                                                                                                                           |
| `name`            | `name`             | Name that will be used as ID                                                            | `string`                             | `'daterange'`                                                                                                                                                                                                                                                                                                                                                                                  |
| `presetLabel`     | `preset-label`     | (optional) Text to be rendered next to presets field                                    | `string`                             | `'Presets'`                                                                                                                                                                                                                                                                                                                                                                                    |
| `presets`         | --                 | (optional) Array of date presets formatted as [{ label: 'PRESET_LABEL', days: NUMBER }] | `{ label: string; days: number; }[]` | `[         {             label: 'Date Presets',             days: 0         },         {             label: 'Last Week',             days: -7         },         {             label: 'Next Week',             days: 7         },         {             label: 'Last Month',             days: -30         },         {             label: 'Next Month',             days: 30         }     ]` |
| `required`        | `required`         | (optional) Determine if it is needed                                                    | `boolean`                            | `false`                                                                                                                                                                                                                                                                                                                                                                                        |
| `requiredMessage` | `required-message` | (optional) Text that be shown when required is set and condition not met                | `string`                             | `'This field is required'`                                                                                                                                                                                                                                                                                                                                                                     |
| `value`           | `value`            | (optional) Value formatted with start and end date splitted with a comma                | `string`                             | `''`                                                                                                                                                                                                                                                                                                                                                                                           |


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
