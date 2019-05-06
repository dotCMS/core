import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-time', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChange: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-time
                    label="Time:"
                    name="time01"
                    value="18:30:30"
                    hint="Time hint"
                    required
                    required-message="Required Time"
                    validation-message="Time out of range"
                    min="06:00:00"
                    max="22:00:00"
                    step="10"
                ></dot-time>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        spyValueChange = await page.spyOnEvent('valueChange');
        element = await page.find('dot-time');
        input = await page.find('input');
    });

    it('should render', () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<dot-time label=\"Time:\" name=\"time01\" value=\"18:30:30\" hint=\"Time hint\" required=\"\" required-message=\"Required Time\" validation-message=\"Time out of range\" min=\"06:00:00\" max=\"22:00:00\" step=\"10\" class=\"dot-valid dot-pristine dot-untouched dot-required hydrated\"><div class=\"dot-field__label\"><label for=\"time01\">Time:</label><span class=\"dot-field__required-mark\">*</span></div><dot-input-calendar type=\"time\" required-message=\"Required Time\" validation-message=\"Time out of range\" class=\"hydrated\"><input id=\"time01\" required=\"\" type=\"time\" min=\"06:00:00\" max=\"22:00:00\" step=\"10\"></dot-input-calendar><span class=\"dot-field__hint\">Time hint</span></dot-time>`;
        expect(element.outerHTML).toBe(tagsRenderExpected);
    });

    it('should be valid, touched and dirty on value change', async () => {
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
        element.setProperty('value', '01:00:00');
        await input.press('2');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Time out of range');
    });

    describe('emit events', () => {
        it('should mark as touched when onblur', async () => {
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'time01',
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
            await input.press('2');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'time01',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'time01',
                value: '14:30:30'
            });
            expect(evt_statusChange.events).toEqual([]);
            expect(evt_valueChange.events).toEqual([]);
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'time01',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: false
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'time01', value: '' });
        });
    });
});
