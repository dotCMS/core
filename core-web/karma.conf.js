// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const { join } = require('path');
const { constants } = require('karma');

module.exports = () => {
    return {
        basePath: '',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-html-reporter'),
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-coverage'),
            require('@angular-devkit/build-angular/plugins/karma'),
            require('karma-junit-reporter')
        ],
        client: {
            clearContext: false // leave Jasmine Spec Runner output visible in browser
        },
        coverageReporter: {
            dir: join(__dirname, './coverage'),
            subdir: '.',
            reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'junit' }]
        },
        reporters: ['progress', 'kjhtml', 'html', 'junit'],
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
        htmlReporter: {
            namedFiles: true,
            reportName: 'report'
        },
        junitReporter: {
            outputDir: join(__dirname, 'target', 'surefire-reports'), // results will be saved as $outputDir/$browserName.xml
            outputFile: 'TEST-karma.xml'
        },
        singleRun: true,
        browserDisconnectTimeout: 20000
    };
};
