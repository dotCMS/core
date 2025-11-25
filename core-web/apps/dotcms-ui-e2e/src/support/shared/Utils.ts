class Utils {
    static login(username?: string, password?: string): Promise<string> {
        return new Cypress.Promise((resolve) => {
            if (localStorage.getItem('token')) {
                resolve(localStorage.getItem('token'));
            } else {
                cy.request({
                    method: 'POST',
                    url: '/api/v1/authentication/api-token',
                    body: {
                        user: username || Cypress.env('adminUsername'),
                        password: password || Cypress.env('adminPassword')
                    }
                }).then((response: Cypress.Response) => {
                    localStorage.setItem('token', response.body.entity.token);
                    resolve(response.body.entity.token);
                });
            }
        });
    }

    static DBSeed(): Promise<void> {
        return Utils.login().then(() => {
            // Declarations
            const fileName = 'Cypress-DB-Seed.tar.gz';
            const method = 'POST';
            const url = '/api/bundle/sync';
            const fileType = 'application/gzip';

            return new Cypress.Promise((resolve) => {
                // Get file from fixtures as binary
                cy.fixture(fileName, 'binary').then((bundle) => {
                    // File in binary format gets converted to blob so it can be sent as Form data
                    const blob = Cypress.Blob.binaryStringToBlob(bundle, fileType);

                    // Build up the form
                    const formData = new FormData();
                    formData.set('file', blob, fileName); // adding a file to the form

                    // Perform the request
                    cy.form_request(method, url, formData, (response) => {
                        expect(response.status).to.eq(200);
                        resolve();
                    });
                });
            });
        });
    }
}
export default Utils;
