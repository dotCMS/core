import Form from '@e2e/shared/Form';
import Navigation from '@e2e/shared/Navigation';
import Page from '@e2e/shared/Page';

const URL = '/dotAdmin/#/templates';
const CREATE_TEMPLATE_BUTTON = 'dot-action-button dot-icon i';

class DotTemplates {
    static openPage() {
        cy.intercept('GET', 'v1/templates').as('templates');
        Navigation.visit(URL);
        Navigation.assertPageUrlIs(URL);
        cy.wait('@templates');
    }

    static checkTemplatesPageLoaded() {
        Page.assertElementContainsText('.p-breadcrumb > ul > :nth-child(3)', `Templates`); // Header
        Page.assertElementSize('p-table', 1); // Table
        Page.assertElementSize('.action-header__global-search input[type="text"]', 1); // Global Search Input
        Page.assertElementContainsText(
            '.template-listing__header-options p-checkbox',
            `Show Archived`
        ); // Show Archived button
        Page.assertElementContainsText(
            '.template-listing__header-options .p-button-label',
            `Actions`
        ); // BulkAction button
        Page.assertElementContainsText(CREATE_TEMPLATE_BUTTON, `add`); // Action button
    }

    static openCreateTemplateDialog() {
        Page.click(CREATE_TEMPLATE_BUTTON);
        Page.assertElementContainsText('.p-dialog-title', 'Create a template');
        Page.assertElementContainsText('label[data-testid="designer"] span', 'Designer');
        Page.assertElementContainsText('label[data-testid="advanced"] span', 'Advanced');
        Page.assertElementContainsText(
            'p[data-testid="description"]',
            'Template Designer create reusable templates using a drag and drop interface and tools.'
        );
        Page.assertElementContainsText('dot-dot-template-selector button', 'Next').click();
    }

    static fillCreateTemplateForm({ title, theme }: { title: string; theme: string }) {
        // TODO: Complete to fill extra fields (not required)
        Form.fill('input[data-testid="templatePropsTitleField"]', title);
        Page.click('dot-theme-selector-dropdown[data-testid="templatePropsThemeField"]');
        Page.click(
            cy.get('.theme-selector__data-list-item .dot-theme-item__label').contains(theme)
        );
    }

    static submitCreateTemplateForm() {
        Page.click('button[data-testid="dotFormDialogSave"');
    }

    static checkEditTemplatesPageLoaded() {
        // TODO: Check other components loaded on Edit Template page
        Page.assertElementContainsText('#p-tabpanel-0-label .p-tabview-title', `design`); // Secondary Menu bar
        Page.assertElementContainsText('#p-tabpanel-1-label .p-tabview-title', `Permissions`); // Secondary Menu bar
        Page.assertElementContainsText('#p-tabpanel-2-label .p-tabview-title', `History`); // Secondary Menu bar
        Page.assertElementContainsText('.dot-edit-layout__toolbar-save', `Save`); // Save button
        Page.assertElementContainsText('dot-searchable-dropdown button', `Add a Container`); // Add container select
    }

    static addContainer({ type }: { type: string }) {
        Page.click('dot-container-selector-layout dot-searchable-dropdown button');
        Page.click(
            cy
                .get('.searchable-dropdown__data-list .searchable-dropdown__data-list-item')
                .contains(type)
        );
    }

    static saveEditTemplate() {
        cy.intercept('PUT', 'api/v1/templates').as('templates');
        Page.click('.dot-edit-layout__toolbar-save');
        cy.wait('@templates');
    }
}

export default DotTemplates;
