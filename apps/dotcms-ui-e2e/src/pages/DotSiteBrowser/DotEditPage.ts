import Navigation from '@e2e/shared/Navigation';
import Page from '@e2e/shared/Page';

const URL = '/dotAdmin/#/edit-page/content';
const IFRAME = 'iframe.dot-edit__iframe';
const DIALOG_IFRAME = 'iframe#detailFrame';

class DotEditPage {
    static openPage(page: string) {
        const url = `${URL}?url=${page.replace(' ', '-')}`;
        Navigation.visit(url);
        Navigation.assertPageUrlIs(url);
    }

    static goToMode({ type }: { type: string }) {
        Page.click(cy.get('p-selectbutton .p-button').contains(type));
    }

    static openAddToContainerDialog({ type }: { type: string }) {
        cy.intercept('GET', 'appconfiguration').as('appConfiguration');
        cy.wait('@appConfiguration');

        cy.get(IFRAME)
            .iframe()
            .then((iframe) => {
                Page.click(cy.get(iframe).find('#section-1 .dotedit-menu__button'));
                Page.click(cy.get(iframe).find('#section-1 .dotedit-menu__item').contains(type));
            });
    }

    static addElementToContainer({ elementIndex }: { elementIndex: number }) {
        cy.intercept('POST', 'ContentletAjax.searchContentlets.dwr').as('searchContentlets');
        cy.wait('@searchContentlets');
        cy.get(DIALOG_IFRAME)
            .iframe()
            .then((iframe) => {
                Page.click(cy.get(iframe).find(`#rowId${elementIndex} #${elementIndex}`));
            });
    }

    static checkToastMessage({ message }: { message: string }) {
        Page.assertElementSize('p-toastitem', 1);
        Page.assertElementContainsText('p-toastitem dot-icon + span', message);
    }

    static checkContainerMustHaveElements({
        container,
        size
    }: {
        container: string;
        size: number;
    }) {
        cy.get(IFRAME)
            .iframe()
            .then((iframe) => {
                cy.get(iframe)
                    .find(`${container} .row > div`)
                    .children()
                    .its('length')
                    .should('be.gte', size + 1);
            });
    }
}

export default DotEditPage;