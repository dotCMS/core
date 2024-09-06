describe('dotcms-block-editor', () => {
    beforeEach(() => cy.visit('/iframe.html?id=dotblockeditorcomponent--primary'));

    it('should render the component', () => {
        cy.get('dot-block-editor').should('exist');
    });
});
