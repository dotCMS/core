# eslint-plugin-github

## Installation

```sh
$ npm install --save-dev eslint eslint-plugin-github
```

## Setup

Add `github` to your list of plugins in your ESLint config.

JSON ESLint config example:

```json
{
  "plugins": ["github"]
}
```

Extend the configs you wish to use.

JSON ESLint config example:

```json
{
  "extends": ["plugin:github/recommended"]
}
```

The available configs are:

- `internal`
  - Rules useful for github applications.
- `browser`
  - Useful rules when shipping your app to the browser.
- `recommended`
  - Recommended rules for every application.
- `typescript`
  - Useful rules when writing TypeScript.

### Rules

- [Array Foreach](./docs/rules/array-foreach.md)
- [Async Currenttarget](./docs/rules/async-currenttarget.md)
- [Async Preventdefault](./docs/rules/async-preventdefault.md)
- [Authenticity Token](./docs/rules/authenticity-token.md)
- [Get Attribute](./docs/rules/get-attribute.md)
- [JS Class Name](./docs/rules/js-class-name.md)
- [No Blur](./docs/rules/no-blur.md)
- [No D None](./docs/rules/no-d-none.md)
- [No Dataset](./docs/rules/no-dataset.md)
- [No Implicit Buggy Globals](./docs/rules/no-implicit-buggy-globals.md)
- [No Inner HTML](./docs/rules/no-inner-html.md)
- [No InnerText](./docs/rules/no-innerText.md)
- [No Then](./docs/rules/no-then.md)
- [No Useless Passive](./docs/rules/no-useless-passive.md)
- [Prefer Observers](./docs/rules/prefer-observers.md)
- [Require Passive Events](./docs/rules/require-passive-events.md)
- [Unescaped HTML Literal](./docs/rules/unescaped-html-literal.md)
