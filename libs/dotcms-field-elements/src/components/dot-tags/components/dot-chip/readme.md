# dot-chip

<!-- Auto Generated Below -->


## Properties

| Property      | Attribute      | Description                                      | Type      | Default    |
| ------------- | -------------- | ------------------------------------------------ | --------- | ---------- |
| `deleteLabel` | `delete-label` | (optional) Delete button's label                 | `string`  | `'Delete'` |
| `disabled`    | `disabled`     | (optional) If is true disabled the delete button | `boolean` | `false`    |
| `label`       | `label`        | Chip's label                                     | `string`  | `''`       |


## Events

| Event    | Description | Type                  |
| -------- | ----------- | --------------------- |
| `remove` |             | `CustomEvent<String>` |


## Dependencies

### Used by

 - [dot-tags](../..)

### Graph
```mermaid
graph TD;
  dot-tags --> dot-chip
  style dot-chip fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
