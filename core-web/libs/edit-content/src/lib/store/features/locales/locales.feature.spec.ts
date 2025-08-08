/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { patchState, signalStore, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';
import { MOCK_SINGLE_WORKFLOW_ACTIONS } from '@dotcms/utils-testing';

import { withLocales } from './locales.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { initialRootState } from '../../edit-content.store';

const MOCK_LANGUAGES = [
    { id: 1, isoCode: 'en-us', translated: true },
    { id: 2, isoCode: 'es-es', translated: true },
    { id: 3, isoCode: 'it-it', translated: false }
] as DotLanguage[];

const SYSTEM_LANGUAGES = [
    { id: 1, isoCode: 'en-us', defaultLanguage: false },
    { id: 2, isoCode: 'it-it', defaultLanguage: true }
] as DotLanguage[];

const withTest = () =>
    signalStoreFeature(
        withState({
            ...initialRootState,
            formValues: {}
        }),
        withMethods((store) => ({
            updateContent: (content) => {
                patchState(store, { contentlet: content });
            }
        }))
    );

const Store = signalStore({ providedIn: 'root' }, withTest(), withLocales());

describe('LocalesFeature', () => {
    let spectator: SpectatorService<any>;

    let store: InstanceType<typeof Store>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;
    let dotContentletService: SpyObject<DotContentletService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dialogService: SpyObject<DialogService>;
    let router: SpyObject<Router>;
    let workflowActionService: SpyObject<DotWorkflowsActionsService>;

    const createStore = createServiceFactory({
        service: Store,
        mocks: [
            DotLanguagesService,
            DotContentletService,
            DotHttpErrorManagerService,
            DotEditContentService,
            DotMessageService,
            DialogService,
            DynamicDialogConfig,
            DynamicDialogRef,
            Router,
            DotWorkflowsActionsService
        ]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotLanguagesService = spectator.inject(DotLanguagesService);
        dotContentletService = spectator.inject(DotContentletService);
        dotEditContentService = spectator.inject(DotEditContentService);
        dialogService = spectator.inject(DialogService);
        router = spectator.inject(Router);
        workflowActionService = spectator.inject(DotWorkflowsActionsService);

        workflowActionService.getDefaultActions.mockReturnValue(of(MOCK_SINGLE_WORKFLOW_ACTIONS));
    });

    it('should load locales when a new contentlet is loaded', fakeAsync(() => {
        dotContentletService.getLanguages.mockReturnValue(of(MOCK_LANGUAGES));
        dotLanguagesService.getDefault.mockReturnValue(of(MOCK_LANGUAGES[1]));

        expect(store.localesStatus().status).toEqual(ComponentStatus.INIT);

        store.updateContent({ identifier: '123' } as DotCMSContentlet);

        spectator.flushEffects();

        expect(dotContentletService.getLanguages).toHaveBeenCalledWith('123');
        expect(dotLanguagesService.getDefault).toHaveBeenCalledTimes(1);
        expect(store.localesStatus().status).toEqual(ComponentStatus.LOADED);
        expect(store.locales()).toEqual(MOCK_LANGUAGES);
    }));

    it('should load system locales when there is no contentlet', fakeAsync(() => {
        dotLanguagesService.get.mockReturnValue(of(SYSTEM_LANGUAGES));

        store.loadSystemLocales();
        tick();

        expect(dotLanguagesService.get).toHaveBeenCalledTimes(1);
        expect(store.localesStatus().status).toEqual(ComponentStatus.LOADED);
        expect(store.locales()).toEqual(SYSTEM_LANGUAGES);
        expect(store.systemDefaultLocale()).toEqual(SYSTEM_LANGUAGES[1]);
        expect(store.currentLocale()).toEqual(SYSTEM_LANGUAGES[1]);
    }));

    describe('when there is switch of locale', () => {
        beforeEach(() => {
            const mockContentlet = { inode: '456' } as DotCMSContentlet;
            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            dotContentletService.getLanguages.mockReturnValue(of(MOCK_LANGUAGES));
            dotLanguagesService.getDefault.mockReturnValue(of(MOCK_LANGUAGES[0]));
            store.updateContent({ identifier: '123', languageId: 1 } as DotCMSContentlet);
        });

        it('should switch to a translated locale', fakeAsync(() => {
            spectator.flushEffects();
            store.switchLocale(MOCK_LANGUAGES[1]);
            spectator.flushEffects();

            expect(dotEditContentService.getContentById).toHaveBeenCalledWith({
                id: '123',
                languageId: 2
            });

            expect(router.navigate).toHaveBeenCalledWith(['/content', '456'], {
                replaceUrl: true,
                queryParamsHandling: 'preserve'
            });
        }));

        it('should open dialog and update state for untranslated locale doing populate copy', fakeAsync(() => {
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of('populate')
            } as DynamicDialogRef);

            tick();
            store.switchLocale(MOCK_LANGUAGES[2]);

            tick();

            expect(dialogService.open).toHaveBeenCalledTimes(1);

            const expectedContentlet = {
                identifier: '123',
                languageId: 1,
                locked: false,
                lockedBy: undefined
            } as DotCMSContentlet;

            expect(store.currentLocale()).toEqual(MOCK_LANGUAGES[2]);
            expect(store.initialContentletState()).toEqual('copy');
            expect(store.contentlet()).toEqual(expectedContentlet);
            expect(store.formValues()).toEqual(null);
        }));

        it('should open dialog, update state for untranslated locale doing manual copy', fakeAsync(() => {
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of('manual')
            } as DynamicDialogRef);

            tick();
            store.switchLocale(MOCK_LANGUAGES[2]);

            tick();

            expect(dialogService.open).toHaveBeenCalledTimes(1);

            expect(store.contentlet()).toEqual(null);
        }));
    });
});
