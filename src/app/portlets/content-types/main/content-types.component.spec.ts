import { ActionButtonComponent } from '../../../view/components/_common/action-button/action-button.component';
import { ActionHeaderComponent } from '../../../view/components/listing-data-table/action-header/action-header';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentTypesPortletComponent } from './content-types.component';
import { CrudService } from '../../../api/services/crud';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { FormatDateService } from '../../../api/services/format-date-service';
import { ListingDataTableComponent } from '../../../view/components/listing-data-table/listing-data-table.component';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';

describe('ContentTypesPortletComponent', () => {
    let comp: ContentTypesPortletComponent;
    let fixture: ComponentFixture<ContentTypesPortletComponent>;

    beforeEach(() => {
        let messageServiceMock = new MockMessageService({
            'Description': 'Description',
            'Entries': 'Entries',
            'Structure-Name': 'Content Type Name',
            'Variable': 'Variable Name',
            'mod_date': 'Last Edit Date'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ActionHeaderComponent,
                ActionButtonComponent,
                ContentTypesPortletComponent,
                ListingDataTableComponent
            ],
            imports: [RouterTestingModule.withRoutes([
                { path: 'test', component: ContentTypesPortletComponent }
            ])],
            providers: [
                 {provide: MessageService, useValue: messageServiceMock},
                 CrudService,
                 FormatDateService,
                 ContentTypesInfoService
            ],
        });

        fixture = DOTTestBed.createComponent(ContentTypesPortletComponent);
        comp = fixture.componentInstance;
    });

    it('should display a listing-data-table.component', () => {
        let de = fixture.debugElement.query(By.css('listing-data-table'));

        expect('v1/contenttype').toEqual(de.nativeElement.getAttribute('url'));

        let columns = comp.contentTypeColumns;
        expect(5).toEqual(columns.length);

        expect('name').toEqual(columns[0].fieldName);
        expect('Content Type Name').toEqual(columns[0].header);
        expect('35%').toEqual(columns[0].width);

        expect('variable').toEqual(columns[1].fieldName);
        expect('Variable Name').toEqual(columns[1].header);
        expect('10%').toEqual(columns[1].width);

        expect('description').toEqual(columns[2].fieldName);
        expect('Description').toEqual(columns[2].header);
        expect('35%').toEqual(columns[2].width);

        expect('nEntries').toEqual(columns[3].fieldName);
        expect('Entries').toEqual(columns[3].header);
        expect('10%').toEqual(columns[3].width);

        expect('modDate').toEqual(columns[4].fieldName);
        expect('Last Edit Date').toEqual(columns[4].header);
        expect('10%').toEqual(columns[4].width);
    });
});