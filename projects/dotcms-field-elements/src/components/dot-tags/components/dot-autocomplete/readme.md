# dot-autocomplete



<!-- Auto Generated Below -->


## Properties

| Property      | Attribute     | Description                                                                    | Type                                  | Default |
| ------------- | ------------- | ------------------------------------------------------------------------------ | ------------------------------------- | ------- |
| `data`        | --            | Function or array of string to get the data to use for the autocomplete search | `() => string[] \| Promise<string[]>` | `null`  |
| `debounce`    | `debounce`    | (optional) Duraction in ms to start search into the autocomplete               | `number`                              | `300`   |
| `disabled`    | `disabled`    | (optional) Disables field's interaction                                        | `boolean`                             | `false` |
| `maxResults`  | `max-results` | (optional)  Max results to show after a autocomplete search                    | `number`                              | `0`     |
| `placeholder` | `placeholder` | (optional) text to show when no value is set                                   | `string`                              | `''`    |
| `threshold`   | `threshold`   | (optional)  Min characters to start search in the autocomplete input           | `number`                              | `0`     |


## Events

| Event       | Description | Type                      |
| ----------- | ----------- | ------------------------- |
| `enter`     |             | `CustomEvent<string>`     |
| `lostFocus` |             | `CustomEvent<FocusEvent>` |
| `selection` |             | `CustomEvent<string>`     |


----------------------------------------------

*Built with [StencilJS](https://stenciljs.com/)*
