import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-date-range', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChange: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-date-range
                    label='Name:'
                    min="2019-01-01"
                    max="2019-12-31"
                    name='dateRange'
                    value='2019-5-1,2019-5-8'
                    hint='this is a hint'
                    required=true
                    required-message="Required Name"
                ></dot-date-range>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        spyValueChange = await page.spyOnEvent('valueChange');
        element = await page.find('dot-date-range');
        input = await page.find('input');
    });

    it('should render', () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<dot-date-range label=\"Name:\" min=\"2019-01-01\" max=\"2019-12-31\" name=\"dateRange\" value=\"2019-5-1,2019-5-8\" hint=\"this is a hint\" required=\"true\" required-message=\"Required Name\" class=\"dot-valid dot-required hydrated dot-dirty dot-touched\"><div class=\"dot-field__label\"><label for=\"dot-dateRange\">Name:</label><span class=\"dot-field__required-mark\">*</span></div><input class=\"flatpickr-input\" id=\"dateRange\" required=\"\" type=\"text\" readonly=\"readonly\"><select><option value=\"0\">Date Presets</option><option value=\"-7\">Last Week</option><option value=\"7\">Next Week</option><option value=\"-30\">Last Month</option><option value=\"30\">Next Month</option></select><span class=\"dot-field__hint\">this is a hint</span></dot-date-range>`;
        expect(element.outerHTML).toBe(tagsRenderExpected);
    });

    it('should set default value', async() => {
        const dateValue = await input.getProperty('value');
        expect(dateValue).toBe('2019-05-01,2019-05-08');
    });

    it('should be invalid and contain "dot-invalid", dot-dirty" and "dot-touched" css classes', async() => {
        element.setProperty('value', '');
        await page.waitForChanges();
        expect(element).toHaveClasses(['dot-dirty', 'dot-touched', 'dot-invalid', 'dot-required', 'hydrated']);
    });

    it('it should not render hint', async () => {
        element.setProperty('hint', '');
        await page.waitForChanges();
        const hint = await element.find('.dot-field__hint');
        expect(hint).toBeNull();
    });

    describe('flatpickr interaction', () => {
        let calendar: E2EElement;
        beforeEach(async () => {
            input.click();
            await page.waitForChanges();
            calendar = await page.find('.flatpickr-calendar');
        });

        it('should open flatpickr when click on input', async() => {
            expect(calendar.classList.contains('open')).toBe(true);
        });

        describe('date range set', () => {
            beforeEach(async () => {
                const days = await page.findAll('.flatpickr-day');
                days[5].click();
                days[8].click();
                await page.waitForChanges();
            });

            it('should set date range value on input', async() => {
                const dateValue = await input.getProperty('value');
                expect(dateValue).toBe('2019-05-03,2019-05-06');
            });

            it('should set "dot-valid", dot-touched" and "dot-dirty" Css classes', async() => {
                expect(element).toHaveClasses(['dot-touched', 'dot-dirty', 'dot-valid']);
            });

            it('should emit "statusChange" and "valueChange"', async() => {
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'dateRange',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    }
                });
                expect(spyValueChange).toHaveReceivedEventDetail({
                    name: 'dateRange',
                    value: '2019-05-03,2019-05-06'
                });
            });
        });
    });

    it('should set date based on preset', async() => {
        const preset = 7;
        await page.select('select', preset.toString());
        await page.waitForChanges();

        const dt = new Date();
        const today = dt.toISOString().split('T')[0];
        dt.setDate(dt.getDate() + preset);
        const future = dt.toISOString().split('T')[0];

        const dateValue = await input.getProperty('value');
        expect(dateValue).toBe(`${today},${future}`);
    });

    describe('onReset', () => {
        beforeEach(async () => {
            await element.callMethod('reset');
            await page.waitForChanges();
        });

        it('should clear value', async () => {
            expect(await input.getProperty('value')).toBe('');
        });

        it('should set "dot-invalid", dot-untouched" and "dot-pristine" Css classes', async () => {
            expect(element).toHaveClasses(['dot-pristine', 'dot-untouched', 'dot-invalid']);
        });
    });

    describe('disabled', () => {
        it('should render', async() => {
            element.setProperty('disabled', true);
            await page.waitForChanges();
            // tslint:disable-next-line:max-line-length
            const tagsRenderExpected = `<dot-date-range label=\"Name:\" min=\"2019-01-01\" max=\"2019-12-31\" name=\"dateRange\" value=\"2019-5-1,2019-5-8\" hint=\"this is a hint\" required=\"true\" required-message=\"Required Name\" class=\"dot-valid dot-required hydrated dot-dirty dot-touched\"><div class=\"dot-field__label\"><label for=\"dot-dateRange\">Name:</label><span class=\"dot-field__required-mark\">*</span></div><input class=\"flatpickr-input\" id=\"dateRange\" required=\"\" type=\"text\" readonly=\"readonly\" disabled=\"\"><select disabled=\"\"><option value=\"0\">Date Presets</option><option value=\"-7\">Last Week</option><option value=\"7\">Next Week</option><option value=\"-30\">Last Month</option><option value=\"30\">Next Month</option></select><span class=\"dot-field__hint\">this is a hint</span></dot-date-range>`;
            expect(element.outerHTML).toBe(tagsRenderExpected);
        });
    });
});
