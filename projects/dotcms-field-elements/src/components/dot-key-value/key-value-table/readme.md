# key-value-table



<!-- Auto Generated Below -->


## Properties

| Property       | Attribute       | Description                                                | Type                 | Default       |
| -------------- | --------------- | ---------------------------------------------------------- | -------------------- | ------------- |
| `buttonLabel`  | `button-label`  | (optional) Label for the delete button in each item list   | `string`             | `'Delete'`    |
| `disabled`     | `disabled`      | (optional) Disables all form interaction                   | `boolean`            | `false`       |
| `emptyMessage` | `empty-message` | (optional) Message to show when the list of items is empty | `string`             | `'No values'` |
| `items`        | --              | (optional) Items to render in the list of key value        | `DotKeyValueField[]` | `[]`          |


## Events

| Event    | Description                                      | Type                  |
| -------- | ------------------------------------------------ | --------------------- |
| `delete` | Emit the index of the item deleted from the list | `CustomEvent<number>` |


----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
