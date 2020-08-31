import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';

describe('dot-error-message', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-error-message>Hello World</dot-error-message>`
        });
        element = await page.find('dot-error-message');
    });

    it('should render message', () => {
        expect(element.textContent).toBe('Hello World');
    });
});
