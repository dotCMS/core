class Form {
    static fill(selector: string | Cypress.Chainable, text: string) {
        if (typeof selector === 'string') {
            cy.get(selector).type(text);
        } else {
            selector.type(text);
        }
    }

    static submit(selector: string) {
        cy.get(selector).get('input[type="submit"]').click();
    }
}

export default Form;
