import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

import { provideZoneChangeDetection } from '@angular/core';

setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });

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

// jsdom implements no `matchMedia`, and PrimeNG's Overlay reads it while deciding whether it is
// modal — so any spec that opens one (the paginator's rows-per-page panel, a popover) throws from
// inside the framework before it can assert anything. Stubbed rather than worked around in each
// spec, because the answer is the same everywhere: this is a desktop admin screen, so no media
// query matches.
if (typeof window !== 'undefined' && !window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: (query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: () => undefined,
            removeListener: () => undefined,
            addEventListener: () => undefined,
            removeEventListener: () => undefined,
            dispatchEvent: () => false
        })
    });
}

// Workaround for the following issue:
// https://github.com/jsdom/jsdom/issues/2177#issuecomment-1724971596
const originalConsoleError = console.error;
const jsDomCssError = 'Error: Could not parse CSS stylesheet';
console.error = (...params) => {
    if (!params.find((p) => p.toString().includes(jsDomCssError))) {
        originalConsoleError(...params);
    }
};
