# dot-binary-upload-button

<!-- Auto Generated Below -->


## Properties

| Property      | Attribute      | Description                                                                                             | Type      | Default     |
| ------------- | -------------- | ------------------------------------------------------------------------------------------------------- | --------- | ----------- |
| `accept`      | `accept`       | (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg | `string`  | `undefined` |
| `buttonLabel` | `button-label` | (optional) Text that be shown in the browse file button                                                 | `string`  | `''`        |
| `disabled`    | `disabled`     | (optional) Disables field's interaction                                                                 | `boolean` | `false`     |
| `name`        | `name`         | Name that will be used as ID                                                                            | `string`  | `''`        |
| `required`    | `required`     | (optional) Determine if it is mandatory                                                                 | `boolean` | `false`     |


## Events

| Event        | Description | Type                              |
| ------------ | ----------- | --------------------------------- |
| `fileChange` |             | `CustomEvent<DotBinaryFileEvent>` |


## Dependencies

### Used by

 - [dot-binary-file](..)

### Graph
```mermaid
graph TD;
  dot-binary-file --> dot-binary-upload-button
  style dot-binary-upload-button fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
