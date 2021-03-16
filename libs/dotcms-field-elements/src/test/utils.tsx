import { E2EElement, E2EPage } from '@stencil/core/testing';

export const dotTestUtil = {
    getDotLabel: (page: E2EPage) => page.find('dot-label'),
    getHint: (page: E2EPage) => page.find('.dot-field__hint'),
    getErrorMessage: (page: E2EPage) => page.find('.dot-field__error-message'),
    class: {
        empty: ['dot-valid', 'dot-untouched', 'dot-pristine'],
        emptyPristineInvalid: ['dot-pristine', 'dot-untouched', 'dot-invalid'],
        emptyRequired: ['dot-required', 'dot-invalid', 'dot-touched', 'dot-dirty'],
        emptyRequiredPristine: ['dot-required', 'dot-invalid', 'dot-untouched', 'dot-pristine'],
        filled: ['dot-valid', 'dot-touched', 'dot-dirty'],
        filledRequired: ['dot-required', 'dot-valid', 'dot-touched', 'dot-dirty'],
        filledRequiredPristine: ['dot-required', 'dot-valid', 'dot-untouched', 'dot-pristine'],
        touchedPristine: ['dot-valid', 'dot-pristine', 'dot-touched']
    },
    triggerStatusChange: (
        pristine: boolean,
        touched: boolean,
        valid: boolean,
        element: E2EElement,
        isValidRange?: boolean
    ) => {
        element.triggerEvent('_statusChange', {
            detail: {
                name: '',
                status: {
                    dotPristine: pristine,
                    dotTouched: touched,
                    dotValid: valid
                },
                isValidRange: isValidRange
            }
        });
    }
};
