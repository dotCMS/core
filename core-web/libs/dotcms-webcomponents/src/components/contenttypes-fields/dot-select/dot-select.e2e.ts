import { newE2EPage, E2EElement, E2EPage, EventSpy } from '@stencil/core/testing';
import { dotTestUtil } from '../../../test';

const getSelect = (page: E2EPage) => page.find('select');
const getOptions = (page: E2EPage) => page.findAll('option');

describe('dot-select', () => {
    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    describe('render CSS classes', () => {
        beforeEach(async () => {
            page = await newE2EPage();
        });

        it('should be valid, touched & dirty when picked an option', async () => {
            await page.setContent(`
                <dot-select
                    options="|,valueA|1,valueB|2"
                    value="2">
                </dot-select>`);
            await page.select('select', '1');
            await page.waitForChanges();
            element = await page.find('dot-select');
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        it('should be required, valid, touched & dirty when picked an option', async () => {
            await page.setContent(`
                <dot-select
                    options="|,valueA|1,valueB|2"
                    value="2"
                    required="true">
                </dot-select>`);
            await page.select('select', '1');
            await page.waitForChanges();
            element = await page.find('dot-select');
            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should be required, valid, untouched & pristine when picked an option', async () => {
            await page.setContent(`
                <dot-select
                    options="|,valueA|1,valueB|2"
                    value="2"
                    required="true">
                </dot-select>`);
            await page.waitForChanges();
            element = await page.find('dot-select');
            expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
        });

        it('should be invalid, touched & dirty when no option set', async () => {
            await page.setContent(`
            <dot-select
                options="|,valueA|1,valueB|2"
                value="2"
                required="true">
            </dot-select>`);
            element = await page.find('dot-select');
            await page.select('select', '');
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
        });

        it('should be invalid, untouched & pristine when loaded and no option set', async () => {
            await page.setContent(`
            <dot-select
                options="|,valueA|1,valueB|2"
                required="true">
            </dot-select>`);
            element = await page.find('dot-select');
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
        });

        it('should be pristine, untouched & valid', async () => {
            await page.setContent(`<dot-select></dot-select>`);
            element = await page.find('dot-select');
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });
    });

    describe('@Props', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-select></dot-select>`
            });
            element = await page.find('dot-select');
        });

        describe('dot-attr', () => {
            it('should set value correctly', async () => {
                page = await newE2EPage({
                    html: `<dot-select dotmultiple="true"></dot-select>`
                });
                await page.waitForChanges();
                const htmlElement = await getSelect(page);
                expect(htmlElement.getAttribute('multiple')).toBe('true');
            });
        });

        describe('disabled', () => {
            it('should not render attribute', async () => {
                const htmlElement = await getSelect(page);
                expect(htmlElement.getAttribute('disabled')).toBeNull();
            });

            it('should render attribute', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();
                const htmlElement = await getSelect(page);
                expect(htmlElement.getAttribute('disabled')).toBeDefined();
            });

            it('should not break with invalid data --> truthy', async () => {
                element.setProperty('disabled', ['a', 'b']);
                await page.waitForChanges();
                const htmlElement = await getSelect(page);
                expect(htmlElement.getAttribute('disabled')).toBeDefined();
            });

            it('should not break with invalid data --> falsy', async () => {
                element.setProperty('disabled', 0);
                await page.waitForChanges();
                const htmlElement = await getSelect(page);
                expect(htmlElement.getAttribute('disabled')).toBeNull();
            });
        });

        describe('name', () => {
            const value = 'test';

            it('should render attribute in label and select', async () => {
                element.setProperty('name', value);
                await page.waitForChanges();

                const selectElement = await getSelect(page);
                const idValue = selectElement.getAttribute('id');
                expect(idValue).toBe('dot-test');

                const labelElement = await dotTestUtil.getDotLabel(page);
                expect(labelElement.getAttribute('name')).toBe(value);
            });

            it('should not render attribute in label and select', async () => {
                const selectElement = await getSelect(page);
                const idValue = selectElement.getAttribute('id');
                expect(idValue).toBeNull();

                const labelElement = await dotTestUtil.getDotLabel(page);
                expect(labelElement.getAttribute('name')).toBe('');
            });

            it('should not break with invalid data', async () => {
                const wrongValue = [1, 2, 3];
                element.setProperty('name', wrongValue);
                await page.waitForChanges();

                const selectElement = await getSelect(page);
                const idValue = selectElement.getAttribute('id');
                expect(idValue).toBe('dot-123');
            });
        });

        describe('label', () => {
            it('should render attribute in label', async () => {
                const value = 'test';
                element.setProperty('label', value);
                await page.waitForChanges();
                const labelElement = await dotTestUtil.getDotLabel(page);
                expect(labelElement.getAttribute('label')).toBe(value);
            });

            it('should not break with invalid data', async () => {
                const wrongValue = [1, 2, '3'];
                element.setProperty('label', wrongValue);
                await page.waitForChanges();
                const labelElement = await dotTestUtil.getDotLabel(page);
                expect(labelElement.getAttribute('label')).toEqual('');
            });
        });

        describe('hint', () => {
            it('should set hint correctly and set aria attribute', async () => {
                const value = 'test';
                element.setProperty('hint', value);
                await page.waitForChanges();
                const hintElement = await dotTestUtil.getHint(page);
                const selectElement = await getSelect(page);
                expect(hintElement.innerText).toBe(value);
                expect(selectElement.getAttribute('aria-describedby')).toBe('hint-test');
            });

            it('should not render hint and does not set aria attribute', async () => {
                const hintElement = await dotTestUtil.getHint(page);
                const selectElement = await getSelect(page);
                expect(hintElement).toBeNull();
                expect(selectElement.getAttribute('aria-describedby')).toBeNull();
            });

            it('should not break and not render with invalid data', async () => {
                const wrongValue = [1, 2, '3'];
                element.setProperty('hint', wrongValue);
                await page.waitForChanges();
                const hintElement = await dotTestUtil.getHint(page);
                expect(hintElement).toBeNull();
            });
        });

        describe('options', () => {
            it('should render options', async () => {
                const value = 'a|1,b|2,c|3';
                element.setProperty('options', value);
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(optionElements.length).toBe(3);
            });

            it('should not render options', async () => {
                const optionElements = await getOptions(page);
                expect(optionElements.length).toBe(0);
            });
        });

        describe('required', () => {
            it('should render required attribute in label and dot-required css class', async () => {
                element.setProperty('required', true);
                await page.waitForChanges();
                const labelElement = await dotTestUtil.getDotLabel(page);
                expect(element).toHaveClasses(['dot-required']);
                expect(labelElement.getAttribute('required')).toBeDefined();
            });

            it('should not render required error msg', async () => {
                const errorElement = await dotTestUtil.getErrorMessage(page);
                expect(errorElement).toBeNull();
            });

            it('should not break and not render with invalid data', async () => {
                const wrongValue = [1, 2, '3'];
                element.setProperty('required', wrongValue);
                await page.waitForChanges();
                const errorElement = await dotTestUtil.getErrorMessage(page);
                expect(errorElement.innerText).toBe('This field is required');
            });
        });

        describe('requiredMessage', () => {
            it('should not break and not render with invalid data', async () => {
                const wrongValue = [1, 2, '3'];
                element.setProperty('requiredMessage', wrongValue);
                await page.waitForChanges();
                const errorElement = await dotTestUtil.getErrorMessage(page);
                expect(errorElement).toBeNull();
            });
        });

        describe('required & requiredMessage', () => {
            it('should render required error msg', async () => {
                element.setProperty('required', true);
                element.setProperty('requiredMessage', 'test');
                await page.waitForChanges();
                const errorElement = await dotTestUtil.getErrorMessage(page);
                expect(errorElement.innerText).toBe('test');
            });

            it('should not break and not render with invalid data', async () => {
                const wrongValue = [1, 2, '3'];
                element.setProperty('required', wrongValue);
                element.setProperty('requiredMessage', wrongValue);
                await page.waitForChanges();
                const errorElement = await dotTestUtil.getErrorMessage(page);
                expect(errorElement).toBeNull();
            });
        });

        describe('value', () => {
            it('should render option as selected', async () => {
                element.setProperty('options', 'a|1,b|2');
                element.setProperty('value', '2');
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(await optionElements[1].getProperty('selected')).toBe(true);
            });

            it('should render options with the first option selected (component\'s default behaviour)', async () => {
                element.setProperty('options', 'a|1,b|2,c|3');
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(await optionElements[0].getProperty('selected')).toBe(true);
                expect(await optionElements[1].getProperty('selected')).toBe(false);
                expect(await optionElements[2].getProperty('selected')).toBe(false);
            });

            it('should not break with wrong data format', async () => {
                element.setProperty('options', 'a1,2,c|3');
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(optionElements.length).toBe(0);
            });

            it('should not break with wrong data type', async () => {
                const wrongValue = [{ a: 1 }];
                element.setProperty('options', 'a|1,b|2,c|3');
                element.setProperty('value', wrongValue);
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(await optionElements[0].getProperty('selected')).toBe(true);
                expect(await optionElements[1].getProperty('selected')).toBe(false);
                expect(await optionElements[2].getProperty('selected')).toBe(false);
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `
                <dot-form>
                    <dot-select
                        name="testName"
                        options="|,valueA|1,valueB|2"
                        required="true">
                    </dot-select>
                </dot-form>`
            });
            spyStatusChangeEvent = await page.spyOnEvent('dotStatusChange');
            spyValueChangeEvent = await page.spyOnEvent('dotValueChange');

            element = await page.find('dot-select');
        });

        describe('status and value change', () => {
            it('should display on wrapper not valid css classes when loaded, required and no value set', async () => {
                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });

            it('should emit when option selected', async () => {
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
        });
    });

    describe('@Methods', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `
                <dot-select
                    name="testName"
                    options="|,valueA|1,valueB|2"
                    value="2">
                </dot-select>`
            });
            spyStatusChangeEvent = await page.spyOnEvent('dotStatusChange');
            spyValueChangeEvent = await page.spyOnEvent('dotValueChange');

            element = await page.find('dot-select');
        });

        describe('Reset', () => {
            it('should emit dotStatusChange & dotValueChange Events', async () => {
                await element.callMethod('reset');
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'testName',
                    status: {
                        dotPristine: true,
                        dotTouched: false,
                        dotValid: true
                    }
                });
                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'testName',
                    value: ''
                });
            });

            it('should set first select value', async () => {
                await element.callMethod('reset');
                await page.waitForChanges();
                const optionElements = await getOptions(page);
                expect(await optionElements[0].getProperty('selected')).toBe(true);
                expect(await optionElements[1].getProperty('selected')).toBe(false);
                expect(await optionElements[2].getProperty('selected')).toBe(false);
            });
        });
    });
});
