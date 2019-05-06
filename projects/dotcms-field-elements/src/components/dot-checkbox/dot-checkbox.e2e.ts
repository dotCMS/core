import { newE2EPage, E2EElement, E2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-checkbox', () => {

    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage();

        await page.setContent(`
        <dot-checkbox
            name="testName"
            label="testLabel"
            hint="testHint"
            options="valueA|1,valueB|2,valueC|3"
            value="1"
            required-message="testErrorMsg"
            required="true"
            >
        </dot-checkbox>`);
        element = await page.find('dot-checkbox');
    });

    it('renders', async () => {
        // tslint:disable-next-line:max-line-length
        const expectedMarkup = `<dot-checkbox name=\"testName\" label=\"testLabel\" hint=\"testHint\" options=\"valueA|1,valueB|2,valueC|3\" value=\"1\" required-message=\"testErrorMsg\" required=\"true\" class=\"dot-valid dot-pristine dot-untouched dot-required hydrated\"><div class=\"dot-field__label\"><label for=\"testName\">testLabel</label><span class=\"dot-field__required-mark\">*</span></div><input type=\"checkbox\" id=\"1\" value=\"1\"><div class=\"dot-field__label\"><label for=\"1\">valueA</label></div><input type=\"checkbox\" id=\"2\" value=\"2\"><div class=\"dot-field__label\"><label for=\"2\">valueB</label></div><input type=\"checkbox\" id=\"3\" value=\"3\"><div class=\"dot-field__label\"><label for=\"3\">valueC</label></div><span class=\"dot-field__hint\">testHint</span></dot-checkbox>`;
        const hint = await element.find('.dot-field__hint');
        expect(element.outerHTML).toBe(expectedMarkup);
        expect(hint).toBeTruthy();
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

    it('should be invalid, touched & dirty and the error msg should display', async () => {
        await page.click('input');
        await page.waitForChanges();
        // tslint:disable-next-line:max-line-length
        expect(element.outerHTML).toBe(`<dot-checkbox name=\"testName\" label=\"testLabel\" hint=\"testHint\" options=\"valueA|1,valueB|2,valueC|3\" value=\"1\" required-message=\"testErrorMsg\" required=\"true\" class=\"dot-required hydrated dot-invalid dot-dirty dot-touched\"><div class=\"dot-field__label\"><label for=\"testName\">testLabel</label><span class=\"dot-field__required-mark\">*</span></div><input class=\"dot-field__error\" type=\"checkbox\" id=\"1\" value=\"1\"><div class=\"dot-field__label\"><label for=\"1\">valueA</label></div><input class=\"dot-field__error\" type=\"checkbox\" id=\"2\" value=\"2\"><div class=\"dot-field__label\"><label for=\"2\">valueB</label></div><input class=\"dot-field__error\" type=\"checkbox\" id=\"3\" value=\"3\"><div class=\"dot-field__label\"><label for=\"3\">valueC</label></div><span class=\"dot-field__hint\">testHint</span><span class=\"dot-field__error-message\">testErrorMsg</span></dot-checkbox>`);
        expect(element.classList.contains('dot-invalid')).toBe(true);
        expect(element.classList.contains('dot-dirty')).toBe(true);
        expect(element.classList.contains('dot-touched')).toBe(true);
    });

    describe('Events', () => {
        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
        });

        it('should emit "statusChange" & "valueChange"', async () => {
            await page.click('input');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'testName',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: false
                }
            });
            expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                name: 'testName',
                value: ''
            });
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'testName',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: false
                }
            });
            expect(spyValueChangeEvent).toHaveReceivedEventDetail({ name: 'testName', value: '' });
        });
    });
});
