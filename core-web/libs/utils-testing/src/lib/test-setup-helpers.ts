import { Component, CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';

/**
 * Common schemas to suppress Angular component/element errors in tests.
 * Useful for Jest migration where components might not be properly declared.
 *
 * Usage:
 * ```typescript
 * import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';
 *
 * TestBed.configureTestingModule({
 *   declarations: [...],
 *   schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
 * });
 * ```
 */
export const TEST_SCHEMAS = [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA];

/**
 * Mock components for common dotCMS elements that appear in templates
 * but aren't needed for most unit tests.
 */
@Component({
    selector: 'dot-global-message',
    template: '<div data-testid="mock-dot-global-message"></div>'
})
export class MockDotGlobalMessage {}

@Component({
    selector: 'dot-portlet-base',
    template: '<ng-content></ng-content>'
})
export class MockDotPortletBase {}

@Component({
    selector: 'dot-loading-indicator',
    template: '<div data-testid="mock-loading-indicator"></div>'
})
export class MockDotLoadingIndicator {}

@Component({
    selector: 'dot-spinner',
    template: '<div data-testid="mock-spinner"></div>'
})
export class MockDotSpinner {}

@Component({
    selector: 'dot-empty-state',
    template: '<div data-testid="mock-empty-state"><ng-content></ng-content></div>'
})
export class MockDotEmptyState {}

@Component({
    selector: 'dot-dialog',
    template: '<div data-testid="mock-dialog"><ng-content></ng-content></div>'
})
export class MockDotDialog {}

/**
 * Collection of commonly needed mock dotCMS components
 */
export const MOCK_DOTCMS_COMPONENTS = [
    MockDotGlobalMessage,
    MockDotPortletBase,
    MockDotLoadingIndicator,
    MockDotSpinner,
    MockDotEmptyState,
    MockDotDialog
];

/**
 * Mock components for PrimeNG elements that commonly cause issues
 */
@Component({
    selector: 'p-tabView',
    template: '<ng-content></ng-content>'
})
export class MockPTabView {}

@Component({
    selector: 'p-tabPanel',
    template: '<ng-content></ng-content>'
})
export class MockPTabPanel {}

@Component({
    selector: 'p-dropdown',
    template: '<div data-testid="mock-dropdown"></div>'
})
export class MockPDropdown {}

@Component({
    selector: 'p-button',
    template: '<button data-testid="mock-p-button"><ng-content></ng-content></button>'
})
export class MockPButton {}

/**
 * Collection of commonly needed mock PrimeNG components
 */
export const MOCK_PRIMENG_COMPONENTS = [MockPTabView, MockPTabPanel, MockPDropdown, MockPButton];

/**
 * All mock components combined
 */
export const ALL_MOCK_COMPONENTS = [...MOCK_DOTCMS_COMPONENTS, ...MOCK_PRIMENG_COMPONENTS];

/**
 * Note: For explicit control, prefer importing schemas directly in your tests:
 *
 * ```typescript
 * import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';
 *
 * TestBed.configureTestingModule({
 *   schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
 * });
 * ```
 *
 * The mock components below are available if needed for more specific test scenarios.
 */
