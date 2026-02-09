import { E2EPage, E2EElement, newE2EPage, EventSpy } from '@stencil/core/testing';

import { dotTestUtil } from '../../utils';

describe('dot-key-value', () => {
    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    const getForm = () => page.find('key-value-form');
    const getList = () => page.find('key-value-table');

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-key-value></dot-key-value>`
        });
        element = await page.find('dot-key-value');
        await page.waitForChanges();
    });

    describe('css classes', () => {
        it('should have empty', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should have empty required pristine', async () => {
            element.setProperty('required', true);
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
        });

        it('should have empty required touched when all items is removed', async () => {
            element.setProperty('value', 'key|value,llave|valor');
            element.setProperty('required', true);
            const list = await getList();
            list.triggerEvent('delete', { detail: 0 });
            list.triggerEvent('delete', { detail: 0 });
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
        });

        it('should have filled', async () => {
            const form = await getForm();
            form.triggerEvent('add', {
                detail: {
                    key: 'some',
                    value: 'test'
                }
            });

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        it('should have filled required', async () => {
            element.setProperty('required', true);
            const form = await getForm();
            form.triggerEvent('add', {
                detail: {
                    key: 'some',
                    value: 'test'
                }
            });

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have filled required pristine', async () => {
            element.setProperty('required', true);
            element.setProperty('value', 'key|value,key2|value2');

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
        });

        it('should have filled required touched when item is added', async () => {
            element.setProperty('required', true);
            const form = await getForm();
            form.triggerEvent('add', {
                detail: {
                    key: 'some',
                    value: 'test'
                }
            });
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have filled required touched when one item is removed', async () => {
            element.setProperty('value', 'key|value,llave|valor');
            element.setProperty('required', true);
            const list = await getList();
            list.triggerEvent('delete', { detail: 0 });
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have touched but pristine', async () => {
            const form = await getForm();
            form.triggerEvent('lostFocus', {});
            await page.waitForChanges();

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
        });
    });

    describe('@Props', () => {
        describe('key-value-form attrs', () => {
            it('should pass down valid props', async () => {
                element.setAttribute('form-add-button-label', 'Button to the label');
                element.setAttribute('form-key-placeholder', 'Key Placeholder');
                element.setAttribute('form-value-placeholder', 'Value Placeholder');
                element.setAttribute('form-key-label', 'Key Label');
                element.setAttribute('form-value-label', 'Value Label');

                await page.waitForChanges();

                const form = await getForm();
                expect(form.getAttribute('add-button-label')).toBe('Button to the label');
                expect(form.getAttribute('key-placeholder')).toBe('Key Placeholder');
                expect(form.getAttribute('value-placeholder')).toBe('Value Placeholder');
                expect(form.getAttribute('key-label')).toBe('Key Label');
                expect(form.getAttribute('value-label')).toBe('Value Label');
            });

            it('should pass down empty props', async () => {
                await page.waitForChanges();
                const form = await getForm();
                // internal default
                expect(form.getAttribute('add-button-label')).toBe('Add');
                expect(form.getAttribute('key-placeholder')).toBe('');
                expect(form.getAttribute('value-placeholder')).toBe('');
                expect(form.getAttribute('key-label')).toBe('Key');
                expect(form.getAttribute('value-label')).toBe('Value');
            });
        });

        describe('key-value-table attr', () => {
            describe('button-label', () => {
                it('should pass down valid', async () => {
                    element.setAttribute('list-delete-label', 'Delete this item');
                    await page.waitForChanges();

                    const list = await getList();
                    expect(list.getAttribute('button-label')).toBe('Delete this item');
                });

                it('should pass down empty', async () => {
                    await page.waitForChanges();

                    const list = await getList();
                    expect(list.getAttribute('button-label')).toBe('Delete'); // internal default
                });
            });
        });

        describe('disabled', () => {
            it('should set disabled to child', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();

                const form = await getForm();
                const list = await getList();

                expect(form.getAttribute('disabled')).toBeDefined();
                expect(list.getAttribute('disabled')).toBeDefined();
            });

            it('should not set disabled to child', async () => {
                const form = await getForm();
                const list = await getList();

                expect(form.getAttribute('disabled')).toBeNull();
                expect(list.getAttribute('disabled')).toBeNull();
            });
        });

        describe('hint', () => {
            it('should render and set aria attribute', async () => {
                element.setProperty('hint', 'Some hint');
                await page.waitForChanges();
                const container = await page.find('dot-label');
                const hint = await dotTestUtil.getHint(page);
                expect(hint.innerText).toBe('Some hint');
                expect(hint.getAttribute('id')).toBe('hint-some-hint');
                expect(container.getAttribute('aria-describedby')).toBe('hint-some-hint');
                expect(container.getAttribute('tabIndex')).toBe('0');
            });

            it('should not render and not set aria attribute', async () => {
                const hint = await dotTestUtil.getHint(page);
                const container = await page.find('dot-label');
                expect(hint).toBeNull();
                expect(container.getAttribute('aria-describedby')).toBeNull();
                expect(container.getAttribute('tabIndex')).toBeNull();
            });

            it('should handle invalid', async () => {
                element.setProperty('hint', { a: 'object' });
                await page.waitForChanges();

                const hint = await dotTestUtil.getHint(page);
                expect(hint).toBeNull();
            });
        });

        describe('label', () => {
            it('should render', async () => {
                element.setProperty('label', 'Some label');
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('label')).toBe('Some label');
            });

            it('should not render', async () => {
                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('label')).toBe('');
            });

            it('should handle invalid', async () => {
                element.setProperty('label', ['some', 'array']);
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('label')).toBe('');
            });
        });

        describe('name', () => {
            it('should render', async () => {
                element.setProperty('name', 'Some name');
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('name')).toBe('Some name');
            });

            it('should not render', async () => {
                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(await dotLabel.getAttribute('name')).toBe('');
            });

            it('should handle invalid', async () => {
                element.setProperty('name', NaN);
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('name')).toBeNull();
            });
        });

        describe('required', () => {
            it('should render', async () => {
                element.setProperty('required', true);
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('required')).toBe('');
            });

            it('should not render', async () => {
                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('required')).toBeNull();
            });

            it('should handle invalid value --> truthy', async () => {
                element.setProperty('required', 1);
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('required')).toBe('');
            });

            it('should handle invalid value --> falsy', async () => {
                element.setProperty('required', NaN);
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('required')).toBeNull();
            });
        });

        describe('requiredMessage', () => {
            it('should show default', async () => {
                element.setProperty('required', true);
                element.setProperty('value', 'key|value');
                const list = await getList();
                list.triggerEvent('delete', { detail: 0 });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error.textContent).toBe('This field is required');
            });

            it('should render custom', async () => {
                element.setProperty('required', true);
                element.setProperty('requiredMessage', 'This is a custom message');
                element.setProperty('value', 'key|value');
                const list = await getList();
                list.triggerEvent('delete', { detail: 0 });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error.textContent).toBe('This is a custom message');
            });

            it('should not show', async () => {
                element.setProperty('requiredMessage', 'This is a custom message');
                element.setProperty('value', 'key|value');
                const list = await getList();
                list.triggerEvent('delete', { detail: 0 });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error).toBeNull();
            });
        });

        describe('value', () => {
            it('should set items', async () => {
                element.setProperty('value', 'hello|world,hola|mundo');
                await page.waitForChanges();
                const list = await getList();
                expect(await list.getProperty('items')).toEqual([
                    { key: 'hello', value: 'world' },
                    { key: 'hola', value: 'mundo' }
                ]);
            });

            it('should handle invalid format', async () => {
                element.setProperty('value', 'hello/world*hola,mundo');
                await page.waitForChanges();
                const list = await getList();
                expect(await list.getProperty('items')).toEqual([]);
            });

            it('should handle invalid type', async () => {
                element.setProperty('value', { hello: 'world' });
                await page.waitForChanges();
                const list = await getList();
                expect(await list.getProperty('items')).toEqual([]);
            });

            it('should handle undefined', async () => {
                const list = await getList();
                expect(await list.getProperty('items')).toEqual([]);
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            element.setAttribute('name', 'fieldName');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        });

        describe('valueChange and statusChange', () => {
            it('shoult emit on add', async () => {
                const form = await getForm();
                form.triggerEvent('add', {
                    detail: {
                        key: 'some key',
                        value: 'hello world'
                    }
                });
                await page.waitForChanges();
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: 'some key|hello world'
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: false, dotTouched: true, dotValid: true }
                });
            });

            it('shoult emit on remove', async () => {
                element.setAttribute('value', 'first key|first value,second key|second value');
                const list = await getList();
                list.triggerEvent('delete', {
                    detail: 1
                });
                await page.waitForChanges();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: 'first key|first value'
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: false, dotTouched: true, dotValid: true }
                });
            });
        });

        describe('statusChange', () => {
            it('should emit default valueChange', async () => {
                page = await newE2EPage({
                    html: `
                        <dot-form>
                            <dot-key-value name="fieldName" required="true" />
                        </dot-form>
                    `
                });
                await page.waitForChanges();

                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });

            it('should emit on lost focus in autocomplete', async () => {
                const form = await getForm();
                form.triggerEvent('lostFocus', {});
                await page.waitForChanges();

                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: true, dotTouched: true, dotValid: true }
                });
            });
        });
    });

    describe('@Methods', () => {
        beforeEach(async () => {
            element.setAttribute('name', 'fieldName');
            element.setAttribute('value', 'first key|first value,second key|second value');

            spyValueChangeEvent = await page.spyOnEvent('valueChange');
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
        });

        describe('reset', () => {
            it('should clear the field and emit invalid (field required)', async () => {
                element.setAttribute('required', true);
                element.callMethod('reset');
                await page.waitForChanges();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: ''
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: true, dotTouched: false, dotValid: false }
                });
            });
            it('should clear the field and emit valid (field not required)', async () => {
                await page.waitForChanges();

                element.callMethod('reset');
                await page.waitForChanges();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: ''
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: true, dotTouched: false, dotValid: true }
                });
            });
        });
    });
});
