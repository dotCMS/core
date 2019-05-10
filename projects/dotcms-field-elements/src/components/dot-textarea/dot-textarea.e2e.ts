import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-textarea', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
            <dot-textarea
                label='Address:'
                name='Address'
                value='Address'>
            </dot-textarea>`
        });

        element = await page.find('dot-textarea');
        input = await page.find('textarea');
    });

    it('should render', async () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<div class=\"dot-field__label\"><label for=\"dot-Address\">Address:</label></div><textarea id=\"dot-Address\" name=\"Address\"></textarea>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });

    it('should show Regex validation message', async () => {
        element.setProperty('regexCheck', '^[A-Za-z ]+$');
        element.setProperty('validationMessage', 'Invalid Address');

        await input.press('@');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Invalid Address');
    });

    it('should load as pristine and untouched', () => {
        expect(element.classList.contains('dot-pristine')).toBe(true);
        expect(element.classList.contains('dot-untouched')).toBe(true);
    });

    it('should mark as dirty and touched when type', async () => {
        await input.press('a');
        await page.waitForChanges();

        expect(element).toHaveClasses(['dot-dirty', 'dot-touched']);
    });

    it('should mark as invalid when value dont match REgex', async () => {
        element.setProperty('regexCheck', '^[A-Za-z ]+$');

        await input.press('@');
        await page.waitForChanges();

        expect(element).toHaveClasses(['dot-invalid']);
    });

    it('should clear value, set pristine and untouched  when input set reset', async () => {
        await input.press('A');
        element.callMethod('reset');
        await page.waitForChanges();

        expect(element).toHaveClasses(['dot-pristine', 'dot-untouched', 'dot-valid']);
        expect(await input.getProperty('value')).toBe('');
    });

    it('should mark as disabled when prop is present', async () => {
        element.setProperty('disabled', true);
        await page.waitForChanges();
        expect(await input.getProperty('disabled')).toBe(true);
    });

    it('should mark as required when prop is present', async () => {
        element.setProperty('required', 'true');
        element.setProperty('requiredMessage', 'Invalid Address');
        await page.waitForChanges();
        expect(await input.getProperty('required')).toBe(true);
    });

    describe('emit events', () => {
        let spyStatusChangeEvent: EventSpy;
        let spyValueChange: EventSpy;

        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChange = await page.spyOnEvent('valueChange');
        });

        it('should send status onBlur', async () => {
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'Address',
                status: {
                    dotPristine: true,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should mark as touched when onblur', async() => {
            await input.press('a');
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'Address',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should send status value change', async () => {
            input.press('a');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'Address',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'Address',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'Address', value: '' });
        });

        it('should emit change value', async () => {
            input.press('a');
            await page.waitForChanges();
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'Address', value: 'Addressa' });
        });
    });

    it('should render with hint', async () => {
        element.setProperty('hint', 'this is a hint');
        await page.waitForChanges();
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<div class=\"dot-field__label\"><label for=\"dot-Address\">Address:</label></div><textarea id=\"dot-Address\" name=\"Address\"></textarea><span class=\"dot-field__hint\">this is a hint</span>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });
});
