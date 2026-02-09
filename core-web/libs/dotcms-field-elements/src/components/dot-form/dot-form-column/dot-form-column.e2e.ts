import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';

import { dotFormLayoutMock } from '../../../test';

describe('dot-form-column', () => {
    let page: E2EPage;
    let element: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form></dot-form>`
        });
        element = await page.find('dot-form');
    });

    describe('columns and fields', () => {
        beforeEach(async () => {
            element.setProperty('layout', dotFormLayoutMock);
            await page.waitForChanges();
        });

        it('should have 3 fields', async () => {
            const fields = await element.findAll('dot-form-column');
            expect(fields.length).toBe(3);
        });

        it('should have CSS class on field', async () => {
            const firstField = await element.find('dot-form-column');
            expect(firstField).toBeDefined();
        });
    });

    describe('@Props', () => {
        describe('column', () => {
            it('should render textfield and keyValue fields', async () => {
                element.setProperty('layout', dotFormLayoutMock);
                await page.waitForChanges();

                const textfield = await element.find('dot-textfield');
                const keyValue = await element.find('dot-key-value');
                expect(textfield).not.toBeNull();
                expect(keyValue).not.toBeNull();
            });

            it('should not render any fields', async () => {
                const fields = await element.findAll('dot-form-column');
                expect(fields.length).toBe(0);
            });
        });

        describe('fieldsToShow', () => {
            it('should only render textfield field', async () => {
                element.setProperty('layout', dotFormLayoutMock);
                element.setProperty('fieldsToShow', 'textfield1');
                await page.waitForChanges();

                const textfield = await element.find('dot-textfield');
                const keyValue = await element.find('dot-key-value');
                expect(textfield).not.toBeNull();
                expect(keyValue).toBeNull();
            });

            it('should render all fields (3)', async () => {
                element.setProperty('layout', dotFormLayoutMock);
                await page.waitForChanges();

                const fields = await element.findAll('dot-form-column');
                expect(fields.length).toBe(3);
            });
        });
    });
});
