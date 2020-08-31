# dot-binary-file



<!-- Auto Generated Below -->


## Properties

| Property               | Attribute                  | Description                                                                                             | Type      | Default                                                |
| ---------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------- | --------- | ------------------------------------------------------ |
| `URLValidationMessage` | `u-r-l-validation-message` | (optional) Text that be shown when the URL is not valid                                                 | `string`  | `'The specified URL is not valid'`                     |
| `accept`               | `accept`                   | (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg | `string`  | `''`                                                   |
| `buttonLabel`          | `button-label`             | (optional) Text that be shown in the browse file button                                                 | `string`  | `'Browse'`                                             |
| `disabled`             | `disabled`                 | (optional) Disables field's interaction                                                                 | `boolean` | `false`                                                |
| `errorMessage`         | `error-message`            | (optional) Text that be shown in the browse file button                                                 | `string`  | `''`                                                   |
| `hint`                 | `hint`                     | (optional) Hint text that suggest a clue of the field                                                   | `string`  | `''`                                                   |
| `label`                | `label`                    | (optional) Text to be rendered next to input field                                                      | `string`  | `''`                                                   |
| `maxFileLength`        | `max-file-length`          | (optional) Set the max file size limit                                                                  | `string`  | `''`                                                   |
| `name`                 | `name`                     | Name that will be used as ID                                                                            | `string`  | `''`                                                   |
| `placeholder`          | `placeholder`              | (optional) Placeholder specifies a short hint that describes the expected value of the input field      | `string`  | `'Drop or paste a file or url'`                        |
| `previewImageName`     | `preview-image-name`       | (optional) Name of the file uploaded                                                                    | `string`  | `''`                                                   |
| `previewImageUrl`      | `preview-image-url`        | (optional) URL of the file uploaded                                                                     | `string`  | `''`                                                   |
| `required`             | `required`                 | (optional) Determine if it is mandatory                                                                 | `boolean` | `false`                                                |
| `requiredMessage`      | `required-message`         | (optional) Text that be shown when required is set and condition not met                                | `string`  | `'This field is required'`                             |
| `validationMessage`    | `validation-message`       | (optional) Text that be shown when the Regular Expression condition not met                             | `string`  | `"The field doesn't comply with the specified format"` |


## Events

| Event          | Description | Type                               |
| -------------- | ----------- | ---------------------------------- |
| `statusChange` |             | `CustomEvent<DotFieldStatusEvent>` |
| `valueChange`  |             | `CustomEvent<DotFieldValueEvent>`  |


## Methods

### `clearValue() => void`

Clear value of selected file, when the endpoint fails.

#### Returns

Type: `void`



### `reset() => void`

Reset properties of the field, clear value and emit events.

#### Returns

Type: `void`




----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
