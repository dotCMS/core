import { newE2EPage, E2EElement, E2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-select', () => {

    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    describe('with params', () => {
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
            expect(element).toEqualHtml(`
            <dot-select
                class=\"dot-pristine dot-required dot-untouched dot-valid hydrated\"
                hint=\"testHint\"
                label=\"testLabel\"
                name=\"testName\"
                options=\"|,valueA|1,valueB|2\"
                required=\"true\"
                required-message=\"testErrorMsg\"
                value=\"2\">
                    <div class=\"dot-field__label\">
                    <label for=\"dot-testName\">
                        testLabel
                    </label>
                    <span class=\"dot-field__required-mark\">
                        *
                    </span>
                    </div>
                    <select id=\"dot-testName\">
                        <option value=\"\"></option>
                        <option value=\"1\">
                            valueA
                        </option>
                        <option value=\"2\">
                            valueB
                        </option>
                    </select>
                    <span class=\"dot-field__hint\">
                        testHint
                    </span>
            </dot-select>`);
        });

        it('should be invalid, touched & dirty and the error msg should display', async () => {
            await page.select('select', '');
            await page.waitForChanges();
            expect(element).toHaveClasses(['dot-invalid', 'dot-touched', 'dot-dirty']);
        });

        it('it should set options blank when an Object is passed as options', async () => {
            element.setProperty('options', { noValid: true });
            await page.waitForChanges();
            const options = await page.find('option');
            expect(options).toBe(null);
        });

        it('it should not break when an invalid formatted string is passed as options', async () => {
            element.setProperty('options', 'a,b');
            await page.waitForChanges();
            const options = await page.findAll('option');
            expect(options[0]).toEqualHtml('<option>a</option>');
            expect(options[1]).toEqualHtml('<option>b</option>');
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
                await element.callMethod('reset');
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

    describe('without params', () => {
        it('renders', async () => {
            page = await newE2EPage();

            await page.setContent(`<dot-select></dot-select>`);
            element = await page.find('dot-select');
            const label = await page.find('label');
            const requiredLabel = await page.find('.dot-field__required-mark');
            const options = await page.findAll('option');
            const hint = await page.find('.dot-field__hint');

            expect(element).toHaveClasses(['dot-pristine', 'dot-untouched', 'dot-valid', 'hydrated']);
            expect(label.innerHTML).toBe('');
            expect(requiredLabel).toBe(null);
            expect(options.length).toBe(0);
            expect(hint).toBe(null);
        });
    });
});
