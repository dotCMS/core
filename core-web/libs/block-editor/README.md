# block-editor

This library was generated with [Nx](https://nx.dev).

## Running unit tests

Run `nx test block-editor` to execute the unit tests.

## Folder Structure

The top-level `folders` are `Extensions`, `Nodes`, and `Shared`. This is because we use `tiptap` which is based on `extensions` for new capabilities to the editor, and `nodes` which are the elements. The folder structure to use is as follows:

```
|-- lib
     |-- extension
       |-- bubble-menu
           |-- [+] components
           |-- [+] plugins
           |-- [+] services
           |-- [+] utils
           |-- bubble-menu.component.html
           |-- bubble-menu.component.scss
           |-- bubble-menu.component.spec.ts
           |-- bubble-menu.component.ts
           |-- bubble-menu.extension.ts
     |-- nodes
       |-- dot-image
           |-- dot-image.component.html
           |-- dot-image.component.scss
           |-- dot-image.component.spec.ts
           |-- dot-image.component.ts
           |-- dot-image.node.ts
     |-- shared
          |-- [+] components
          |-- [+] directives
          |-- [+] pipes
          |-- [+] models
          |-- [+] services
          |-- [+] utils
|-- assets
     |-- icons
          |-- heading.svg
          |-- paragraph.sv
```

### Extensions

The `ExtensionModule` contains all the `extensions` used by `tiptap`. The root of an `extension` folder contains the `extension` and the main `Angular component` it uses -if required-. There are a couple of `sub-folders` inside each `extensions` folder like: `components`, `nodes`, `plugins`, `services` and `utils`. The necessary elements for the extension are stored in these folders.

```
|-- extension
   |-- bubble-menu
       |-- [+] components
       |-- [+] nodes
       |-- [+] plugins
       |-- [+] services
       |-- [+] utils
       |-- bubble-menu.component.html
       |-- bubble-menu.component.scss
       |-- bubble-menu.component.spec.ts
       |-- bubble-menu.component.ts
       |-- bubble-menu.extension.ts
```

### Nodes

The `nodes` are simple `static components`, they do not need `services`. So, inside each node `folder`, we will have its respective `{custom-node}.node.ts` file and its `Angular Component`.

```
|-- nodes
   |-- dot-image
      |-- dot-image.component.html
      |-- dot-image.component.scss
      |-- dot-image.component.spec.ts
      |-- dot-image.component.ts
      |-- dot-image.node.ts
```

### Shared

The `SharedModule` is where any shared `components`, pipes/filters and services should go. The `SharedModule` can be imported in any other module when those items will be re-used. For example, the `components` folder, inside the `SharedModule`, contains all the “shared” components.

```
|-- shared
   |-- [+] components
   |-- [+] directives
   |-- [+] pipes
   |-- [+] models
   |-- [+] services
   |-- [+] utils
```

### Use Middleware (proxy)

Run Storybook with the proxy to the backend sending the environment var `USE_MIDDLEWARE=true` when you run it the command:

```
USE_MIDDLEWARE=true npx nx run dotcms-ui:storybook
```
