import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { Observable, Subject, throwError as observableThrowError, of } from 'rxjs';
import { vi } from 'vitest';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { fakeAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotAlertConfirmService,
    DotContainersService,
    DotContentTypeService,
    DotCrudService,
    DotFormatDateService,
    DotHttpErrorHandled,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    PushPublishService
} from '@dotcms/data-access';
import {
    DotPushPublishDialogService,
    HttpCode,
    LoggerService,
    ResponseView,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotCMSContentType, DotContainer } from '@dotcms/dotcms-models';
import {
    dotcmsContentTypeBasicMock,
    MockDotMessageService,
    MockPushPublishService
} from '@dotcms/utils-testing';

import { DotContentTypeInUseDialogComponent } from './components/dot-content-type-in-use-dialog/dot-content-type-in-use-dialog.component';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

import { DotBaseTypeSelectorComponent } from '../../../view/components/dot-base-type-selector/dot-base-type-selector.component';

const DELETE_MENU_ITEM_INDEX = 4;
const ADD_TO_MENU_INDEX = 2;
const ADD_TO_BUNDLE_MENU_ITEM_INDEX = 1;

const messageServiceMock = new MockDotMessageService({
    'contenttypes.form.label.description': 'Description',
    'contenttypes.fieldname.last_edit_date': 'Last',
    'contenttypes.fieldname.entries': 'Entries',
    'contenttypes.fieldname.structure.name': 'Content Type Name',
    'contenttypes.content.variable': 'Variable Name',
    mod_date: 'Last Edit Date',
    'contenttypes.action.delete': 'Delete',
    'contenttypes.action.cancel': 'Cancel',
    'message.structure.cantdelete': 'Delete Content Type',
    'message.contenttypes.in_use.header': 'Delete {0}',
    'contenttypes.content.push_publish': 'Push Publish',
    'contenttypes.content.add_to_bundle': 'Add to bundle',
    'contenttypes.content.add_to_menu': 'Add to Menu',
    'contenttypes.content.copy': 'Copy',
    'contenttypes.content.form': 'Form',
    'contenttypes.content.widget': 'Widget',
    'contenttypes.content.content': 'Content'
});

const mockContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    id: '1234567890',
    name: 'Nuevo',
    variable: 'Nuevo',
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    owner: '123',
    system: false
};

const baseContentTypes = [
    { name: 'CONTENT', label: 'Content', types: [] },
    { name: 'WIDGET', label: 'Widget', types: [] },
    { name: 'FORM', label: 'Form', types: [] }
];

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(true);
    }
}

@Injectable()
class MockDotHttpErrorManagerService {
    handle(_err: ResponseView): Observable<DotHttpErrorHandled> {
        return of({
            redirected: false,
            status: HttpCode.BAD_REQUEST
        });
    }
}

Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn()
    }))
});

