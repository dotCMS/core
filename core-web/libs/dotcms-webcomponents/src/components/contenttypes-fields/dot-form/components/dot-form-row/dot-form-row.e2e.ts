import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';
import { dotFormLayoutMock } from '../../../../../test';

describe('dot-form-row', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form></dot-form>`
        });
        element = await page.find('dot-form');
    });

    describe('columns', () => {
        beforeEach(async () => {
            element.setProperty('layout', dotFormLayoutMock);
            element.setProperty('fieldsToShow', 'test');
            await page.waitForChanges();
        });

        it('should have 3 columns', async () => {
            const columns = await element.findAll('dot-form-column');
            expect(columns.length).toBe(3);
        });

        it('should set values on dot-form-row', async () => {
            const firstColumn = await element.find('dot-form-column');
            expect(await firstColumn.getProperty('column')).toEqual(
                dotFormLayoutMock[0].columns[0]
            );
            expect(await firstColumn.getProperty('fieldsToShow')).toEqual('test');
        });
    });
});
