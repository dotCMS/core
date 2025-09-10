import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

const originalConsoleError = console.error;

// To avoid the error: Error: Could not parse CSS stylesheet
// https://stackoverflow.com/questions/69906136/console-error-error-could-not-parse-css-stylesheet
console.error = function (...data) {
    if (
        typeof data[0]?.toString === 'function' &&
        data[0].toString().includes('Error: Could not parse CSS stylesheet')
    )
        return;
    originalConsoleError(...data);
};
