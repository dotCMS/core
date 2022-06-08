# dot-card-view



<!-- Auto Generated Below -->


## Properties

| Property | Attribute | Description | Type                      | Default     |
| -------- | --------- | ----------- | ------------------------- | ----------- |
| `items`  | --        |             | `DotCardContentletItem[]` | `[]`        |
| `value`  | `value`   |             | `string`                  | `undefined` |


## Events

| Event       | Description | Type               |
| ----------- | ----------- | ------------------ |
| `cardClick` |             | `CustomEvent<any>` |
| `selected`  |             | `CustomEvent<any>` |


## Methods

### `clearValue() => Promise<void>`



#### Returns

Type: `Promise<void>`



### `getValue() => Promise<DotContentletItem[]>`



#### Returns

Type: `Promise<DotContentletItem[]>`




## Dependencies

### Depends on

- [dot-card-contentlet](../../components/dot-card-contentlet)

### Graph
```mermaid
graph TD;
  dot-card-view --> dot-card-contentlet
  dot-card-contentlet --> dot-card
  dot-card-contentlet --> dot-contentlet-thumbnail
  dot-card-contentlet --> dot-tooltip
  dot-card-contentlet --> dot-state-icon
  dot-card-contentlet --> dot-badge
  dot-card-contentlet --> dot-contentlet-lock-icon
  dot-card-contentlet --> dot-context-menu
  dot-contentlet-thumbnail --> dot-contentlet-icon
  dot-state-icon --> dot-tooltip
  style dot-card-view fill:#f9f,stroke:#333,stroke-width:4px
```

----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
