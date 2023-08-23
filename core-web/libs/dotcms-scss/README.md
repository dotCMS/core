# SCSS theme and variables

dotCMS UI is running in two libraries Angular and Dojo.

In this library we unified the SCSS themes for both so they can share the variables for the design tokens:

1. Colors
2. Fonts
3. Shadows
4. Spacing

This list will continue to grow as we add more tokes and components.

## How to add a new token file

1. Create the file in the `shared` folder as partial (add `_` prefix)
2. Add the variables you need
3. Edit `/jsp/scss/_variables.scss` and `/angular/_variables.scss`
4. Add the `@use` and the `@forward`

This will make this new variables available in all the `.scss` files for Angular and JSP