describe('DotContentTypesPortletComponent', () => {
    let spectator: Spectator<DotContentTypesPortletComponent>;
    let crudService: DotCrudService;
    let route: ActivatedRoute;
    let dotContentTypeService: DotContentTypeService;
    let pushPublishService: PushPublishService;
    let dotLicenseService: DotLicenseService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPushPublishDialogService: DotPushPublishDialogService;

    const createComponent = createComponentFactory({
        component: DotContentTypesPortletComponent,
        imports: [
            RouterTestingModule.withRoutes([
                { path: 'test', component: DotContentTypesPortletComponent }
            ])
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            DotAlertConfirmService,
            DotFormatDateService,
            DotPushPublishDialogService,
            ConfirmationService,
            LoggerService,
            StringUtils,
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: PushPublishService, useClass: MockPushPublishService },
            { provide: DotLicenseService, useClass: MockDotLicenseService },
            {
                provide: DotHttpErrorManagerService,
                useClass: MockDotHttpErrorManagerService
            },
            {
                provide: DotContainersService,
                useFactory: () => ({ getByContentType: vi.fn() })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        route = spectator.inject(ActivatedRoute);
        crudService = spectator.inject(DotCrudService, true);
        dotContentTypeService = spectator.inject(DotContentTypeService, true);
        pushPublishService = spectator.inject(PushPublishService);
        dotLicenseService = spectator.inject(DotLicenseService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotPushPublishDialogService = spectator.inject(DotPushPublishDialogService);

        vi.spyOn(dotContentTypeService, 'getAllContentTypes').mockReturnValue(of(baseContentTypes));
        vi.mocked(spectator.inject(DotContainersService).getByContentType).mockReset();
    });

    /**
     * The listing is behind `@if (showTable)`, which the component flips in a deferred callback.
     */
    const renderListing = (): void => {
        spectator.detectChanges();
        spectator.tick(1);
    };

    it('should display a listing-data-table.component', fakeAsync(() => {
        renderListing();

        expect(spectator.query('dot-listing-data-table')).toHaveAttribute('url', 'v1/contenttype');

        const columns = spectator.component.contentTypeColumns;
        expect(5).toEqual(columns.length);

        /*
            TODO: needs to compare the whole array and not each entry.
        */
        expect('name').toEqual(columns[0].fieldName);
        expect('Content Type Name').toEqual(columns[0].header);

        expect('variable').toEqual(columns[1].fieldName);
        expect('Variable Name').toEqual(columns[1].header);

        expect('description').toEqual(columns[2].fieldName);
        expect('Description').toEqual(columns[2].header);

        expect('nEntries').toEqual(columns[3].fieldName);
        expect('Entries').toEqual(columns[3].header);
        expect('8%').toEqual(columns[3].width);

        expect('modDate').toEqual(columns[4].fieldName);
        expect('Last').toEqual(columns[4].header);
        expect('13%').toEqual(columns[4].width);
    }));

    it('should remove the content type on click command function', () => {
        spectator.detectChanges();

        const dotDialogService = spectator.inject(DotAlertConfirmService);
        vi.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });

        vi.spyOn(crudService, 'delete').mockReturnValue(of(mockContentType));

        // The table is rendered BEFORE the command runs: removeContentType() reaches
        // for the `$listing()` viewChild on success, and that viewChild lives behind
        // `@if (showTable)` — which the component only flips in a deferred callback. An
        // unrendered table left it undefined, and the resulting throw sat inside a
        // subscribe handler, where rxjs reports it asynchronously and the runner dropped it.
        spectator.component.showTable = true;
        spectator.detectChanges();

        const getByContentType = vi.mocked(spectator.inject(DotContainersService).getByContentType);
        getByContentType.mockReturnValue(of({ containers: [], total: 0 }));
        const dialogService = spectator.inject(DialogService, true);
        vi.spyOn(dialogService, 'open');

        spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(mockContentType);

        expect(getByContentType).toHaveBeenCalledWith(mockContentType.id);
        expect(dialogService.open).not.toHaveBeenCalled();
        expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', mockContentType.id);
        expect(crudService.delete).toHaveBeenCalledTimes(1);
    });

    it('should have remove, push publish, Copy and Add to bundle actions to the list item', () => {
        spectator.detectChanges();

        expect(spectator.component.rowActions.map((action) => action.menuItem['label'])).toEqual([
            'Push Publish',
            'Add to bundle',
            'Add to Menu',
            'Copy',
            'Delete'
        ]);
    });

    it('should have ONLY remove action because is community license', () => {
        vi.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));

        spectator.detectChanges();
        expect(
            spectator.component.rowActions.map((action) => {
                return {
                    label: action.menuItem.label,
                    icon: action.menuItem.icon
                };
            })
        ).toEqual([
            {
                label: 'Delete',
                icon: 'pi pi-trash'
            }
        ]);
    });

    it('should have remove and add to bundle actions if is not community license and no publish environments are created', () => {
        vi.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(of([]));
        spectator.detectChanges();

        expect(spectator.component.rowActions.map((action) => action.menuItem['label'])).toEqual([
            'Add to bundle',
            'Add to Menu',
            'Copy',
            'Delete'
        ]);
    });

    it('should open push publish dialog', () => {
        spectator.detectChanges();
        vi.spyOn(dotPushPublishDialogService, 'open');

        expect(spectator.query('p-dialog')).toBeNull();

        spectator.component.rowActions[0].menuItem.command!(mockContentType);
        spectator.detectChanges();
        expect(spectator.query('p-dialog')).toBeDefined();
        expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
            assetIdentifier: mockContentType.id,
            title: 'Push Publish'
        });
    });

    it('should open add to bundle dialog', fakeAsync(() => {
        renderListing();
        expect(spectator.component.addToBundleIdentifier).not.toBeDefined();

        spectator.component.rowActions[ADD_TO_BUNDLE_MENU_ITEM_INDEX].menuItem.command!(
            mockContentType
        );

        expect(spectator.component.addToBundleIdentifier).toEqual(mockContentType.id);
    }));

    it('should open Add to Menu dialog', fakeAsync(() => {
        renderListing();
        expect(spectator.component.addToMenuContentType).not.toBeDefined();

        spectator.component.rowActions[ADD_TO_MENU_INDEX].menuItem.command!(mockContentType);

        expect(spectator.component.addToMenuContentType).toEqual(mockContentType);
    }));

    it('should populate the actionHeaderOptions based on a call to dotContentTypeService', () => {
        spectator.detectChanges();
        expect(dotContentTypeService.getAllContentTypes).toHaveBeenCalled();
        expect(spectator.component.actionHeaderOptions.primary!.model!.length).toEqual(3);
    });

    it('should not set primary command in the header options', () => {
        spectator.detectChanges();
        expect(spectator.component.actionHeaderOptions.primary!.command).toBe(undefined);
    });

    it('should emit changes in base types selector', fakeAsync(() => {
        renderListing();
        const baseTypesSelector = spectator.query(DotBaseTypeSelectorComponent);
        vi.spyOn(spectator.component, 'changeBaseTypeSelector');
        baseTypesSelector!.selected.emit('test');

        expect(spectator.component.changeBaseTypeSelector).toHaveBeenCalledWith('test');
        expect(spectator.component.changeBaseTypeSelector).toHaveBeenCalledTimes(1);
    }));

    it('should handle error if is not possible delete the content type', () => {
        const forbiddenError = {
            bodyJsonObject: {
                error: ''
            },
            response: {
                status: 403
            }
        };

        spectator.detectChanges();

        const dotDialogService = spectator.inject(DotAlertConfirmService);
        vi.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });

        vi.spyOn(dotHttpErrorManagerService, 'handle');
        vi.spyOn(crudService, 'delete').mockReturnValue(observableThrowError(forbiddenError));
        vi.mocked(spectator.inject(DotContainersService).getByContentType).mockReturnValue(
            of({ containers: [], total: 0 })
        );
        spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(mockContentType);

        spectator.detectChanges();

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
            forbiddenError as unknown as HttpErrorResponse
        );
    });

    it('should show remove option', () => {
        spectator.detectChanges();

        const shouldShow = spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].shouldShow!({
            fixed: false,
            defaultType: false
        });

        expect(shouldShow).toBeTruthy();
    });

    it('should not show remove option if content type is defaultType', () => {
        spectator.detectChanges();
        const shouldShow = spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].shouldShow!({
            fixed: false,
            defaultType: true
        });
        expect(shouldShow).toBeFalsy();
    });

    it('should not show Add To Menu option if content type is HOST', () => {
        spectator.detectChanges();
        const shouldShow = spectator.component.rowActions[ADD_TO_MENU_INDEX].shouldShow!({
            variable: 'Host'
        });
        expect(shouldShow).toBeFalsy();
    });

    it('should show Add to Menu option', () => {
        spectator.detectChanges();
        expect(spectator.component.rowActions[ADD_TO_MENU_INDEX].menuItem['label']).toBe(
            'Add to Menu'
        );
    });

    describe('filterBy', () => {
        beforeEach(() => {
            route.data = of({
                filterBy: 'FORM'
            });
        });

        it('should not display base types selector', fakeAsync(() => {
            renderListing();
            expect(spectator.query(DotBaseTypeSelectorComponent)).toBeNull();
        }));

        it('should set filterBy params', fakeAsync(() => {
            renderListing();
            expect(spectator.component.filterBy).toBe('Form');
            expect(spectator.component.$listing()!.paginatorService.extraParams.get('type')).toBe(
                'Form'
            );
            expect(spectator.component.actionHeaderOptions.primary!.model).toBe(null);
            expect(spectator.component.actionHeaderOptions.primary!.command).toBeDefined();
        }));
    });

    describe('delete with containers using the content type', () => {
        const containers = [
            { identifier: 'a', title: 'A' },
            { identifier: 'b', title: 'B' }
        ] as DotContainer[];

        let dialogService: DialogService;
        let dotDialogService: DotAlertConfirmService;
        let getByContentType: ReturnType<typeof vi.fn>;
        let loadCurrentPage: ReturnType<typeof vi.fn>;

        const answerDialogWith = (confirmed: boolean | undefined): void => {
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(confirmed)
            } as unknown as DynamicDialogRef);
        };

        beforeEach(() => {
            spectator.detectChanges();
            spectator.component.showTable = true;
            spectator.detectChanges();

            dialogService = spectator.inject(DialogService, true);
            dotDialogService = spectator.inject(DotAlertConfirmService);
            vi.spyOn(dotDialogService, 'confirm');
            vi.spyOn(crudService, 'delete').mockReturnValue(of(mockContentType));
            const listing = spectator.component.$listing();
            if (!listing) {
                throw new Error('the listing table did not render');
            }
            loadCurrentPage = vi.spyOn(listing, 'loadCurrentPage');
            getByContentType = vi.mocked(spectator.inject(DotContainersService).getByContentType);
        });

        it('should warn about the containers using the content type and delete it on Delete', () => {
            // Given: the containers lookup answers two containers and the dialog answers Delete
            getByContentType.mockReturnValue(of({ containers, total: 2 }));
            answerDialogWith(true);

            // When: the Delete row action runs
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            // Then: the dialog lists the containers, the content type is deleted and the list reloads
            expect(dotDialogService.confirm).not.toHaveBeenCalled();
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(dialogService.open).toHaveBeenCalledWith(
                DotContentTypeInUseDialogComponent,
                expect.objectContaining({
                    header: 'Delete Nuevo',
                    closable: true,
                    closeOnEscape: true,
                    data: { containers, total: 2 }
                })
            );
            expect(crudService.delete).toHaveBeenCalledTimes(1);
            expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', '1234567890');
            expect(loadCurrentPage).toHaveBeenCalledTimes(1);
        });

        it('should not delete the content type when the in-use dialog is cancelled', () => {
            // Given: the containers lookup answers two containers and the dialog answers Cancel
            getByContentType.mockReturnValue(of({ containers, total: 2 }));
            answerDialogWith(false);

            // When: the Delete row action runs
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            // Then: nothing is deleted
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(crudService.delete).not.toHaveBeenCalled();
        });

        it('should not delete the content type when the in-use dialog is dismissed', () => {
            // Given: the containers lookup answers two containers and the dialog closes without an answer
            getByContentType.mockReturnValue(of({ containers, total: 2 }));
            answerDialogWith(undefined);

            // When: the Delete row action runs
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            // Then: no confirm is shown and nothing is deleted
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(dotDialogService.confirm).not.toHaveBeenCalled();
            expect(crudService.delete).not.toHaveBeenCalled();
        });

        it('should handle a failed container lookup and not delete the content type', () => {
            // Given: the containers lookup fails
            const forbiddenError = {
                bodyJsonObject: {
                    error: ''
                },
                response: {
                    status: 403
                }
            };
            getByContentType.mockReturnValue(observableThrowError(forbiddenError));
            answerDialogWith(true);
            vi.spyOn(dotHttpErrorManagerService, 'handle');

            // When: the Delete row action runs
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            // Then: the error is handled and nothing is shown or deleted
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(
                forbiddenError as unknown as HttpErrorResponse
            );
            expect(dotDialogService.confirm).not.toHaveBeenCalled();
            expect(dialogService.open).not.toHaveBeenCalled();
            expect(crudService.delete).not.toHaveBeenCalled();

            // And: a later Delete looks the containers up again
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            expect(getByContentType).toHaveBeenCalledTimes(2);
        });

        it('should ignore a second Delete while the containers lookup is in flight', () => {
            // Given: a containers lookup that has not answered yet
            const lookup = new Subject<{ containers: DotContainer[]; total: number }>();
            getByContentType.mockReturnValue(lookup);
            answerDialogWith(true);

            // When: the Delete row action runs twice before the lookup answers
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            // Then: only one lookup is sent
            expect(getByContentType).toHaveBeenCalledTimes(1);

            // When: the lookup answers two containers
            lookup.next({ containers, total: 2 });
            lookup.complete();

            // Then: one dialog opens and one delete is sent
            expect(dialogService.open).toHaveBeenCalledTimes(1);
            expect(crudService.delete).toHaveBeenCalledTimes(1);

            // And: once the lookup is over, a later Delete looks the containers up again
            spectator.component.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command!(
                mockContentType
            );

            expect(getByContentType).toHaveBeenCalledTimes(2);
        });
    });
});
