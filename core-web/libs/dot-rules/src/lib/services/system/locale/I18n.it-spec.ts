import { ReflectiveInjector, Provider } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { UserModel } from '@dotcms/dotcms-js';
import { ApiRoot } from '@dotcms/dotcms-js';

import { I18nService } from './I18n';

const injector = ReflectiveInjector.resolveAndCreate([
    UserModel,
    ApiRoot,
    I18nService,
    BrowserModule
]);

describe('Integration.api.system.locale.I18n', function () {
    ('');
    let rsrcService: I18nService;

    beforeAll(function () {
        rsrcService = injector.get(I18nService);
    });

    beforeEach(function () {});

    it('Can get a specific message.', function (done) {
        console.log('Called - 01', 'can get specific');
        rsrcService.getForLocale('en-US', 'message.comment.success', true).subscribe((rsrc) => {
            console.log('Called - 02', 'can get specific');
            expect(rsrc).toBe('Your comment has been saved');
            rsrcService.getForLocale('de', 'message.comment.success', true).subscribe((rsrc) => {
                expect(rsrc).toBe('Ihr Kommentar wurde gespeichert');
                done();
            });
        });
    });

    it('Can get all message under a particular path.', function (done) {
        const base = 'message.comment';
        rsrcService.getForLocale('en-US', base, false).subscribe((rsrc) => {
            rsrcService.get(base + '.delete').subscribe((v) => {
                expect(v).toBe('Your comment has been delete');
                rsrcService.get(base + '.failure').subscribe((v) => {
                    expect(v).toBe("Your comment couldn't be created");
                    rsrcService.get(base + '.success').subscribe((v) => {
                        expect(v).toBe('Your comment has been saved');
                        done();
                    });
                });
            });
        });
    });

    it('Can get all message under a particular path in a non-default language.', function (done) {
        const base = 'message.comment';
        rsrcService.getForLocale('de', base, false).subscribe((rsrc) => {
            rsrcService.getForLocale('de', base + '.delete').subscribe((v) => {
                expect(v).toBe('Ihr Kommentar wurde gelöscht');
                rsrcService.getForLocale('de', base + '.failure').subscribe((v) => {
                    expect(v).toBe('Ihr Kommentar konnte nicht erstellt werden');
                    rsrcService.getForLocale('de', base + '.success').subscribe((v) => {
                        expect(v).toBe('Ihr Kommentar wurde gespeichert');
                        done();
                    });
                });
            });
        });
    });
});
