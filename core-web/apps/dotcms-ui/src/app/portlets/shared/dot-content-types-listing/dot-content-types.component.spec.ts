/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, throwError as observableThrowError, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, SelectItem } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotContentTypesInfoService,
    DotCrudService,
    DotFormatDateService,
    DotHttpErrorHandled,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    PushPublishService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotPushPublishDialogService,
    HttpCode,
    LoggerService,
    ResponseView,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotCMSContentType, DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    MockDotMessageService,
    MockPushPublishService
} from '@dotcms/utils-testing';

import { DotContentTypeStore } from './dot-content-type.store';
import { DotContentTypesPortletComponent } from './dot-content-types.component';

import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';

const DELETE_MENU_ITEM_INDEX = 4;
const ADD_TO_MENU_INDEX = 2;
const ADD_TO_BUNDLE_MENU_ITEM_INDEX = 1;

@Injectable()
class MockDotContentTypeService {
    getAllContentTypes() {}
}

@Component({
    selector: 'dot-dot-content-type-copy-dialog',
    template: '',
    standalone: false
})
class MockDotContentTypeCloneDialogComponent {
    @Input()
    isVisibleDialog = false;
    @Input()
    isSaving = false;
    @Output() cancelBtn = new EventEmitter<boolean>();

    @Output()
    validFormFields = new EventEmitter<DotCopyContentTypeDialogFormFields>();
}

@Component({
    selector: 'dot-base-type-selector',
    template: '',
    standalone: false
})
class MockDotBaseTypeSelectorComponent {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();
}

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

@Injectable()
class MockDotContentTypeStore {}

@Component({
    selector: 'dot-add-to-bundle ',
    template: ``,
    standalone: false
})
class MockDotAddToBundleComponent {
    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
}

@Component({
    selector: 'dot-portlet-base',
    template: '<ng-content></ng-content>'
})
class MockDotPortletBaseComponent {
    @Input() boxed = true;
}

@Component({
    selector: 'dot-add-to-menu',
    template: ''
})
class MockDotAddToMenuComponent {
    @Input() contentType;
    @Output() cancel = new EventEmitter<boolean>();
}

Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

