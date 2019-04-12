import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-textfield', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChanges: EventSpy;
    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-textfield
                    label='Name:'
                    name='fullName'
                    value='John'
                    hint='this is a hint'
                    placeholder='Enter Name'
                    regexcheck='^[A-Za-z ]+$'
                    regexcheckmessage="Invalid Name"
                    required
                    requiredmessage="Required Name"
                ></dot-textfield>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChanges');
        spyValueChanges = await page.spyOnEvent('valueChanges');
        element = await page.find('dot-textfield');
        input = await page.find('input');
    });

    it('should render', () => {
        const tagsRenderExpected = `<label>Name:</label><input name="fullName" type="text" placeholder="Enter Name" required=""><span class="dot-field__hint">this is a hint</span>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });

    it('should show Regex validation message', async () => {
        await input.press('@');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-meessage');
        expect(errorMessage.innerHTML).toBe('Invalid Name');
    });

    it('should load as pristine and untouched', () => {
        expect(element.classList.contains('dot-pristine')).toBe(true);
        expect(element.classList.contains('dot-untouched')).toBe(true);
    });

    xit('should mark as touched when onblur', () => {
        // TODO: Need to find the way to test the blur event.
    });

    it('should mark as dirty and touched when type', async () => {
        input.press('a');
        await page.waitForChanges();
        expect(element.classList.contains('dot-dirty')).toBe(true);
        expect(element.classList.contains('dot-touched')).toBe(true);
    });

    it('should mark as invalid when value dont match REgex', async () => {
        input.press('@');
        await page.waitForChanges();
        expect(element.classList.contains('dot-invalid')).toBe(true);
    });

    it('should clear value, set pristine and untouched  when input set reset', async () => {
        element.callMethod('reset');
        await page.waitForChanges();

        expect(element.classList.contains('dot-pristine')).toBe(true);
        expect(element.classList.contains('dot-untouched')).toBe(true);
        expect(element.classList.contains('dot-invalid')).toBe(true);
        expect(await input.getProperty('value')).toBe('');
    });

    it('should mark as disabled when prop is present', async () => {
        element.setProperty('disabled', true);
        await page.waitForChanges();
        expect(await input.getProperty('disabled')).toBe(true);
    });

    it('should mark as required when prop is present', async () => {
        expect(await input.getProperty('required')).toBe(true);
    });

    describe('emit events', () => {
        xit('should send status onBlur', async () => {
            // TODO: Need to find the way to test the blur event.
            // await page.$eval('input', (e: HTMLInputElement) => {
            //     e.blur();
            // });
        });

        it('should send status value change', async () => {
            input.press('a');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'fullName',
                dotPristine: false,
                dotTouched: true,
                dotValid: true
            });
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'fullName',
                dotPristine: true,
                dotTouched: false,
                dotValid: false
            });
            expect(spyValueChanges).toHaveReceivedEventDetail({ name: 'fullName', value: '' });
        });

        it('should emit change value', async () => {
            input.press('a');
            await page.waitForChanges();
            expect(spyValueChanges).toHaveReceivedEventDetail({ name: 'fullName', value: 'Johna' });
        });
    });
});
