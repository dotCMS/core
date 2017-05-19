import { DOTTestBed } from '../../api/util/test/dot-test-bed';
import { ContentTypesPortletComponent } from './content-types-component';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ListingDataTableComponent } from '../../view/components/listing-data-table/listing-data-table-component';
import { DataTableModule, SharedModule } from 'primeng/primeng';
import { MessageService } from '../../api/services/messages-service';
import { MockMessageService } from '../../api/util/test/mock-message-service';
import { ListingService } from '../../api/services/listing-service';

describe('ContentTypesPortletComponent', () => {

    let comp:    ContentTypesPortletComponent;
    let fixture: ComponentFixture<ContentTypesPortletComponent>;

    beforeEach(() => {
        let messageServiceMock = new MockMessageService({
            'Description': 'Description',
            'Entries': 'Entries',
            'Structure-Name': 'Content Type Name',
            'Variable': 'Variable Name'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesPortletComponent,
                ListingDataTableComponent
            ],
            imports: [ DataTableModule, SharedModule ],
            providers: [
                 {provide: MessageService, useValue: messageServiceMock},
                 ListingService
            ],
        });

        fixture = DOTTestBed.createComponent(ContentTypesPortletComponent);
        comp = fixture.componentInstance;
    });

    it('should display a listing-data-table-component', () => {
        let de = fixture.debugElement.query(By.css('listing-data-table-component'));

        expect('v1/contenttype').toEqual(de.nativeElement.getAttribute('url'));

        let columns = comp.contentTypeColumns;
        expect(4).toEqual(columns.length);

        expect('name').toEqual(columns[0].fieldName);
        expect('Content Type Name').toEqual(columns[0].header);
        expect('40%').toEqual(columns[0].width);

        expect('velocityVarName').toEqual(columns[1].fieldName);
        expect('Variable Name').toEqual(columns[1].header);
        expect('10%').toEqual(columns[1].width);

        expect('description').toEqual(columns[2].fieldName);
        expect('Description').toEqual(columns[2].header);
        expect('40%').toEqual(columns[2].width);

        expect('nEntries').toEqual(columns[3].fieldName);
        expect('Entries').toEqual(columns[3].header);
        expect('10%').toEqual(columns[3].width);
    });
});