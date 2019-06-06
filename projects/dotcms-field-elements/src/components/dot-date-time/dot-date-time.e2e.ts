import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

xdescribe('dot-date-time', () => {
    let page: E2EPage;
    let element: E2EElement;
    let dateInput: E2EElement;
    let timeInput: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChange: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-date-time
                    label="Date and Time:"
                    name="dt01"
                    value="2019-01-20 11:10:00"
                    hint="date time hint"
                    required
                    required-message="Required Date Time"
                    validation-message="Value out of range"
                    min="2000-01-01 01:00:00"
                    max="2020-10-30 23:30:59"
                    step="2,10"
                ></dot-date-time>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        spyValueChange = await page.spyOnEvent('valueChange');
        element = await page.find('dot-date-time');
        dateInput = await page.find('dot-input-calendar[type="date"] input');
        timeInput = await page.find('dot-input-calendar[type="time"] input');
    });

    it('should render', () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<dot-date-time label=\"Date and Time:\" name=\"dt01\" value=\"2019-01-20 11:10:00\" hint=\"date time hint\" required=\"\" required-message=\"Required Date Time\" validation-message=\"Value out of range\" min=\"2000-01-01 01:00:00\" max=\"2020-10-30 23:30:59\" step=\"2,10\" class=\"dot-valid dot-pristine dot-untouched dot-required hydrated\"><div class=\"dot-field__label\"><label for=\"dot-dt01\">Date and Time:</label><span class=\"dot-field__required-mark\">*</span></div><dot-input-calendar type=\"date\" required-message=\"Required Date Time\" validation-message=\"Value out of range\" class=\"hydrated\"><input id=\"dot-dt01-date\" required=\"\" type=\"date\" min=\"2000-01-01\" max=\"2020-10-30\" step=\"2\"></dot-input-calendar><dot-input-calendar type=\"time\" required-message=\"Required Date Time\" validation-message=\"Value out of range\" class=\"hydrated\"><input id=\"dot-dt01-time\" required=\"\" type=\"time\" min=\"01:00:00\" max=\"23:30:59\" step=\"10\"></dot-input-calendar><span class=\"dot-field__hint\">date time hint</span></dot-date-time>`;
        expect(element.outerHTML).toBe(tagsRenderExpected);
    });

    it('should be valid, touched & dirty on date change', async () => {
        await dateInput.press('2');
        await page.waitForChanges();
        expect(element).toHaveClasses(['dot-valid', 'dot-dirty', 'dot-touched']);
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

    it('should show invalid range validation message, because date', async () => {
        await dateInput.press('Tab');
        await dateInput.press('Tab');
        await dateInput.press('2');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Value out of range');
    });

    it('should show invalid range validation message, because time', async () => {
        element.setProperty('value', '2010-10-01 23:45:30');
        await timeInput.press('0');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Value out of range');
    });

    describe('emit events', () => {
        it('should mark as touched when onblur', async () => {
            await timeInput.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'dt01',
                status: {
                    dotPristine: true,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should send status and value change and stop dot-input-calendar events', async () => {
            const evt_statusChange = await page.spyOnEvent('_statusChange');
            const evt_valueChange = await page.spyOnEvent('_valueChange');
            await dateInput.press('2');
            await timeInput.press('5');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'dt01',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'dt01',
                value: '2019-02-20 05:10:00'
            });
            expect(evt_statusChange.length).toEqual(0);
            expect(evt_valueChange.length).toEqual(0);
        });

        it('should emit status and value on Reset', async () => {
            await element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'dt01',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: false
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'dt01', value: '' });
            expect(spyValueChange.length).toEqual(1);
            expect(spyStatusChangeEvent.length).toEqual(1);
        });

        it('should not emit value until both inputs are filled', async () => {
            await element.callMethod('reset');
            await dateInput.press('2');
            await page.waitForChanges();
            expect(spyValueChange.length).toEqual(1);
            expect(spyStatusChangeEvent.length).toEqual(1);
        });
    });
});
