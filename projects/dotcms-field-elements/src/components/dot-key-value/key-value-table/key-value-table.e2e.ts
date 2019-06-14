import { newE2EPage, E2EElement, E2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('key-value-table', () => {
    let page: E2EPage;
    let element: E2EElement;
    let spyDeleteEvent: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<key-value-table />`
        });
        element = await page.find('key-value-table');
        await page.waitForChanges();
    });

    const getButton = () => page.find('.dot-key-value__delete-button');

    describe('@Props', () => {
        describe('items', () => {
            it('should fill table with valid value and set aria label', async () => {
                element.setProperty('items', [
                    { key: 'keyA', value: '1' },
                    { key: 'keyB', value: '2' }
                ]);
                await page.waitForChanges();

                const rows = await element.findAll('tr');
                expect(rows.length).toBe(2);
                expect(rows[0]).toEqualHtml(`
                    <tr>
                        <td>
                            <button
                                aria-label="Delete keyA, 1"
                                class="dot-key-value__delete-button">
                                    Delete
                             </button>
                        </td>
                        <td>keyA</td>
                        <td>1</td>
                    </tr>
                `);
                expect(rows[1]).toEqualHtml(`
                    <tr>
                        <td>
                            <button
                                aria-label="Delete keyB, 2"
                                class="dot-key-value__delete-button">
                                    Delete
                                </button>
                        </td>
                        <td>keyB</td>
                        <td>2</td>
                    </tr>
                `);
            });

            it('should handle invalid items', async () => {
                element.setProperty('items', {
                    a: { key: 'keyA', value: '1' },
                    b: { key: 'keyB', value: '2' }
                });
                await page.waitForChanges();

                const rows = await element.findAll('tr');
                expect(rows.length).toBe(1);
                expect(rows[0]).toEqualHtml(`
                    <tr><td>No values</td></tr>
                `);
            });

            it('should handle empty items', async () => {
                element.setProperty('items', []);
                await page.waitForChanges();

                const rows = await element.findAll('tr');
                expect(rows.length).toBe(1);
                expect(rows[0]).toEqualHtml(`
                    <tr><td>No values</td></tr>
                `);
            });
        });

        describe('disabled', () => {
            beforeEach(async () => {
                element.setProperty('items', [{ key: 'keyA', value: '1' }]);
                await page.waitForChanges();
            });

            it('set disable button', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();

                const button = await getButton();
                expect(button.getAttribute('disabled')).not.toBeNull();
            });

            it('should not set disabled button', async () => {
                element.setProperty('disabled', false);
                await page.waitForChanges();

                const button = await getButton();
                expect(button.getAttribute('disabled')).toBeNull();
            });
        });

        describe('buttonLabel', () => {
            beforeEach(async () => {
                element.setProperty('items', [{ key: 'keyA', value: '1' }]);
                await page.waitForChanges();
            });

            it('should set default label', async () => {
                const button = await getButton();
                expect(button.innerText).toBe('Delete');
            });

            it('should set a label correctly', async () => {
                element.setProperty('buttonLabel', 'Some text');
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('Some text');
            });

            it('should handle label with invalid type', async () => {
                element.setProperty('buttonLabel', []);
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('');
            });
        });

        describe('emptyMessage', () => {
            it('should set default message', async () => {
                const td = await page.find('td');
                expect(td.innerText).toBe('No values');
            });

            it('should set a message correctly', async () => {
                element.setProperty('emptyMessage', 'Some text');
                await page.waitForChanges();

                const td = await page.find('td');
                expect(td.innerText).toBe('Some text');
            });

            it('should handle message with invalid type', async () => {
                element.setProperty('emptyMessage', []);
                await page.waitForChanges();

                const td = await page.find('td');
                expect(td.innerText).toBe('');
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            spyDeleteEvent = await page.spyOnEvent('delete');
            element.setProperty('items', [{ key: 'keyA', value: '1' }]);
            await page.waitForChanges();
        });

        describe('delete', () => {
            it('should emit when click delete button', async () => {
                const deleteBtn = await getButton();
                deleteBtn.click();
                await page.waitForChanges();
                expect(spyDeleteEvent).toHaveReceivedEventDetail(0);
            });
        });
    });
});
