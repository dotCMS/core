# DotCMS UI
This project was created using [angular-cli](https://cli.angular.io/) it means you can use `ng` with all the commands and params.

## Minimum Requirements
1. NodeJS v8.11.3
2. NPM 6.3.0
3. Angular CLI 6.1.2

## Before start
Build the interal libraries (until we publish it to npm)

```ng build dot-layout-grid```
```ng build dot-rules```
```ng build dotcms-js```

You should see now the folders:

```
dist-lib/dot-layout-grid
dist-lib/dot-rules
dist-lib/dotcms-js
```

## Run for DotCMS
Watch and compile your files in development mode and copy it into ```dotAdmin``` folder in core.

1. ```npm run build:dev -- --output-path your/path/to/dotadmin``` (ex. ~/dev/dotcms/tomcat8/webapps/ROOT/dotAdmin)
2. Open http://localhost:8080/dotAdmin

## Run tests
`ng test`

## Publish to NPM
**You need the credentials for dotcms npm account**

### Publish a dev verison

```
$ npm run publish:dev
```

This will build a production ready app, update the ```package.json``` version using a timestamp (ex: 5.0.1-1537889821825) and publish that as latest version to npm.

### Publish a release
```
$ npm run build:prod
```

This will build a production ready app inside `dist` folder, then:

```
$ cd dist
```

Edit the `package.json` to update manually the version of the module and finally run:

```
$ npm publish
```
