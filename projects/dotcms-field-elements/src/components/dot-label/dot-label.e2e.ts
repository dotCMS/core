import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';

describe('dot-label', () => {
    let page: E2EPage;
    let element: E2EElement;

    const getLabel = async () => await page.find('label');
    const getText = async () => await page.find('.dot-label__text');
    const getMark = async () => await page.find('.dot-label__required-mark');
    const getHost = async () => await page.find('dot-label');

    describe('<slot />', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `
                <dot-label labe="hello world">
                    <h1>into the slot</h1>
                </dot-label>`
            });
            element = await getHost();
        });

        it('should render after label', async () => {
            const slot = await page.find('.dot-label__text + h1');
            expect(slot.innerHTML).toBe('into the slot');
        });
    });

    describe('@Props', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `
                <dot-label>
                    <h1>into the slot</h1>
                </dot-label>`
            });
            element = await getHost();
        });

        describe('label', () => {
            it('should render with valid value', async () => {
                element.setProperty('label', 'a valid label');
                await page.waitForChanges();
                const text = await getText();
                expect(text.innerText).toBe('a valid label');
            });

            it('should render empty string with invalid type', async () => {
                element.setProperty('label', {});
                await page.waitForChanges();
                const text = await getText();
                expect(text.innerText).toBe('[object Object]');
            });

            it('should render empty string with no value', async () => {
                element.setProperty('label', undefined);
                await page.waitForChanges();
                const text = await getText();
                expect(text.innerText).toBe('');
            });
        });

        describe('required', () => {
            it('should show mark on true', async () => {
                element.setProperty('required', true);
                await page.waitForChanges();
                const mark = await getMark();
                expect(mark.innerText).toBe('*');
            });

            it('should hide mark on false', async () => {
                element.setProperty('required', false);
                await page.waitForChanges();
                const mark = await getMark();
                expect(mark).toBeNull();
            });
        });

        describe('name', () => {
            it('should render with valid value', async () => {
                element.setProperty('name', 'someCamelCas*eNa&me&$');
                await page.waitForChanges();
                const label = await getLabel();
                expect(await label.getAttribute('id')).toBe('label-somecamelcasename');
            });

            it('should not render when not defined', async () => {
                element.setProperty('name', undefined);
                await page.waitForChanges();
                const label = await getLabel();
                expect(await label.getAttribute('id')).toBe(null);
            });

            it('should not render with invalid value', async () => {
                element.setProperty('name', null);
                await page.waitForChanges();
                const label = await getLabel();
                expect(await label.getProperty('id')).toBe('');
            });
        });
    });
});
