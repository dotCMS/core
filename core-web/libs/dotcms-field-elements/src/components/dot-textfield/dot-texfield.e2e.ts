import { E2EElement, E2EPage, newE2EPage, EventSpy } from '@stencil/core/testing';

import { dotTestUtil } from '../../utils';

describe('dot-textfield', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-textfield></dot-textfield>`
        });

        element = await page.find('dot-textfield');
        input = await page.find('input');
    });

    describe('render CSS classes', () => {
        it('should be valid, untouched & pristine on load', async () => {
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should be valid, touched & dirty when filled', async () => {
            await input.press('a');
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        it('should have touched but pristine on blur', async () => {
            await input.triggerEvent('blur');
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
        });

        describe('required', () => {
            beforeEach(async () => {
                element.setProperty('required', 'true');
            });

            it('should be valid, untouched & pristine and required when filled on load', async () => {
                element.setProperty('value', 'ab');
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
            });

            it('should be valid, touched & dirty and required when filled', async () => {
                await input.press('a');
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
            });

            it('should be invalid, untouched, pristine and required when empty on load', async () => {
                element.setProperty('value', '');
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
            });

            it('should be invalid, touched, dirty and required when valued is cleared', async () => {
                element.setProperty('value', 'a');
                await page.waitForChanges();
                await input.press('Backspace');
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
            });
        });
    });

    describe('@Props', () => {
        describe('dot-attr', () => {
            it('should set value correctly', async () => {
                page = await newE2EPage({
                    html: `<dot-textfield dotplaceholder="test"></dot-textfield>`
                });
                await page.waitForChanges();
                input = await page.find('input');
                expect(input.getAttribute('placeholder')).toBe('test');
            });
        });

        describe('value', () => {
            it('should set value correctly', async () => {
                element.setProperty('value', 'hi');
                await page.waitForChanges();
                expect(await input.getProperty('value')).toBe('hi');
            });
            it('should render and not break when is a unexpected value', async () => {
                element.setProperty('value', { test: true });
                await page.waitForChanges();
                expect(await input.getProperty('value')).toBe('[object Object]');
            });
        });

        describe('name', () => {
            it('should render with valid id name', async () => {
                element.setProperty('name', 'text01');
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-text01');
            });

            it('should render when is a unexpected value', async () => {
                element.setProperty('name', { input: 'text01' });
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-object-object');
            });

            it('should set name prop in dot-label', async () => {
                element.setProperty('name', 'text01');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('name')).toBe('text01');
            });
        });

        describe('label', () => {
            it('should set label prop in dot-label', async () => {
                element.setProperty('label', 'Name:');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('label')).toBe('Name:');
            });
        });

        describe('placeholder', () => {
            it('should set placeholder correctly', async () => {
                element.setProperty('placeholder', 'Test');
                await page.waitForChanges();
                expect(input.getAttribute('placeholder')).toBe('Test');
            });
        });

        describe('hint', () => {
            it('should set hint correctly and set aria attribute', async () => {
                element.setProperty('hint', 'Test');
                await page.waitForChanges();
                expect((await dotTestUtil.getHint(page)).innerText).toBe('Test');
                expect(input.getAttribute('aria-describedby')).toBe('hint-test');
            });

            it('should not render hint and do not set aria attribute', async () => {
                expect(await dotTestUtil.getHint(page)).toBeNull();
                expect(input.getAttribute('aria-describedby')).toBeNull();
            });

            it('should not break hint with invalid value', async () => {
                element.setProperty('hint', { test: 'hint' });
                await page.waitForChanges();
                expect((await dotTestUtil.getHint(page)).innerText).toBe('[object Object]');
            });
        });

        describe('required', () => {
            it('should render required attribute with invalid value', async () => {
                element.setProperty('required', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeDefined();
            });

            it('should not render required attribute', async () => {
                element.setProperty('required', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeNull();
            });

            it('should render required attribute for the do-tlabel', async () => {
                element.setProperty('required', 'true');
                await page.waitForChanges();
                const label = await dotTestUtil.getDotLabel(page);
                expect(label.getAttribute('label')).toBeDefined();
            });
        });

        describe('requiredMessage', () => {
            it('should show default value of requiredMessage', async () => {
                element.setProperty('required', 'true');
                await input.press('a');
                await input.press('Backspace');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                    'This field is required'
                );
            });

            it('should show requiredMessage', async () => {
                element.setProperty('required', 'true');
                element.setProperty('requiredMessage', 'Test');
                await input.press('a');
                await input.press('Backspace');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('Test');
            });

            it('should not render requiredMessage', async () => {
                await page.waitForChanges();
                expect(await dotTestUtil.getErrorMessage(page)).toBe(null);
            });

            it('should not render and not break with with invalid value', async () => {
                element.setProperty('required', 'true');
                element.setProperty('requiredMessage', { test: 'hi' });
                await input.press('a');
                await input.press('Backspace');
                await page.waitForChanges();
                expect(await dotTestUtil.getErrorMessage(page)).toBeNull();
            });
        });

        describe('regexCheck', () => {
            it('should set correct value when valid regexCheck', async () => {
                element.setAttribute('regex-check', '[0-9]*');
                await page.waitForChanges();
                expect(await element.getProperty('regexCheck')).toBe('[0-9]*');
            });

            it('should set empty value when invalid regexCheck', async () => {
                element.setAttribute('regex-check', '[*');
                await page.waitForChanges();
                expect(await element.getProperty('regexCheck')).toBe('');
            });
        });

        describe('validationMessage', () => {
            it('should show default value of validationMessage', async () => {
                element.setProperty('regexCheck', '[0-9]');
                await input.press('a');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                    "The field doesn't comply with the specified format"
                );
            });

            it('should render validationMessage', async () => {
                element.setProperty('regexCheck', '[0-9]');
                element.setProperty('validationMessage', 'Test');
                await input.press('a');
                await page.waitForChanges();
                expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('Test');
            });

            it('should not render validationMessage whe value is valid', async () => {
                await input.press('a');
                await page.waitForChanges();
                expect(await dotTestUtil.getErrorMessage(page)).toBeNull();
            });
        });

        describe('disabled', () => {
            it('should render disabled attribute', async () => {
                element.setProperty('disabled', 'true');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });

            it('should not render disabled attribute', async () => {
                element.setProperty('disabled', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeNull();
            });

            it('should render disabled attribute with invalid value', async () => {
                element.setProperty('disabled', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });
        });

        describe('type', () => {
            it('should set value to text on default correctly', async () => {
                await page.waitForChanges();
                expect(input.getAttribute('type')).toBe('text');
            });

            it('should set value correctly', async () => {
                element.setProperty('type', 'email');
                await page.waitForChanges();
                expect(input.getAttribute('type')).toBe('email');
            });

            it('should render and not break when is a unexpected value and set default(text)', async () => {
                element.setProperty('type', { test: true });
                await page.waitForChanges();
                expect(input.getAttribute('type')).toBe('text');
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
        });

        describe('status and value change', () => {
            it('should display on wrapper not valid css classes when loaded when required and no value set', async () => {
                page = await newE2EPage({
                    html: `
                <dot-form>
                    <dot-textfield required="true" ></dot-textfield>
                </dot-form>`
                });
                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });

            it('should send status and value change', async () => {
                await input.press('a');
                await page.waitForChanges();
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: false,
                        dotTouched: true,
                        dotValid: true
                    }
                });
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: 'a'
                });
            });

            it('should emit status and value on Reset', async () => {
                await element.callMethod('reset');
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: true
                    }
                });
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    value: ''
                });
            });
        });

        describe('status change', () => {
            it('should mark as touched when onblur', async () => {
                await input.triggerEvent('blur');
                await page.waitForChanges();

                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: '',
                    status: {
                        dotPristine: true,
                        dotTouched: true,
                        dotValid: true
                    }
                });
            });
        });
    });
});
