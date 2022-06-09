import DotEditPage from '@e2e/pages/DotSiteBrowser/DotEditPage';
import DotSiteBrowser from '@e2e/pages/DotSiteBrowser/DotSiteBrowser';
import Templates from '@e2e/pages/DotTemplates/DotTemplates';
import Utils from '@e2e/shared/Utils';

const PAGE_NAME = `Cypress Page ${Date.now()}`;

describe('Templates', () => {
    beforeEach(async () => {
        await Utils.login();
    });

    it('Create Template and create Page with content using previous template', () => {
        // Load Page
        Templates.openPage();
        Templates.checkTemplatesPageLoaded();

        // Create Template
        Templates.openCreateTemplateDialog();
        Templates.fillCreateTemplateForm({ title: 'CypressTemplate', theme: 'default' });
        Templates.submitCreateTemplateForm();

        // Edit Template
        Templates.checkEditTemplatesPageLoaded();
        Templates.addContainer({ type: 'Default - default' });
        Templates.saveEditTemplate();

        // Site Browser
        cy.log('Opening DotSiteBrowser');

        DotSiteBrowser.openPage();
        DotSiteBrowser.checkSiteBrowserPageLoaded();
        DotSiteBrowser.openCreatePageDialog({ type: 'Page' });

        DotSiteBrowser.fillCreatePageForm({
            title: PAGE_NAME,
            template: 'CypressTemplatedefault'
        });

        DotSiteBrowser.submitCreatePageForm({ action: 'Save' });

        DotEditPage.goToMode({ type: 'Edit' });
        DotEditPage.openAddToContainerDialog({ type: 'Content' });
        DotEditPage.addElementToContainer({ elementIndex: 0 });
        DotEditPage.checkToastMessage({ message: 'All changes Saved' });

        DotEditPage.goToMode({ type: 'Preview' });
        DotEditPage.checkContainerMustHaveElements({ container: '#section-1', size: 1 });
    });
});
