/*
 * This config is only used during development and build phase only
 * It will not be available on production
 *
 */

(function(global) {
    // ENV
    global.ENV = 'development'

    // wildcard paths
    var paths = {
        'n:*': 'thirdparty/*'
    };

    // map tells the System loader where to look for things
    var map = {
        'build': 'build',
        'rxjs': 'n:rxjs',
        '@angular': 'n:@angular',

        // @ngrx/core
        '@ngrx/core': 'n:@ngrx/core',

        // @ngrx/router
        '@ngrx/router': 'n:@ngrx/router',

        // @ngrx/router dependencies
        'path-to-regexp': 'n:path-to-regexp',
        'isarray': 'n:isarray',
        'query-string': 'n:query-string',
        'strict-uri-encode': 'n:strict-uri-encode',
        'object-assign': 'n:object-assign',

        // angular-material
        '@angular2-material': 'n:@angular2-material',

        // Other libraries
        'moment': 'n:moment'
    };

    // packages tells the System loader how to load when no filename and/or no extension
    var packages = {
        'app': {
            defaultExtension: 'js'
        },
        'build': {
            defaultExtension: 'js'
        },
        'api': {
            defaultExtension: 'js'
        },
        'view': {
            defaultExtension: 'js'
        },
        'rxjs': {
            defaultExtension: 'js'
        },

        // @ngrx/core package
        '@ngrx/core': {
            main: 'index.js',
            defaultExtension: 'js'
        },

        // @ngrx/router package
        '@ngrx/router': {
            main: 'index.js',
            defaultExtension: 'js'
        },

        // @ngrx/router dependencies
        'path-to-regexp': {
            main: 'index.js',
            defaultExtension: 'js'
        },
        'isarray': {
            main: 'index.js',
            defaultExtension: 'js'
        },
        'query-string': {
            main: 'index.js',
            defaultExtension: 'js'
        },
        'strict-uri-encode': {
            main: 'index.js',
            defaultExtension: 'js'
        },
        'object-assign': {
            main: 'index.js',
            defaultExtension: 'js'
        },
        'moment': {
            main: 'moment.js',
            defaultExtension: 'js'
        }
    };

    var packageNames = [
        "@ngrx/core",
        "@ngrx/router",
        '@angular/common',
        '@angular/compiler',
        '@angular/core',
        '@angular/http',
        '@angular/forms',
        '@angular/platform-browser',
        '@angular/platform-browser-dynamic',
        '@angular/router',
        '@angular/testing'
    ];

    // add package entries for angular packages in the form '@angular/common': { main: 'index.js', defaultExtension: 'js' }
    packageNames.forEach(function(pkgName) {
        packages[pkgName] = { main: 'index.js', defaultExtension: 'js' };
    });

    const angularMaterialPackages = [
        'core',
        'button',
        'sidenav',
        'list',
        'toolbar',
        'input',
        'icon',
        'checkbox',
        'card',
        'progress-circle',
        'radio'
    ];

    angularMaterialPackages.forEach(function(pkg) {
        packages[`@angular2-material/${pkg}`] = {main: `${pkg}.js`};
    });

    var config = {
        map: map,
        packages: packages,
        paths: paths
    };

    // filterSystemConfig - index.html's chance to modify config before we register it.
    if (global.filterSystemConfig) { global.filterSystemConfig(config); }

    System.config(config);

})(this);
