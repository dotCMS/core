import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-date', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChange: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-date
                    label="Date:"
                    name="date01"
                    value="2019-01-20"
                    hint="date hint"
                    required
                    required-message="Required Date"
                    validation-message="Invalid Date Range"
                    min="2019-01-01"
                    max="2019-10-30"
                    step="2"
                ></dot-date>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        spyValueChange = await page.spyOnEvent('valueChange');
        element = await page.find('dot-date');
        input = await page.find('input');
    });

    it('should render', () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<div class=\"dot-field__label\"><label for=\"date01\">Date:</label><span class=\"dot-field__required-mark\">*</span></div><input id=\"date01\" required=\"\" type=\"date\" min=\"2019-01-01\" max=\"2019-10-30\" step=\"2\"><span class=\"dot-field__hint\">date hint</span>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });

    it('should load as pristine and untouched', () => {
        expect(element.classList.contains('dot-pristine')).toBe(true);
        expect(element.classList.contains('dot-untouched')).toBe(true);
    });

    it('should be valid, touched & dirty ', async () => {
        await input.press('2');
        await page.waitForChanges();
        expect(element.classList.contains('dot-valid')).toBe(true);
        expect(element.classList.contains('dot-dirty')).toBe(true);
        expect(element.classList.contains('dot-touched')).toBe(true);
    });

    it('it should not render hint', async () => {
        element.setProperty('hint', '');
        await page.waitForChanges();
        const hint = await element.find('.dot-field__hint');
        expect(hint).toBeNull();
    });

    it('it should have required as false', async () => {
        element.setProperty('required', 'false');
        await page.waitForChanges();
        const required = await element.getProperty('required');
        expect(required).toBeFalsy();
    });

    it('should show invalid range validation message', async () => {
        element.setProperty('value', '2015-10-01');
        await input.press('2');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Invalid Date Range');
    });

    describe('emit events', () => {
        it('should send status onBlur', async () => {
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'date01',
                status: {
                    dotPristine: true,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should mark as touched when onblur', async () => {
            await input.press('2');
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'date01',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should send status value change', async () => {
            await input.press('2');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'date01',
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
                name: 'date01',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: false
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'date01', value: '' });
        });

        it('should emit change value', async () => {
            await input.press('2');
            await page.waitForChanges();
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'date01',
                value: '2019-02-20'
            });
        });
    });
});
