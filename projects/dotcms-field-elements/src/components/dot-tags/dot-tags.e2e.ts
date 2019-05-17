import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-tags', () => {
    let page: E2EPage;
    let element: E2EElement;

    describe('render all attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-tags
                            value="tag-1"
                            name="tag_name"
                            label="label"
                            hint="hint"
                            placeholder='placeholder'
                            required="true"
                            requiredMessage="requiredMessage"
                            disabled="true"
                            threshold="3"
                            debounce="100"
                       >
                       </dot-tags>`
            });

            element = await page.find('dot-tags');
        });

        it('should has a label', async () => {
            const label = await element.find('.dot-field__label label');
            expect(label.innerHTML).toEqualHtml('label');
        });

        describe('should has a tab container', () => {
            it('should has a div tab container', async () => {
                expect(await element.find('div.tag_container')).toBeDefined();
            });

            it('should has a div tab container', async () => {
                const divContainer = await element.find('div.tag_container');
                expect((await divContainer.findAll('dot-chip')).length).toEqual(1);
            });
        });

        describe('should has a dot-autocomplete', () => {
            let dotAutoComplete;

            beforeEach(async () => {
                dotAutoComplete = await element.find('dot-autocomplete');
            });

            it('should has a id', async () => {
                expect(await dotAutoComplete.getProperty('id')).toEqualHtml('tag_name');
            });

            it('should has a disabled', async () => {
                expect(await dotAutoComplete.getProperty('disabled')).toBeTruthy();
            });

            it('should has a plaveholder', async () => {
                expect(await dotAutoComplete.getProperty('placeholder')).toBe('placeholder');
            });
        });
    });

    describe('render each attributes', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-tags></dot-tags>`
            });

            element = await page.find('dot-tags');
        });

        it('should render', async () => {
            expect(await element.find('.dot-field__label label')).toBeDefined();
            expect(await element.find('div.tag_container')).toBeDefined();
            expect(await element.find('dot-autocomplete')).toBeDefined();
        });

        it('should render with name', async () => {
            element.setAttribute('name', 'testing');
            await page.waitForChanges();

            const label = await element.find('.dot-field__label label');
            expect(label.getAttribute('for')).toBe('dot-testing');

            const dotAutocomplete = await element.find('dot-autocomplete');
            expect(await dotAutocomplete.getProperty('id')).toBe('testing');
        });

        it('should render with label', async () => {
            element.setAttribute('label', 'testing');
            await page.waitForChanges();

            const label = await element.find('.dot-field__label label');
            expect(label.innerHTML).toBe('testing');
        });

        it('should render with hint', async () => {
            element.setAttribute('hint', 'hint');
            await page.waitForChanges();

            const hint = await element.find('.dot-field__hint');
            expect(await hint.innerHTML).toBe('hint');
        });

        it('should render with paceholder', async () => {
            element.setAttribute('placeholder', 'placeholder');
            await page.waitForChanges();

            const autocomplete = await element.find('dot-autocomplete');
            expect(await autocomplete.getProperty('placeholder')).toBe('placeholder');
        });

        it('should mark autocomplete and tag as disabled', async () => {
            element.setProperty('disabled', true);
            element.setProperty('value', 'tag-1');
            await page.waitForChanges();

            const autocomplete = await page.find('dot-tags dot-autocomplete');
            expect(await autocomplete.getProperty('disabled')).toBe(true);

            const tag = await page.find('dot-tags dot-chip');
            expect(await tag.getProperty('disabled')).toBe(true);
        });

        it('should mark any new tag as disabled', async () => {
            element.setProperty('disabled', true);
            await page.waitForChanges();

            element.setProperty('value', 'tag-1');
            await page.waitForChanges();

            const tag = await page.find('dot-tags dot-chip');
            expect(await tag.getProperty('disabled')).toBe(true);
        });

        describe('unvalid inputs', () => {
            it('should not broke when value does not have comma', async () => {
                element.setAttribute('value', 'tag-1');
                await page.waitForChanges();

                expect(element.getAttribute('value')).toBe('tag-1');
            });

            it('should not broke when value is not a string', async () => {
                element.setAttribute('value', {});
                await page.waitForChanges();

               expect(element.getAttribute('value')).toBe('[object Object]');
            });

            it('should not broke when name is not a string', async () => {
                element.setAttribute('name', {});
                await page.waitForChanges();

                expect(element.getAttribute('name')).toBe('[object Object]');
            });

            it('should not broke when label is not a string', async () => {
                element.setAttribute('label', {});
                await page.waitForChanges();

                expect(element.getAttribute('label')).toBe('[object Object]');
            });

           it('should not broke when hint is not a string', async () => {
                element.setAttribute('hint', {});
                await page.waitForChanges();

                expect(element.getAttribute('hint')).toBe('[object Object]');
            });

            it('should not broke when placeholder is not a string', async () => {
                element.setAttribute('placeholder', {});
                await page.waitForChanges();

                expect(element.getAttribute('placeholder')).toBe('[object Object]');
            });

            it('should not broke when disabled is not a boolean', async () => {
                element.setAttribute('disabled', {});
                await page.waitForChanges();

                expect(element.getAttribute('disabled')).toBeTruthy();
            });

            it('should not broke when required is not a boolean', async () => {
                element.setAttribute('required', {});
                await page.waitForChanges();

                expect(element.getAttribute('required')).toBeTruthy();
            });

            it('should not broke when requiredMessage is not a string', async () => {
                element.setAttribute('requiredMessage', {});
                await page.waitForChanges();

                expect(element.getAttribute('requiredMessage')).toBe('[object Object]');
            });
        });
    });

    describe('status', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-tags name='tag'
                                 label='tag'>
                       </dot-tags>`
            });

            element = await page.find('dot-tags');
        });

        it('should load as pristine and untouched', () => {
            expect(element).toHaveClasses(['dot-pristine', 'dot-untouched']);
        });

        it('should mark as dirty and touched when select a tag', async () => {
            const autocomplete = await page.find('dot-tags dot-autocomplete');
            await autocomplete.triggerEvent('selection', {detail: 'tag-1'});
            await page.waitForChanges();

            expect(element).toHaveClasses(['dot-dirty', 'dot-touched']);
        });

        it('should mark as dirty and touched when remove a tag', async () => {
            element.setAttribute('value', 'tag-1');
            await page.waitForChanges();

            const dotChip = await page.find('dot-tags dot-chip');
            await dotChip.triggerEvent('remove', {detail: 'tag-1'});
            await page.waitForChanges();

            expect(element).toHaveClasses(['dot-dirty', 'dot-touched']);
        });

        it('should clear value, set pristine and untouched  when input set reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();

            expect(element).toHaveClasses(['dot-pristine', 'dot-untouched', 'dot-valid']);
        });
    });

    describe('emit events', () => {
        let spyStatusChangeEvent: EventSpy;
        let spyValueChange: EventSpy;

        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-tags name='tag'
                                 label='tag'>
                       </dot-tags>`
            });

            spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            spyValueChange = await page.spyOnEvent('valueChange');
            element = await page.find('dot-tags');
        });

        it('should emit status event when blur', async () => {
            const autocomplete = await page.find('dot-autocomplete');
            await autocomplete.triggerEvent('lostFocus');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'tag',
                status: {
                    dotPristine: true,
                    dotTouched: true,
                    dotValid: true
                }
            });
        });

        it('should send status when autocomplete value is selection', async () => {
            const autocomplete = await page.find('dot-tags dot-autocomplete');
            await autocomplete.triggerEvent('selection', {detail: 'tag-1'});
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'tag',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'tag',
                value: 'tag-1'
            });
        });

        it('should send status when a tag is remove', async () => {
            element.setAttribute('value', 'tag-1');
            await page.waitForChanges();

            const dotChip = await page.find('dot-tags dot-chip');
            await dotChip.triggerEvent('remove', {detail: 'tag-1'});
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'tag',
                status: {
                    dotPristine: false,
                    dotTouched: true,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({
                name: 'tag',
                value: ''
            });
        });

        it('should emit status and value on Reset', async () => {
            element.callMethod('reset');
            await page.waitForChanges();

            expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                name: 'tag',
                status: {
                    dotPristine: true,
                    dotTouched: false,
                    dotValid: true
                }
            });
            expect(spyValueChange).toHaveReceivedEventDetail({ name: 'tag', value: '' });
        });

        it('should be unvalid when not have any value and is mark as required', async () => {
            element.setAttribute('required', true);
            await page.waitForChanges();

            expect(element).toHaveClasses(['dot-invalid']);
        });

        it('should be valid when not have any value and is not mark as required', async () => {
            element.setAttribute('required', false);
            await page.waitForChanges();

            expect(element).toHaveClasses(['dot-valid']);
        });
    });
});
