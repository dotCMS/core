import { E2EPage, newE2EPage } from '@stencil/core/testing';
import { DotContentState } from '@dotcms/dotcms-models';
import { E2EElement } from '@stencil/core/testing/puppeteer/puppeteer-declarations';

describe('dot-state-icon', () => {
    let page: E2EPage;
    let element, tooltip, icon: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-state-icon></dot-state-icon>`
        });
        await page.waitForChanges();
        tooltip = await page.find('dot-state-icon >>> dot-tooltip');
        icon = await page.find('dot-state-icon >>> div');
        element = await page.find('dot-state-icon');
    });

    it('should set Archived attributes', async () => {
        const archivedState: DotContentState = {
            live: 'false',
            working: 'true',
            deleted: 'true',
            hasLiveVersion: 'false'
        };

        element.setAttribute('size', '14px');
        element.setProperty('state', archivedState);
        await page.waitForChanges();

        expect(element.getAttribute('aria-label')).toEqual('Archived');
        expect(icon.getAttribute('class')).toEqual('archived');
        expect(await tooltip.getProperty('content')).toEqual('Archived');
    });

    it('should set Published attributes', async () => {
        const publishedState: DotContentState = {
            live: 'true',
            working: 'true',
            deleted: 'false',
            hasLiveVersion: 'true'
        };

        element.setProperty('state', publishedState);
        await page.waitForChanges();

        expect(element.getAttribute('aria-label')).toEqual('Published');
        expect(icon.getAttribute('class')).toEqual('published');
        expect(await tooltip.getProperty('content')).toEqual('Published');
    });

    it('should set Revision attributes', async () => {
        const revisionState: DotContentState = {
            live: 'false',
            working: 'false',
            deleted: 'false',
            hasLiveVersion: 'true'
        };

        element.setProperty('state', revisionState);
        await page.waitForChanges();

        expect(element.getAttribute('aria-label')).toEqual('Revision');
        expect(icon.getAttribute('class')).toEqual('revision');
        expect(await tooltip.getProperty('content')).toEqual('Revision');
    });

    it('should set Draft attributes', async () => {
        const draftState: DotContentState = {
            live: false,
            working: true,
            deleted: false,
            hasLiveVersion: false
        };

        element.setProperty('state', draftState);
        await page.waitForChanges();

        expect(element.getAttribute('aria-label')).toEqual('Draft');
        expect(icon.getAttribute('class')).toEqual('draft');
        expect(await tooltip.getProperty('content')).toEqual('Draft');
    });
});
