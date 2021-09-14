import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-material-icon-picker', () => {
    let page: E2EPage;
    let dotSelectButton: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-material-icon-picker></dot-material-icon-picker>`
        });
        dotSelectButton = await page.find('dot-material-icon-picker');
        dotSelectButton.setProperty('value', 'accessibility');
        dotSelectButton.setProperty('showColor', 'true');
        await page.waitForChanges();
    });

    describe('@Elements', () => {
        it('should have mwc-icon elements', async () => {
            dotSelectButton.click();
            await page.waitForChanges();
            const icons: E2EElement[] = await page.findAll(
                'dot-material-icon-picker .dot-material-icon__list mwc-icon'
            );
            expect(icons.length).toBeGreaterThan(0);
        });

        it('should have input color', async () => {
            const inputColor = await page.find('dot-material-icon-picker #iconColor');
            expect(inputColor.getAttribute('type')).toBe('color');
        });
    });

    describe('@Events', () => {
        let options: E2EElement[];
        let input: E2EElement;
        let inputColor: E2EElement;
        let event;

        beforeEach(async () => {
            event = await page.spyOnEvent('dotValueChange');
            dotSelectButton.click();
            await page.waitForChanges();
            input = await page.find('dot-material-icon-picker .dot-material-icon__input');
            inputColor = await page.find('dot-material-icon-picker #iconColor');
            options = await page.findAll('dot-material-icon-picker .dot-material-icon__option');
        });

        it('should go down on the options dropdown', async () => {
            await input.press('ArrowDown');
            await page.waitForChanges();
            expect(options[0].classList.contains('dot-material-icon__option-selected')).toBe(true);
        });

        it('should emit selected option and set value when pressed enter', async () => {
            await input.press('ArrowDown');
            await page.waitForChanges();
            await input.press('Enter');
            expect(event).toHaveReceivedEventDetail({
                colorValue: '#000',
                name: '',
                value: 'accessibility_new'
            });
            expect(await dotSelectButton.getProperty('value')).toEqual('accessibility_new');
        });

        it('should go up on the options dropdown', async () => {
            await input.press('ArrowUp');
            await page.waitForChanges();
            expect(
                options[options.length - 1].classList.contains('dot-material-icon__option-selected')
            ).toBe(true);
        });

        it('should emit selected option and set value when option clicked', async () => {
            await options[0].click();
            await page.waitForChanges();
            expect(event).toHaveReceivedEventDetail({
                colorValue: '#000',
                name: '',
                value: 'accessibility_new'
            });
            expect(await dotSelectButton.getProperty('value')).toEqual('accessibility_new');
        });

        it('should emit value when text pasted', async () => {
            dotSelectButton.setProperty('value', '');
            await page.waitForChanges();
            await input.type('360');
            await input.press('Tab');
            await page.waitForChanges();
            expect(event).toHaveReceivedEventDetail({
                colorValue: '#000',
                name: '',
                value: '360'
            });
            expect(await input.getProperty('value')).toEqual('360');
        });

        it('should allow wildcard on search', async () => {
            const results = [];
            dotSelectButton.setProperty('value', '');
            await page.waitForChanges();
            await input.type('tool');
            await page.waitForChanges();
            const icons: E2EElement[] = await page.findAll('[aria-labelledby]');
            icons.forEach( (icon) =>  results.push(icon.innerText));
            expect(results).toContain('pan_tool');
        });

        // TODO: Find a way to trigger the change event on Input type="color"
        xit('should emit input color when picked', async () => {
            await inputColor.type('#777');
            await page.waitForChanges();
            expect(event).toHaveReceivedEventDetail({
                colorValue: '#777',
                name: '',
                value: 'accessibility_new'
            });
            expect(await dotSelectButton.getProperty('colorValue')).toEqual('#777');
        });
    });
});
