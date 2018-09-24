# DotCMS UI

## Minimum Requirements
1. NodeJS v8.11.3
2. NPM 6.3.0
3. Angular CLI 6.1.2

## Before start
Build the interal libraries (until we publish it to npm)

```ng build dot-layout-grid```

You should see now a `dist/dot-layout-grid` folder.

## Run for DotCMS
Watch and compile your files in prod mode and copy it into ```dotAdmin``` folder in core.

1. ```ng build --configuration=dotcms --output-path your/path/to/dotadmin``` (ex. ~/dev/dotcms/tomcat8/webapps/ROOT/dotAdmin)
2. Open http://localhost:8080/dotAdmin

## Run tests
`ng test`

More info [here](https://github.com/angular/angular-cli/wiki/test)

## Publish to NPM
-- Coming Soon --
