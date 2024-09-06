# dotCMS Vue JS Example

This template should help get you started developing with Vue 3 in Vite.

## What do you need?
1. A dotCMS instance or you can use https://demo.dotcms.com
2. A valid AUTH token for the target instance (see: https://auth.dotcms.com/docs/latest/rest-api-authentication#creating-an-api-token-in-the-ui)
3. Node js 18+ and npm installed
4. Terminal
5. And a code editor.


## Get the Vue Example code

You can get the vue example by checking out the project repo
```bash 
git clone -n --depth=1 --filter=tree:0 https://github.com/dotCMS/core
cd core
git sparse-checkout set --no-cone examples/vuejs
git checkout
```
The files will be found under the `examples/angular` folder

## Customize configuration

See [Vite Configuration Reference](https://vitejs.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
