import { E2EPage } from '@stencil/core/testing';

export const dotTestUtil = {
    getDotLabel: (page: E2EPage) => page.find('dot-label'),
    getHint: (page: E2EPage) => page.find('.dot-field__hint'),
    getErrorMessage: (page: E2EPage) => page.find('.dot-field__error-message'),
    class: {
        empty: ['dot-valid', 'dot-untouched', 'dot-pristine'],
        emptyRequired: ['dot-invalid', 'dot-touched', 'dot-dirty'],
        emptyRequiredPristine: ['dot-invalid', 'dot-untouched', 'dot-pristine'],
        filled: ['dot-valid', 'dot-touched', 'dot-dirty'],
    }
};
