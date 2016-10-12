/*
 * This config is only used during development and build phase only
 * It will not be available on production
 *
 */

(function(global) {
    // ENV
    global.ENV = 'development';

    // wildcard paths
    var paths = {
        'n:*': 'thirdparty/*'
    };

    // map tells the System loader where to look for things
    var map = {
        'lodash': 'n:lodash/lodash.js',
        'build': 'build',
        'rxjs': 'n:rxjs',
        'moment': 'n:moment',

        // angular bundles
        '@angular/core': 'n:@angular/core/bundles/core.umd.js',
        '@angular/common': 'n:@angular/common/bundles/common.umd.js',
        '@angular/compiler': 'n:@angular/compiler/bundles/compiler.umd.js',
        '@angular/platform-browser': 'n:@angular/platform-browser/bundles/platform-browser.umd.js',
        '@angular/platform-browser-dynamic': 'n:@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.js',
        '@angular/http': 'n:@angular/http/bundles/http.umd.js',
        '@angular/router': 'n:@angular/router/bundles/router.umd.js',
        '@angular/forms': 'n:@angular/forms/bundles/forms.umd.js',
        '@angular/material': 'n:@angular/material/material.umd.js',

        // angular-material
        '@angular2-material': 'n:@angular2-material',

        // PrimeNG Components
        'primeng': 'n:primeng',
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
        'moment': {
            main: 'moment.js',
            defaultExtension: 'js'
        },
        'primeng': {
            defaultExtension: 'js'
        },
    };

    var config = {
        map: map,
        packages: packages,
        paths: paths
    };

    // filterSystemConfig - index.html's chance to modify config before we register it.
    if (global.filterSystemConfig) { global.filterSystemConfig(config); }

    System.config(config);

})(this);
