# dot-binary-text-field



<!-- Auto Generated Below -->


## Properties

| Property      | Attribute     | Description                                                                                             | Type      | Default     |
| ------------- | ------------- | ------------------------------------------------------------------------------------------------------- | --------- | ----------- |
| `accept`      | `accept`      | (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg | `string`  | `undefined` |
| `disabled`    | `disabled`    | (optional) Disables field's interaction                                                                 | `boolean` | `false`     |
| `hint`        | `hint`        | (optional) Hint text that suggest a clue of the field                                                   | `string`  | `''`        |
| `placeholder` | `placeholder` | (optional) Placeholder specifies a short hint that describes the expected value of the input field      | `string`  | `''`        |
| `required`    | `required`    | (optional) Determine if it is mandatory                                                                 | `boolean` | `false`     |
| `value`       | `value`       | Value specifies the value of the <input> element                                                        | `any`     | `null`      |


## Events

| Event        | Description | Type                              |
| ------------ | ----------- | --------------------------------- |
| `fileChange` |             | `CustomEvent<DotBinaryFileEvent>` |
| `lostFocus`  |             | `CustomEvent<void>`               |


----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
