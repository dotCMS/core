/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of as observableOf, throwError as observableThrowError } from 'rxjs';
import { DotCrudService } from '@services/dot-crud/dot-crud.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotListingDataTableModule } from '@components/dot-listing-data-table/dot-listing-data-table.module';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotContentTypesPortletComponent } from './dot-content-types.component';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { ConfirmationService, SelectItem } from 'primeng/api';
import {
    CoreWebService,
    DotPushPublishDialogService,
    HttpCode,
    LoggerService,
    ResponseView,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    DotHttpErrorHandled,
    DotHttpErrorManagerService
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotCMSContentType, DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { DotContentTypeStore } from '@portlets/shared/dot-content-types-listing/dot-content-type.store';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';

const DELETE_MENU_ITEM_INDEX = 4;
const ADD_TO_MENU_INDEX = 2;
const ADD_TO_BUNDLE_MENU_ITEM_INDEX = 1;

@Injectable()
class MockDotContentTypeService {
    getAllContentTypes() {}
}

@Component({
    selector: 'dot-dot-content-type-copy-dialog',
    template: ''
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
    template: ''
})
class MockDotBaseTypeSelectorComponent {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();
}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return observableOf(true);
    }
}

@Injectable()
export class MockPushPublishService {
    getEnvironments() {
        return observableOf([
            {
                id: '123',
                name: 'Environment 1'
            },
            {
                id: '456',
                name: 'Environment 2'
            }
        ]);
    }
}

@Injectable()
class MockDotHttpErrorManagerService {
    handle(_err: ResponseView): Observable<DotHttpErrorHandled> {
        return observableOf({
            redirected: false,
            status: HttpCode.BAD_REQUEST
        });
    }
}

@Injectable()
class MockDotContentTypeStore {}

@Component({
    selector: 'dot-add-to-bundle ',
    template: ``
})
class MockDotAddToBundleComponent {
    @Input() assetIdentifier: string;
    @Output() cancel = new EventEmitter<boolean>();
}

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
                DotContentTypesPortletComponent,
                MockDotBaseTypeSelectorComponent,
                MockDotAddToBundleComponent,
                MockDotContentTypeCloneDialogComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    { path: 'test', component: DotContentTypesPortletComponent }
                ]),
                BrowserAnimationsModule,
                DotListingDataTableModule,
                ReactiveFormsModule,
                HttpClientTestingModule
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
                { provide: DotHttpErrorManagerService, useClass: MockDotHttpErrorManagerService },
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

        spyOn(dotContentletService, 'getAllContentTypes').and.returnValue(
            observableOf([
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
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });

        spyOn(crudService, 'delete').and.returnValue(observableOf(mockContentType));
        comp.rowActions[DELETE_MENU_ITEM_INDEX].menuItem.command(mockContentType);

        fixture.detectChanges();

        expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', mockContentType.id);
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
        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(observableOf(false));

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
                icon: 'delete'
            }
        ]);
    });

    it('should have remove and add to bundle actions if is not community license and no publish environments are created', () => {
        spyOn(pushPublishService, 'getEnvironments').and.returnValue(observableOf([]));
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
        spyOn(dotPushPublishDialogService, 'open').and.callThrough();
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

    it('should open add to bundle dialog', () => {
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
        expect(de.query(By.css('p-dialog'))).toBeNull();

        comp.rowActions[ADD_TO_BUNDLE_MENU_ITEM_INDEX].menuItem.command(mockContentType);
        fixture.detectChanges();

        expect(de.query(By.css('p-dialog'))).toBeDefined();
        expect(comp.addToBundleIdentifier).toEqual(mockContentType.id);
    });

    it('should open Add to Menu dialog', () => {
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
        expect(de.query(By.css('p-dialog'))).toBeNull();
        comp.rowActions[ADD_TO_MENU_INDEX].menuItem.command(mockContentType);
        fixture.detectChanges();

        expect(de.query(By.css('p-dialog'))).toBeDefined();
        expect(comp.addToMenuContentType).toEqual(mockContentType);
    });

    it('should populate the actionHeaderOptions based on a call to dotContentletService', () => {
        fixture.detectChanges();
        expect(dotContentletService.getAllContentTypes).toHaveBeenCalled();
        expect(comp.actionHeaderOptions.primary.model.length).toEqual(3);
    });

    it('should not set primary command in the header options', () => {
        fixture.detectChanges();
        expect(comp.actionHeaderOptions.primary.command).toBe(undefined);
    });

    it('should emit changes in base types selector', () => {
        fixture.detectChanges();
        baseTypesSelector = de.query(By.css('dot-base-type-selector')).componentInstance;
        spyOn(comp, 'changeBaseTypeSelector');
        baseTypesSelector.selected.emit('test');

        expect(comp.changeBaseTypeSelector).toHaveBeenCalledWith('test');
    });

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
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });

        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(crudService, 'delete').and.returnValue(observableThrowError(forbiddenError));
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
            router.data = observableOf({
                filterBy: 'FORM'
            });
            fixture.detectChanges();
        });

        it('should not display base types selector', () => {
            const dotBaseTypeSelector = de.query(By.css('dot-base-type-selector'));
            expect(dotBaseTypeSelector).toBeNull();
        });

        it('should set filterBy params', () => {
            fixture.detectChanges();
            expect(comp.filterBy).toBe('Form');
            expect(comp.listing.paginatorService.extraParams.get('type')).toBe('Form');
            expect(comp.actionHeaderOptions.primary.model).toBe(null);
            expect(comp.actionHeaderOptions.primary.command).toBeDefined();
        });
    });
});
