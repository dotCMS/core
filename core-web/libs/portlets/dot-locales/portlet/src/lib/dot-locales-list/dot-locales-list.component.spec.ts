import { Spectator, createComponentFactory, mockProvider, byTestId } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Table } from 'primeng/table';

import {
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import { MockDotMessageService, mockLanguagesISO, mockLocales } from '@dotcms/utils-testing';

import { DotLocalesListComponent } from './dot-locales-list.component';
import { DotLocalesListStore } from './store/dot-locales-list.store';

const messageServiceMock = new MockDotMessageService({
    Default: 'Default'
});

describe('DotLocalesListComponent', () => {
    let spectator: Spectator<DotLocalesListComponent>;
    const createComponent = createComponentFactory({
        component: DotLocalesListComponent,
        imports: [],
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        data: {
                            pushPublishEnvironments: [],
                            isEnterprise: true
                        }
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotHttpErrorManagerService),
            ConfirmationService,
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        componentProviders: [
            DotLocalesListStore,
            DialogService,
            MessageService,
            {
                provide: DotLanguagesService,
                useValue: {
                    get: () => of([...mockLocales]),
                    getISO: () => of(mockLanguagesISO)
                }
            }
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should display locales when component is initialized', fakeAsync(() => {
        spectator.detectChanges();
        tick();

        const localeElements = spectator.queryAll(byTestId('locale-cell'));
        expect(localeElements.length).toEqual(2);
        expect(localeElements[0]).toHaveText('English (en-US)');
    }));

    it('should filter locale when using the filer input', () => {
        const table = spectator.query(Table);
        jest.spyOn(table, 'filterGlobal');

        spectator.detectChanges();

        spectator.typeInElement('Spanish', byTestId('input-search'));

        expect(table.filterGlobal).toHaveBeenCalledWith('Spanish', 'contains');
    });

    it('should display default tag for default locale', fakeAsync(() => {
        spectator.detectChanges();
        tick();

        expect(spectator.query('.p-tag-success')).toHaveText('Default');
    }));

    it('should open AddEditDialog with locale id when row is clicked', fakeAsync(() => {
        spectator.detectChanges();
        tick();

        jest.spyOn(spectator.component.store, 'openAddEditDialog');

        const row = spectator.query(byTestId('locale-row'));

        spectator.click(row);

        expect(spectator.component.store.openAddEditDialog).toHaveBeenCalled();
    }));
});
