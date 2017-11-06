import { Observable } from 'rxjs';
import { CrudService } from './../../../api/services/crud/crud.service';
import { ContentType } from './../shared/content-type.model';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ListingDataTableModule } from './../../../view/components/listing-data-table/listing-data-table.module';
import { DotConfirmationService } from './../../../api/services/dot-confirmation/dot-confirmation.service';
import { MenuItem } from 'primeng/primeng';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentTypesPortletComponent } from './content-types.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { FormatDateService } from '../../../api/services/format-date-service';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { Injectable } from '@angular/core';
import { DotContentletService } from '../../../api/services/dot-contentlet.service';

@Injectable()
class MockDotContentletService {
    getAllContentTypes() {}
}

describe('ContentTypesPortletComponent', () => {
    let comp: ContentTypesPortletComponent;
    let fixture: ComponentFixture<ContentTypesPortletComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let crudService: CrudService;
    let dotContentletService: DotContentletService;

    beforeEach(() => {
        let messageServiceMock = new MockMessageService({
            'contenttypes.form.label.description': 'Description',
            'contenttypes.fieldname.entries': 'Entries',
            'contenttypes.fieldname.structure.name': 'Content Type Name',
            'contenttypes.content.variable': 'Variable Name',
            mod_date: 'Last Edit Date'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ContentTypesPortletComponent],
            imports: [
                RouterTestingModule.withRoutes([{ path: 'test', component: ContentTypesPortletComponent }]),
                BrowserAnimationsModule,
                ListingDataTableModule
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
                CrudService,
                FormatDateService,
                ContentTypesInfoService,
                DotConfirmationService,
                { provide: DotContentletService, useClass: MockDotContentletService }
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesPortletComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
        crudService = fixture.debugElement.injector.get(CrudService);
        dotContentletService = fixture.debugElement.injector.get(DotContentletService);
        spyOn(dotContentletService, 'getAllContentTypes').and.returnValue(
            Observable.of([
                { name: 'CONTENT', label: 'Content', types: [] },
                { name: 'WIDGET', label: 'Widget', types: [] },
                { name: 'FORM', label: 'Form', types: [] }
            ])
        );
    });

    it('should display a listing-data-table.component', () => {
        let de = fixture.debugElement.query(By.css('listing-data-table'));
        comp.ngOnInit();

        expect('v1/contenttype').toEqual(de.nativeElement.getAttribute('url'));

        const columns = comp.contentTypeColumns;
        expect(5).toEqual(columns.length);

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
        comp.ngOnInit();
        const fakeActions: MenuItem[] = [
            {
                icon: 'fa-trash',
                label: 'Remove',
                command: () => {}
            }
        ];

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

        const dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });

        spyOn(crudService, 'delete').and.returnValue(Observable.of(mockContentType));

        comp.rowActions[0].command(mockContentType);

        fixture.detectChanges();

        expect(crudService.delete).toHaveBeenCalledWith('v1/contenttype/id', mockContentType.id);
    });

    it('should populate the actionHeaderOptions based on a call to dotContentletService', () => {
        comp.ngOnInit();
        expect(dotContentletService.getAllContentTypes).toHaveBeenCalled();
        expect(comp.actionHeaderOptions.primary.model.length).toEqual(3);
    });
});
