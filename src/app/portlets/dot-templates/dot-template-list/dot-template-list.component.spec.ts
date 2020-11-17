import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotTemplateListComponent } from './dot-template-list.component';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { LoggerService, StringUtils } from 'dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';

const templatesMock = [
    {
        anonymous: false,
        friendlyName: 'test',
        identifier: '123',
        inode: '1ASD',
        name: 'Name',
        type: 'type',
        versionType: 'type'
    },
    {
        anonymous: true,
        friendlyName: 'test',
        identifier: '123',
        inode: '1ASD',
        name: 'Name',
        type: 'type',
        versionType: 'type'
    }
];

const routeDataMock = {
    dotTemplateListResolverData: templatesMock
};
class ActivatedRouteMock {
    get data() {
        return of(routeDataMock);
    }
}

const messages = {
    'templates.fieldName.name': 'Name',
    'templates.fieldName.status': 'Status',
    'templates.fieldName.description': 'Description',
    'templates.fieldName.lastEdit': 'Last Edit'
};

const columnsMock = [
    {
        fieldName: 'name',
        header: 'Name',
        sortable: true
    },
    {
        fieldName: 'status',
        header: 'Status'
    },
    {
        fieldName: 'friendlyName',
        header: 'Description'
    },
    {
        fieldName: 'modDate',
        format: 'date',
        header: 'Last Edit',
        sortable: true
    }
];

describe('DotTemplateListComponent', () => {
    let fixture: ComponentFixture<DotTemplateListComponent>;
    let dotListingDataTable: DebugElement;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateListComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy(),
                        goToEditTemplate: jasmine.createSpy()
                    }
                },
                LoggerService,
                StringUtils
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateListComponent);
        fixture.detectChanges();
        dotListingDataTable = fixture.debugElement.query(By.css('dot-listing-data-table'));
    });

    it('should set attributes of dotListingDataTable', () => {
        expect(dotListingDataTable.attributes.sortField).toEqual('identifier');
        expect(dotListingDataTable.attributes.sortOrder).toEqual('DESC');
        expect(dotListingDataTable.attributes.url).toEqual('v1/templates');
        expect(dotListingDataTable.attributes.dataKey).toEqual('inode');
    });

    it('should set properties of dotListingDataTable', () => {
        dotListingDataTable = fixture.debugElement.query(By.css('dot-listing-data-table'));
        expect(dotListingDataTable.properties.columns).toEqual(columnsMock);
        expect(dotListingDataTable.properties.firstPageData).toEqual(templatesMock);
        expect(dotListingDataTable.properties.checkbox).toEqual(true);
    });

    xit('should render passed template', () => {
        // TODO: Find a way to test the ng-template.
    });
});
