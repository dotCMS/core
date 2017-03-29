import { browser, by, element } from 'protractor';

describe('App', () => {

    beforeEach(() => {
        browser.get('/');
    });

    it('should have a title', () => {
        let subject = browser.getTitle();
        let result = 'dotCMS Content Management Platform';
        expect<any>(subject).toEqual(result);
    });
});