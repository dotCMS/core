import { newE2EPage, E2EElement, E2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-select', () => {

    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage();

        await page.setContent(`
        <dot-select
            name="testName"
            label="testLabel"
            hint="testHint"
            options="|,valueA|1,valueB|2"
            value="2"
            required-message="testErrorMsg"
            required="true"
            >
        </dot-select>`);
        element = await page.find('dot-select');
    });

    it('renders', async () => {
        // tslint:disable-next-line:max-line-length
        const expectedMarkup = `<dot-select name=\"testName\" label=\"testLabel\" hint=\"testHint\" options=\"|,valueA|1,valueB|2\" value=\"2\" required-message=\"testErrorMsg\" required=\"true\" class=\"dot-valid dot-pristine dot-untouched dot-required hydrated\"><div class=\"dot-field__label\"><label for=\"testName\">testLabel</label><span class=\"dot-field__required-mark\">*</span></div><select id=\"testName\"><option value=\"\"></option><option value=\"1\">valueA</option><option value=\"2\">valueB</option></select><span class=\"dot-field__hint\">testHint</span></dot-select>`;
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
        await page.select('select', '');
        await page.waitForChanges();
        // tslint:disable-next-line:max-line-length
        expect(element.outerHTML).toBe(`<dot-select name=\"testName\" label=\"testLabel\" hint=\"testHint\" options=\"|,valueA|1,valueB|2\" value=\"2\" required-message=\"testErrorMsg\" required=\"true\" class=\"dot-required hydrated dot-invalid dot-dirty dot-touched\"><div class=\"dot-field__label\"><label for=\"testName\">testLabel</label><span class=\"dot-field__required-mark\">*</span></div><select class=\"dot-field__error\" id=\"testName\"><option value=\"\"></option><option value=\"1\">valueA</option><option value=\"2\">valueB</option></select><span class=\"dot-field__hint\">testHint</span><span class=\"dot-field__error-message\">testErrorMsg</span></dot-select>`);
    });

    describe('Events', () => {
        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
        });

        it('should emit "statusChange" & "valueChange"', async () => {
            await page.select('select', '1');
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'testName',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                name: 'testName',
                value: '1'
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
