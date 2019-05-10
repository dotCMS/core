import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-textfield', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChange: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `
              <dot-textfield
                    label='Name:'
                    name='fullName'
                    value='John'
                    hint='this is a hint'
                    placeholder='Enter Name'
                    regex-check='^[A-Za-z ]+$'
                    validation-message="Invalid Name"
                    required
                    required-message="Required Name"
                ></dot-textfield>`
        });

        spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        spyValueChange = await page.spyOnEvent('valueChange');
        element = await page.find('dot-textfield');
        input = await page.find('input');
    });

    it('should render', () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<div class=\"dot-field__label\"><label for=\"dot-fullName\">Name:</label><span class=\"dot-field__required-mark\">*</span></div><input id=\"dot-fullName\" placeholder=\"Enter Name\" required=\"\" type=\"text\"><span class=\"dot-field__hint\">this is a hint</span>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });

    it('should set type', async () => {
        element.setProperty('type', 'email');
        await page.waitForChanges();
        expect(await input.getProperty('type')).toBe('email');
    });

    it('should show Regex validation message', async () => {
        await input.press('@');
        await page.waitForChanges();
        const errorMessage = await page.find('.dot-field__error-message');
        expect(errorMessage.innerHTML).toBe('Invalid Name');
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
        await input.press('@');
        await page.waitForChanges();
        expect(element).toHaveClasses(['dot-invalid']);
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
        it('should mark as touched when onblur', async () => {
            await input.triggerEvent('blur');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'fullName',
                status: {
                    dotPristine: true,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should send status and value change', async () => {
            await input.press('a');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'fullName',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'fullName',
                value: 'Johna'
            });
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();
            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'fullName',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: false
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'fullName', value: '' });
        });
    });
});
