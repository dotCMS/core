# dot-contentlet-state-icon

<!-- Auto Generated Below -->


> **[DEPRECATED]** Use dot-contentlet-status-chip instead

## Properties

| Property | Attribute | Description | Type                                                                        | Default                                                                                                                      |
| -------- | --------- | ----------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `labels` | --        |             | `{ archived: string; published: string; revision: string; draft: string; }` | `{         archived: 'Archived',         published: 'Published',         revision: 'Revision',         draft: 'Draft'     }` |
| `size`   | `size`    |             | `string`                                                                    | `'16px'`                                                                                                                     |
| `state`  | --        |             | `DotContentState`                                                           | `null`                                                                                                                       |


## Dependencies

### Used by

 - [dot-card-contentlet](../../components/dot-card-contentlet)

### Depends on

- [dot-tooltip](../dot-tooltip)

### Graph
```mermaid
graph TD;
  dot-state-icon --> dot-tooltip
  dot-card-contentlet --> dot-state-icon
  style dot-state-icon fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
