/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-empty-function */

import '@testing-library/jest-dom';
import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
setupZoneTestEnv();

// Angular testing environment setup
import { getTestBed } from '@angular/core/testing';
import {
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting
} from '@angular/platform-browser-dynamic/testing';

// Mock PointerEvent
class MockPointerEvent implements Partial<PointerEvent> {
    public clientX?: number;
    public clientY?: number;
    public pointerType?: string;
    public pressure?: number;
    public relatedTarget?: EventTarget | null;

    constructor(type: string, props: PointerEventInit = {}) {
        Object.assign(this, props);
    }
}
(global as any).PointerEvent = MockPointerEvent;

/* global mocks for jsdom */
const mock = () => {
    let storage: { [key: string]: string } = {};

    return {
        getItem: (key: string) => (key in storage ? storage[key] : null),
        setItem: (key: string, value: string) => (storage[key] = value || ''),
        removeItem: (key: string) => delete storage[key],
        clear: () => (storage = {})
    };
};

Object.defineProperty(window, 'localStorage', { value: mock() });
Object.defineProperty(window, 'sessionStorage', { value: mock() });
Object.defineProperty(window, 'getComputedStyle', {
    value: () => ({
        getPropertyValue: (prop: string) => '',
        setProperty: (propertyName: string, value: string) => {}
    })
});

Object.defineProperty(document.body.style, 'transform', {
    value: () => ({
        enumerable: true,
        configurable: true
    })
});

// PrimeNG mocks
(global as any).ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
};

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

// Mock IntersectionObserver
(global as any).IntersectionObserver = class IntersectionObserver {
    constructor() {}
    observe() {
        return null;
    }
    unobserve() {
        return null;
    }
    disconnect() {
        return null;
    }
};

// Setup Angular testing environment
getTestBed().resetTestEnvironment();
getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting(), {
    teardown: { destroyAfterEach: false }
});
