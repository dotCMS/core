import { E2EPage } from '@stencil/core/testing';

export const dotTestUtil = {
    getDotLabel: (page: E2EPage) => page.find('dot-label'),
    getHint: (page: E2EPage) => page.find('.dot-field__hint'),
    getErrorMessage: (page: E2EPage) => page.find('.dot-field__error-message'),
    class: {
        empty: ['dot-valid', 'dot-untouched', 'dot-pristine'],
        emptyRequired: ['dot-required', 'dot-invalid', 'dot-touched', 'dot-dirty'],
        emptyRequiredPristine: ['dot-required', 'dot-invalid', 'dot-untouched', 'dot-pristine'],
        filled: ['dot-valid', 'dot-touched', 'dot-dirty'],
        filledRequired: ['dot-required', 'dot-valid', 'dot-touched', 'dot-dirty'],
        filledRequiredPristine: ['dot-required', 'dot-valid', 'dot-untouched', 'dot-pristine']
    }
};
