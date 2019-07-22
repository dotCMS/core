import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';

describe('dot-form-error-message', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form-error-message>Hello World</dot-form-error-message>`
        });
        element = await page.find('dot-form-error-message');
    });

    it('should render message', () => {
        expect(element.textContent).toBe('Hello World');
    });
});