describe('DotContentTypesPortletComponent', () => {
    let comp: DotContentTypesPortletComponent;
    let fixture: ComponentFixture<DotContentTypesPortletComponent>;
    let de: DebugElement;
    let crudService: DotCrudService;
    let router: ActivatedRoute;
    let dotContentletService: DotContentTypeService;
    let pushPublishService: PushPublishService;
    let dotLicenseService: DotLicenseService;
    let baseTypesSelector: MockDotBaseTypeSelectorComponent;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPushPublishDialogService: DotPushPublishDialogService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.form.label.description': 'Description',
            'contenttypes.fieldname.last_edit_date': 'Last',
            'contenttypes.fieldname.entries': 'Entries',
            'contenttypes.fieldname.structure.name': 'Content Type Name',
            'contenttypes.content.variable': 'Variable Name',
            mod_date: 'Last Edit Date',
            'contenttypes.action.delete': 'Delete',
            'contenttypes.content.push_publish': 'Push Publish',
            'contenttypes.content.add_to_bundle': 'Add to bundle',
            'contenttypes.content.add_to_menu': 'Add to Menu',
            'contenttypes.content.copy': 'Copy',
            'contenttypes.content.form': 'Form',
            'contenttypes.content.widget': 'Widget',
            'contenttypes.content.content': 'Content'
        });

        TestBed.configureTestingModule({
            declarations: [
                MockDotBaseTypeSelectorComponent,
                MockDotAddToBundleComponent,
                MockDotContentTypeCloneDialogComponent
            ],
            imports: [
                DotContentTypesPortletComponent,
                RouterTestingModule.withRoutes([
                    { path: 'test', component: DotContentTypesPortletComponent }
                ]),
                BrowserAnimationsModule,
                DotListingDataTableComponent,
                ReactiveFormsModule,
                HttpClientTestingModule,
                MockDotPortletBaseComponent,
                MockDotAddToMenuComponent
            ],
            providers: [
                DotContentTypesInfoService,
                DotCrudService,
                DotAlertConfirmService,
                DotFormatDateService,
                DotPushPublishDialogService,
                DotAlertConfirmService,
                ConfirmationService,
                LoggerService,
                StringUtils,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotContentTypeService, useClass: MockDotContentTypeService },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PushPublishService, useClass: MockPushPublishService },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                { provide: DotContentTypeStore, useClass: MockDotContentTypeStore }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentTypesPortletComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        router = de.injector.get(ActivatedRoute);
        crudService = fixture.debugElement.injector.get(DotCrudService);
        dotContentletService = fixture.debugElement.injector.get(DotContentTypeService);
        pushPublishService = fixture.debugElement.injector.get(PushPublishService);
        dotLicenseService = fixture.debugElement.injector.get(DotLicenseService);
        dotHttpErrorManagerService = fixture.debugElement.injector.get(DotHttpErrorManagerService);

        dotPushPublishDialogService = fixture.debugElement.injector.get(
            DotPushPublishDialogService
        );

        jest.spyOn(dotContentletService, 'getAllContentTypes').mockReturnValue(
            of([
                { name: 'CONTENT', label: 'Content', types: [] },
                { name: 'WIDGET', label: 'Widget', types: [] },
                { name: 'FORM', label: 'Form', types: [] }
            ])
        );
    });

    it('should display a listing-data-table.component', fakeAsync(() => {
        fixture.detectChanges();
        tick(1);
        fixture.detectChanges();
        const listingDataTable = fixture.debugElement.query(By.css('dot-listing-data-table'));
        expect('v1/contenttype').toEqual(listingDataTable.nativeElement.getAttribute('url'));

        const columns = comp.contentTypeColumns;
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
        fixture.detectChanges();

        const mockContentType: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };

        const dotDialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
        jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });

        jest.spyOn(crudService, 'delete').mockReturnValue(of(mockContentType));
        comp.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command(mockContentType);

        fixture.detectChanges();

        expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', mockContentType.id);
        expect(crudService.delete).toHaveBeenCalledTimes(1);
    });

    it('should have remove, push publish, Copy and Add to bundle actions to the list item', () => {
        fixture.detectChanges();

        expect(comp.rowActions.map((action) => action.menuItem.label)).toEqual([
            'Push Publish',
            'Add to bundle',
            'Add to Menu',
            'Copy',
            'Delete'
        ]);
    });

    it('should have ONLY remove action because is community license', () => {
        jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));

        fixture.detectChanges();
        expect(
            comp.rowActions.map((action) => {
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
        jest.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(of([]));
        fixture.detectChanges();

        expect(comp.rowActions.map((action) => action.menuItem.label)).toEqual([
            'Add to bundle',
            'Add to Menu',
            'Copy',
            'Delete'
        ]);
    });

    it('should open push publish dialog', () => {
        fixture.detectChanges();
        jest.spyOn(dotPushPublishDialogService, 'open');
        const mockContentType: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };

        expect(de.query(By.css('p-dialog'))).toBeNull();

        comp.rowActions[0].menuItem.command(mockContentType);
        fixture.detectChanges();
        expect(de.query(By.css('p-dialog'))).toBeDefined();
        expect(dotPushPublishDialogService.open).toHaveBeenCalledWith({
            assetIdentifier: mockContentType.id,
            title: 'Push Publish'
        });
    });

    it('should open add to bundle dialog', fakeAsync(() => {
        fixture.detectChanges();
        tick(1);
        fixture.detectChanges();
        const mockContentType: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };
        expect(comp.addToBundleIdentifier).not.toBeDefined();

        comp.rowActions[ADD_TO_BUNDLE_MENU_ITEM_INDEX].menuItem.command(mockContentType);

        // Verify the component state was updated correctly
        expect(comp.addToBundleIdentifier).toEqual(mockContentType.id);
    }));

    it('should open Add to Menu dialog', fakeAsync(() => {
        fixture.detectChanges();
        tick(1);
        fixture.detectChanges();
        const mockContentType: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };
        expect(comp.addToMenuContentType).not.toBeDefined();

        comp.rowActions[ADD_TO_MENU_INDEX].menuItem.command(mockContentType);

        // Verify the component state was updated correctly
        expect(comp.addToMenuContentType).toEqual(mockContentType);
    }));

    it('should populate the actionHeaderOptions based on a call to dotContentletService', () => {
        fixture.detectChanges();
        expect(dotContentletService.getAllContentTypes).toHaveBeenCalled();
        expect(comp.actionHeaderOptions.primary.model.length).toEqual(3);
    });

    it('should not set primary command in the header options', () => {
        fixture.detectChanges();
        expect(comp.actionHeaderOptions.primary.command).toBe(undefined);
    });

    it('should emit changes in base types selector', fakeAsync(() => {
        fixture.detectChanges();
        tick(1);
        fixture.detectChanges();
        baseTypesSelector = de.query(By.css('dot-base-type-selector')).componentInstance;
        jest.spyOn(comp, 'changeBaseTypeSelector');
        baseTypesSelector.selected.emit('test');

        expect(comp.changeBaseTypeSelector).toHaveBeenCalledWith('test');
        expect(comp.changeBaseTypeSelector).toHaveBeenCalledTimes(1);
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

        fixture.detectChanges();

        const mockContentType: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            id: '1234567890',
            name: 'Nuevo',
            variable: 'Nuevo',
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            owner: '123',
            system: false
        };

        const dotDialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
        jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });

        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(crudService, 'delete').mockReturnValue(observableThrowError(forbiddenError));
        comp.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command(mockContentType);

        fixture.detectChanges();

        expect<any>(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(forbiddenError);
    });

    it('should show remove option', () => {
        fixture.detectChanges();

        const shouldShow = comp.rowActions[DELETE_MENU_ITEM_INDEX].shouldShow({
            fixed: false,
            defaultType: false
        });

        expect(shouldShow).toBeTruthy();
    });

    it('should not show remove option if content type is defaultType', () => {
        fixture.detectChanges();
        const shouldShow = comp.rowActions[DELETE_MENU_ITEM_INDEX].shouldShow({
            fixed: false,
            defaultType: true
        });
        expect(shouldShow).toBeFalsy();
    });

    it('should not show Add To Menu option if content type is HOST', () => {
        fixture.detectChanges();
        const shouldShow = comp.rowActions[ADD_TO_MENU_INDEX].shouldShow({
            variable: 'Host'
        });
        expect(shouldShow).toBeFalsy();
    });

    it('should show Add to Menu option', () => {
        fixture.detectChanges();
        expect(comp.rowActions[ADD_TO_MENU_INDEX].menuItem.label).toBe('Add to Menu');
    });

    describe('filterBy', () => {
        beforeEach(() => {
            router.data = of({
                filterBy: 'FORM'
            });
        });

        it('should not display base types selector', fakeAsync(() => {
            fixture.detectChanges();
            tick(1);
            fixture.detectChanges();
            const dotBaseTypeSelector = de.query(By.css('dot-base-type-selector'));
            expect(dotBaseTypeSelector).toBeNull();
        }));

        it('should set filterBy params', fakeAsync(() => {
            fixture.detectChanges();
            tick(1);
            fixture.detectChanges();
            expect(comp.filterBy).toBe('Form');
            expect(comp.$listing().paginatorService.extraParams.get('type')).toBe('Form');
            expect(comp.actionHeaderOptions.primary.model).toBe(null);
            expect(comp.actionHeaderOptions.primary.command).toBeDefined();
        }));
    });
});
