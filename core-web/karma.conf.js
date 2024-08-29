// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const { constants } = require('karma');

module.exports = () => {
    return {
        basePath: '',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-coverage'),
            require('karma-spec-reporter'),
            require('karma-junit-reporter'),
            require('@angular-devkit/build-angular/plugins/karma')
        ],
        client: {
            clearContext: true, // leave Jasmine Spec Runner output visible in browser
            captureConsole: true
        },
        coverageReporter: {
            dir: '../../target/core-web-reports',
            subdir: '.',
            file: 'TEST-dotcms-ui.lcov',
            reporters: [{ type: 'lcovonly' }]
        },
        reporters: ['junit', 'spec'],
        port: 9876,
        colors: true,
        logLevel: constants.LOG_INFO,
        autoWatch: true,
        browsers: ['ChromeHeadlessCI'],
        customLaunchers: {
            ChromeHeadlessCI: {
                base: 'ChromeHeadless',
                flags: ['--no-sandbox']
            }
        },
        junitReporter: {
            subdir: '.',
            useBrowserName: false,
            outputDir: '../../target/core-web-reports',
            outputFile: 'TEST-dotcms-ui.xml'
        },
        summaryReporter: {
            // 'failed', 'skipped' or 'all'
            show: 'failed',
            // Limit the spec label to this length
            specLength: 50,
            // Show an 'all' column as a summary
            overviewColumn: true,
            // Show a list of test clients, 'always', 'never' or 'ifneeded'
            browserList: 'always',
            // Use custom symbols to indicate success and failure
            symbols: { success: 'o', failure: 'x' }
        },
        specReporter: {
            failFast: true,
            suppressPassed: false,
            suppressSkipped: true
        },
        singleRun: true,
        browserDisconnectTimeout: 20000
    };
};
