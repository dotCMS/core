# dot-contentlet-state-icon

<!-- Auto Generated Below -->


## Properties

| Property | Attribute | Description | Type                                                                        | Default                                                                                                                      |
| -------- | --------- | ----------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `labels` | --        |             | `{ archived: string; published: string; revision: string; draft: string; }` | `{         archived: 'Archived',         published: 'Published',         revision: 'Revision',         draft: 'Draft'     }` |
| `size`   | `size`    |             | `string`                                                                    | `'16px'`                                                                                                                     |
| `state`  | --        |             | `DotContentState`                                                           | `null`                                                                                                                       |


## Dependencies

### Depends on

- [dot-tooltip](../dot-tooltip)

### Graph
```mermaid
graph TD;
  dot-state-icon --> dot-tooltip
  style dot-state-icon fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
