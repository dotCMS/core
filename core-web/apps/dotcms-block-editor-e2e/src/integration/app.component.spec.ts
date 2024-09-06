describe('dotcms-block-editor', () => {
    beforeEach(() => cy.visit('/iframe.html?id=appcomponent--primary'));

    it('should render the component', () => {
        cy.get('dotcms-root').should('exist');
    });
});
