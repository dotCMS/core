import { E2EElement, E2EPage, newE2EPage, EventSpy } from '@stencil/core/testing';

import { dotTestUtil } from '../../utils';

describe('dot-tags', () => {
    let page: E2EPage;
    let element: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    const getAutoComplete = () => page.find('dot-autocomplete');
    const getChips = () => page.findAll('dot-chip');

    const createEmptyDotTags = async () => {
        page = await newE2EPage({
            html: `<dot-tags></dot-tags>`
        });
        element = await page.find('dot-tags');
        await page.waitForChanges();
    };
    const autocompleteSelect = async (tag?: string) => {
        const autocomplete = await getAutoComplete();
        autocomplete.triggerEvent('selection', {
            detail: tag || 'sometag'
        });
        await page.waitForChanges();
    };
    const autocompleteEnter = async (tag?: string) => {
        const autocomplete = await getAutoComplete();
        autocomplete.triggerEvent('enter', {
            detail: tag || 'sometag'
        });
        await page.waitForChanges();
    };

    describe('css classes', () => {
        beforeEach(async () => {
            await createEmptyDotTags();
        });

        it('should have empty', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should have empty required pristine', async () => {
            element.setProperty('required', true);
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
        });

        it('should have empty required touched when all items is removed', async () => {
            element.setProperty('value', 'add,some');
            element.setProperty('required', true);
            await page.waitForChanges();

            const chips = await getChips();
            chips[0].triggerEvent('remove', {
                detail: 'add'
            });
            chips[1].triggerEvent('remove', {
                detail: 'some'
            });
            await page.waitForChanges();

            expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
        });

        it('should have filled', async () => {
            await autocompleteSelect();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        it('should have filled required', async () => {
            element.setProperty('required', true);
            await autocompleteSelect();

            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have filled required pristine', async () => {
            element.setProperty('required', true);
            element.setProperty('value', 'some,tags');

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequiredPristine);
        });

        it('should have filled required touched when item is added', async () => {
            element.setProperty('required', true);
            await autocompleteSelect();

            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have filled required touched when one item is removed', async () => {
            element.setProperty('value', 'some,tags');
            element.setProperty('required', true);
            await page.waitForChanges();
            const chips = await getChips();
            chips[0].triggerEvent('remove', {
                detail: 'some'
            });
            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
        });

        it('should have touched but pristine', async () => {
            const autocomplete = await getAutoComplete();
            autocomplete.triggerEvent('lostFocus', {});
            await page.waitForChanges();

            expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
        });
    });

    describe('@Props', () => {
        beforeEach(async () => {
            await createEmptyDotTags();
        });

        describe('data', () => {
            it('should pass data down', async () => {
                spyValueChangeEvent = await page.spyOnEvent('valueChange');
                const mock = async () => ['hello', 'world'];
                element.setProperty('data', mock);
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                const input = await autocomplete.find('input');
                input.type('hel');
                await element.press('ArrowDown');
                await element.press('Enter');
                await page.waitForChanges();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({ name: '', value: 'hel' });
            });
        });

        describe('value', () => {
            it('should render chips', async () => {
                element.setProperty('value', 'give,me,tags');
                await page.waitForChanges();

                const chips = await getChips();
                expect(chips.length).toBe(3);
            });
        });

        describe('name', () => {
            it('should not render', async () => {
                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(await dotLabel.getAttribute('name')).toBe('');
            });

            it('should render', async () => {
                element.setProperty('name', 'Some name');
                await page.waitForChanges();

                const dotLabel = await dotTestUtil.getDotLabel(page);
                expect(dotLabel.getAttribute('name')).toBe('Some name');
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
        });

        describe('hint', () => {
            it('should render and set aria attribute', async () => {
                element.setProperty('hint', 'Some hint');
                await page.waitForChanges();
                const tagsContainer = await page.find('.dot-tags__container');
                const hint = await dotTestUtil.getHint(page);
                expect(hint.innerText).toBe('Some hint');
                expect(hint.getAttribute('id')).toBe('hint-some-hint');
                expect(tagsContainer.getAttribute('aria-describedby')).toBe('hint-some-hint');
                expect(tagsContainer.getAttribute('tabIndex')).toBe('0');
            });

            it('should not render and not set aria attribute', async () => {
                const hint = await dotTestUtil.getHint(page);
                const tagsContainer = await page.find('.dot-tags__container');
                expect(hint).toBeNull();
                expect(tagsContainer.getAttribute('aria-describedby')).toBeNull();
                expect(tagsContainer.getAttribute('tabIndex')).toBeNull();
            });
        });

        describe('placeholder', () => {
            it('should render', async () => {
                element.setProperty('placeholder', 'Some placeholder');
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('placeholder')).toBe('Some placeholder');
            });

            it('should not render', async () => {
                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('placeholder')).toBeNull();
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
        });

        describe('requiredMessage', () => {
            it('should show default', async () => {
                element.setProperty('required', true);
                element.setProperty('value', 'some');
                await page.waitForChanges();

                const chips = await getChips();
                chips[0].triggerEvent('remove', {
                    detail: 'some'
                });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error.textContent).toBe('This field is required');
            });

            it('should render custom', async () => {
                element.setProperty('required', true);
                element.setProperty('requiredMessage', 'Custom error message');
                element.setProperty('value', 'some');
                await page.waitForChanges();

                const chips = await getChips();
                chips[0].triggerEvent('remove', {
                    detail: 'some'
                });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error.textContent).toBe('Custom error message');
            });

            it('should not show', async () => {
                element.setProperty('requiredMessage', 'Custom error message');
                element.setProperty('value', 'some');
                await page.waitForChanges();

                const chips = await getChips();
                chips[0].triggerEvent('remove', {
                    detail: 'some'
                });
                await page.waitForChanges();

                const error = await dotTestUtil.getErrorMessage(page);
                expect(error).toBeNull();
            });
        });

        describe('disabled', () => {
            it('should render', async () => {
                element.setProperty('disabled', true);
                element.setProperty('value', 'some');
                await page.waitForChanges();

                const chips = await getChips();
                const autocomplete = await getAutoComplete();

                expect(chips[0].getAttribute('disabled')).toBeDefined();
                expect(autocomplete.getAttribute('disabled')).toBeDefined();
            });

            it('should not render', async () => {
                element.setProperty('value', 'some');
                await page.waitForChanges();

                const chips = await getChips();
                const autocomplete = await getAutoComplete();

                expect(chips[0].getAttribute('disabled')).toBeNull();
                expect(autocomplete.getAttribute('disabled')).toBeNull();
            });
        });

        describe('threshold', () => {
            it('should render default', async () => {
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('threshold')).toBe('0');
            });

            it('should render passed', async () => {
                element.setProperty('threshold', 100);
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('threshold')).toBe('100');
            });
        });

        describe('debounce', () => {
            it('should render default', async () => {
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('debounce')).toBe('300');
            });

            it('should render passed', async () => {
                element.setProperty('debounce', 100);
                await page.waitForChanges();

                const autocomplete = await getAutoComplete();
                expect(autocomplete.getAttribute('debounce')).toBe('100');
            });
        });
    });

    describe('@Events', () => {
        describe('valueChange and statusChange', () => {
            beforeEach(async () => {
                await createEmptyDotTags();
            });

            beforeEach(async () => {
                element.setAttribute('name', 'fieldName');
                spyValueChangeEvent = await page.spyOnEvent('valueChange');
                spyStatusChangeEvent = await page.spyOnEvent('statusChange');
            });

            it('should emit on add', async () => {
                await autocompleteSelect();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: 'sometag'
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: false, dotTouched: true, dotValid: true }
                });
            });

            it('should emit on remove', async () => {
                element.setAttribute('value', 'some,tag');
                await page.waitForChanges();

                const chips = await getChips();
                chips[0].triggerEvent('remove', {
                    detail: 'some'
                });
                await page.waitForChanges();

                expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    value: 'tag'
                });
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: false, dotTouched: true, dotValid: true }
                });
            });
        });

        describe('statusChange', () => {
            it('should emit on lost focus in autocomplete', async () => {
                await createEmptyDotTags();
                const autocomplete = await getAutoComplete();
                autocomplete.triggerEvent('lostFocus', {});
                await page.waitForChanges();

                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: false, dotTouched: true, dotValid: true }
                });
            });

            it('should emit status changed', async () => {
                page = await newE2EPage({
                    html: `
                        <dot-form>
                            <dot-tags required></dot-tags>
                        </dot-form>
                    `
                });
                await page.waitForChanges();
                const form = await page.find('dot-form');
                expect(form).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });
        });
    });

    describe('@Methods', () => {
        beforeEach(async () => {
            await createEmptyDotTags();
        });

        beforeEach(async () => {
            element.setAttribute('name', 'fieldName');
            element.setAttribute('value', 'some,tag');
            spyValueChangeEvent = await page.spyOnEvent('valueChange');
            spyStatusChangeEvent = await page.spyOnEvent('statusChange');

            await page.waitForChanges();
        });

        describe('reset', () => {
            it('should clear and emit', async () => {
                expect(await element.getProperty('value')).toBe('some,tag');
                element.callMethod('reset');

                await page.waitForChanges();
                expect(await element.getProperty('value')).toBe('');
                expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                    name: 'fieldName',
                    status: { dotPristine: true, dotTouched: false, dotValid: true }
                });
            });
        });
    });

    describe('chips', () => {
        beforeEach(async () => {
            await createEmptyDotTags();
        });

        describe('autocomplete', () => {
            describe('select event', () => {
                it('should add chip', async () => {
                    await autocompleteSelect();
                    const chips = await getChips();
                    expect(chips.length).toBe(1);
                });

                it('should clean tag when have semicolon', async () => {
                    await autocompleteSelect('hello, world');
                    const chips = await getChips();
                    const text = await chips[0].find('span');
                    expect(chips.length).toBe(1);
                    expect(text.innerText).toBe('hello world');
                });
            });

            describe('enter event', () => {
                it('should add chip', async () => {
                    await autocompleteEnter();
                    const chips = await getChips();
                    expect(chips.length).toBe(1);
                });

                it('should clean tag when have semicolon', async () => {
                    await autocompleteEnter('hello, world');
                    const chips = await getChips();
                    expect(chips.length).toBe(2);

                    const text1 = await chips[0].find('span');
                    expect(text1.innerText).toBe('hello');
                    const text2 = await chips[1].find('span');
                    expect(text2.innerText).toBe('world');
                });
            });
        });
    });
});
