import {
    SpectatorService,
    createServiceFactory,
    mockProvider,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { DotEnvironment } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockLanguagesISO, mockLocales } from '@dotcms/utils-testing';

import { DotLocalesListStore } from './dot-locales-list.store';

const messageServiceMock = new MockDotMessageService({
    'locales.edit': 'Edit Locale',
    'locales.add.locale': 'Add Locale',
    'locales.set.as.default': 'Set as default',
    'locales.delete': 'Delete',
    'locales.push.publish': 'Push Publish',
    'contenttypes.content.push_publish': 'Push Publish',
    'locale.delete.confirmation.notification.title': 'Locale deleted',
    'locale.delete.confirmation.notification.message': 'Locale has been deleted'
});

describe('DotLocalesListStore', () => {
    let spectator: SpectatorService<DotLocalesListStore>;
    let languageService: SpyObject<DotLanguagesService>;
    let messageService: SpyObject<MessageService>;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    const storeService = createServiceFactory({
        service: DotLocalesListStore,
        providers: [
            mockProvider(DotLanguagesService),
            mockProvider(DialogService),
            mockProvider(ConfirmationService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotPushPublishDialogService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ],
        imports: [HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = storeService();
        languageService = spectator.inject(DotLanguagesService);
        messageService = spectator.inject(MessageService);
        dotPushPublishDialogService = spectator.inject(DotPushPublishDialogService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);

        languageService.get.mockReturnValue(of([...mockLocales]));
        languageService.getISO.mockReturnValue(of(mockLanguagesISO));
        languageService.makeDefault.mockReturnValue(of(null));
        languageService.delete.mockReturnValue(of(null));

        spectator.service.loadLocales({
            pushPublishEnvironments: [{ test: 1 }] as unknown as DotEnvironment[],
            isEnterprise: true
        });
    });

    it('should load locales', (done) => {
        spectator.service.vm$.subscribe((viewModel) => {
            expect(viewModel.locales.length).toBe(2);
            expect(viewModel.countries).toEqual([...mockLanguagesISO.countries]);
            expect(viewModel.languages).toEqual([...mockLanguagesISO.languages]);

            done();
        });
    });

    it('should make default locale', () => {
        const mockDefaultLocaleId = 1;
        spectator.service.makeDefaultLocale(mockDefaultLocaleId);

        expect(languageService.makeDefault).toHaveBeenCalledWith(mockDefaultLocaleId);
        expect(messageService.add).toHaveBeenCalled();
    });

    it('should delete locale', () => {
        const mockLocaleId = 1;
        spectator.service.deleteLocale(mockLocaleId);

        expect(languageService.delete).toHaveBeenCalledWith(mockLocaleId);
        expect(messageService.add).toHaveBeenCalled();
    });

    it('should open the push publish dialog', () => {
        jest.spyOn(dotPushPublishDialogService, 'open');

        spectator.service.vm$.subscribe((viewModel) => {
            const pushPublishMenuItem = viewModel.locales[0].actions[1].menuItem;
            pushPublishMenuItem.command();

            expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
                assetIdentifier: mockLocales[0].id.toString(),
                title: 'Push Publish'
            });
        });
    });

    it('should set the local actions correctly', (done) => {
        spectator.service.vm$.subscribe((viewModel) => {
            const defaultLocaleActions = viewModel.locales[0].actions.filter((action) =>
                action?.shouldShow ? action.shouldShow() : true
            );
            const notDefaultLocaleActions = viewModel.locales[1].actions.filter((action) =>
                action?.shouldShow ? action.shouldShow() : true
            );

            expect(defaultLocaleActions.length).toEqual(2);
            expect(defaultLocaleActions[0].menuItem.label).toBe('Edit Locale');
            expect(defaultLocaleActions[1].menuItem.label).toBe('Push Publish');

            expect(notDefaultLocaleActions.length).toEqual(4);
            expect(notDefaultLocaleActions[0].menuItem.label).toBe('Edit Locale');
            expect(notDefaultLocaleActions[1].menuItem.label).toBe('Push Publish');
            expect(notDefaultLocaleActions[2].menuItem.label).toBe('Set as default');
            expect(notDefaultLocaleActions[3].menuItem.label).toBe('Delete');
            done();
        });
    });

    it('should handle errors correctly', () => {
        languageService.delete.mockReturnValue(throwError(() => 'test'));

        spectator.service.deleteLocale(1);

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
            'test' as unknown as HttpErrorResponse
        );

        languageService.delete.mockReturnValue(of(null));

        spectator.service.deleteLocale(1);

        expect(messageService.add).toHaveBeenCalledWith({
            severity: 'info',
            summary: 'Locale deleted',
            detail: 'Locale has been deleted'
        });
    });
});
