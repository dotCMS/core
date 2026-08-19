import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';

setupZoneTestEnv({
    errorOnUnknownElements: true,
    errorOnUnknownProperties: true
});

// `@dotcms/dotcms-models` resolves the chart theme colors at MODULE IMPORT TIME: the
// `dotCMSThemeColors` const in `dot-experiments.model.ts` calls
// `getComputedStyle(document.body).getPropertyValue('--color-palette-black-op-*')`.
// Every spec that transitively imports the models library therefore evaluates it while
// jsdom has no stylesheet loaded, and jsdom can answer with `undefined`/`null` — or throw —
// for an unresolved CSS custom property. These shims must run before any spec import.
// They delegate to jsdom's real implementation so specs asserting on layout keep working,
// and only guarantee that `getPropertyValue()` always answers with a string.
const nativeGetComputedStyle = window.getComputedStyle.bind(window);
const nativeGetPropertyValue = CSSStyleDeclaration.prototype.getPropertyValue;
const emptyDeclaration = {
    getPropertyValue: () => ''
} as unknown as CSSStyleDeclaration;

CSSStyleDeclaration.prototype.getPropertyValue = function (property: string): string {
    try {
        return nativeGetPropertyValue.call(this, property) ?? '';
    } catch {
        return '';
    }
};

window.getComputedStyle = (element: Element, pseudoElement?: string | null) => {
    try {
        return nativeGetComputedStyle(element, pseudoElement);
    } catch {
        return emptyDeclaration;
    }
};

// Workaround for the following issue:
// https://github.com/jsdom/jsdom/issues/2177#issuecomment-1724971596
const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    if (!params.find((p) => p.toString().includes(jsDomCssError))) {
        originalConsoleError(...params);
    }
};
