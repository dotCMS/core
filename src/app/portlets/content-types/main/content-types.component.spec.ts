import { throwError as observableThrowError, of as observableOf, Observable } from 'rxjs';
import { CrudService } from '@services/crud/crud.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ListingDataTableModule } from '@components/listing-data-table/listing-data-table.module';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesInfoService } from '@services/content-types-info';
import { ContentTypesPortletComponent } from './content-types.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { FormatDateService } from '@services/format-date-service';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { Injectable } from '@angular/core';
import { DotContentletService } from '@services/dot-contentlet/dot-contentlet.service';
import { PushPublishContentTypesDialogModule } from '@components/_common/push-publish-dialog/push-publish-dialog.module';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { SelectItem } from 'primeng/primeng';
import { ResponseView } from 'dotcms-js/dotcms-js';
import {
    DotHttpErrorHandled,
    DotHttpErrorManagerService
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ContentType } from '@portlets/content-types/shared/content-type.model';

@Injectable()
class MockDotContentletService {
    getAllContentTypes() {}
}

@Component({
    selector: 'dot-base-type-selector',
    template: ''
})
class MockDotBaseTypeSelectorComponent {
    @Input()
    value: SelectItem;
    @Output()
    selected = new EventEmitter<string>();
}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return observableOf(true);
    }
}

@Injectable()
class MockPushPublishService {
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
            redirected: false
        });
    }
}

@Component({
    selector: 'dot-add-to-bundle ',
    template: ``
})
class MockDotAddToBundleComponent {
    @Input()
    assetIdentifier: string;
    @Output()
    cancel = new EventEmitter<boolean>();
}

describe('ContentTypesPortletComponent', () => {
    let comp: ContentTypesPortletComponent;
    let fixture: ComponentFixture<ContentTypesPortletComponent>;
    let de: DebugElement;
    let crudService: CrudService;
    let dotContentletService: DotContentletService;
    let pushPublishService: PushPublishService;
    let dotLicenseService: DotLicenseService;
    let baseTypesSelector: MockDotBaseTypeSelectorComponent;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.form.label.description': 'Description',
            'contenttypes.fieldname.entries': 'Entries',
            'contenttypes.fieldname.structure.name': 'Content Type Name',
            'contenttypes.content.variable': 'Variable Name',
            mod_date: 'Last Edit Date',
            'contenttypes.action.delete': 'Delete',
            'contenttypes.content.push_publish': 'Push Publish',
            'contenttypes.content.add_to_bundle': 'Add to bundle'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesPortletComponent,
                MockDotBaseTypeSelectorComponent,
                MockDotAddToBundleComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    { path: 'test', component: ContentTypesPortletComponent }
                ]),
                BrowserAnimationsModule,
                ListingDataTableModule,
                PushPublishContentTypesDialogModule
            ],
            providers: [
                ContentTypesInfoService,
                CrudService,
                DotAlertConfirmService,
                FormatDateService,
                { provide: DotContentletService, useClass: MockDotContentletService },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PushPublishService, useClass: MockPushPublishService },
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                { provide: DotHttpErrorManagerService, useClass: MockDotHttpErrorManagerService }
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesPortletComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        crudService = fixture.debugElement.injector.get(CrudService);
        dotContentletService = fixture.debugElement.injector.get(DotContentletService);
        pushPublishService = fixture.debugElement.injector.get(PushPublishService);
        dotLicenseService = fixture.debugElement.injector.get(DotLicenseService);
        dotHttpErrorManagerService = fixture.debugElement.injector.get(DotHttpErrorManagerService);

        spyOn(dotContentletService, 'getAllContentTypes').and.returnValue(
            observableOf([
                { name: 'CONTENT', label: 'Content', types: [] },
                { name: 'WIDGET', label: 'Widget', types: [] },
                { name: 'FORM', label: 'Form', types: [] }
            ])
        );
    });

    it('should display a listing-data-table.component', () => {
        const listingDataTable = fixture.debugElement.query(By.css('dot-listing-data-table'));
        fixture.detectChanges();

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
        expect('7%').toEqual(columns[3].width);

        expect('modDate').toEqual(columns[4].fieldName);
        expect('Last Edit Date').toEqual(columns[4].header);
        expect('13%').toEqual(columns[4].width);
    });

    it('should remove the content type on click command function', () => {
        fixture.detectChanges();

        const mockContentType: ContentType = {
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
        comp.rowActions[2].menuItem.command(mockContentType);

        fixture.detectChanges();

        expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', mockContentType.id);
    });

    it('should have remove, push publish and Add to bundle actions to the list item', () => {
        fixture.detectChanges();
        expect(comp.rowActions.map((action) => action.menuItem.label)).toEqual([
            'Push Publish',
            'Add to bundle',
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
            'Delete'
        ]);
    });

    it('should open push publish dialog', () => {
        fixture.detectChanges();
        const mockContentType: ContentType = {
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
        expect(comp.pushPublishIdentifier).not.toBeDefined();
        expect(de.query(By.css('p-dialog'))).toBeNull();

        comp.rowActions[0].menuItem.command(mockContentType);
        fixture.detectChanges();

        expect(de.query(By.css('p-dialog'))).toBeDefined();
        expect(comp.pushPublishIdentifier).toEqual(mockContentType.id);
    });

    it('should open add to bundle dialog', () => {
        fixture.detectChanges();
        const mockContentType: ContentType = {
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

        comp.rowActions[1].menuItem.command(mockContentType);
        fixture.detectChanges();

        expect(de.query(By.css('p-dialog'))).toBeDefined();
        expect(comp.addToBundleIdentifier).toEqual(mockContentType.id);
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

        const mockContentType: ContentType = {
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

        comp.rowActions[2].menuItem.command(mockContentType);

        fixture.detectChanges();

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(forbiddenError);
    });

    it('should show remove option', () => {
        fixture.detectChanges();
        const shouldShow = comp.rowActions[2].shouldShow({
            fixed: false,
            defaultType: false
        });

        expect(shouldShow).toBeTruthy();
    });
});
