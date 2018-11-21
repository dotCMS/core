import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { EditRelationshipsComponent } from './edit-relationships.component';
import { Component, Input, Output, EventEmitter, Injectable, DebugElement } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotEditContentTypeCacheService } from '@portlets/content-types/services/edit-content-type-cache.service';
import { PaginatorService } from '@services/paginator';
import { RelationshipService } from '@portlets/content-types/fields/service/relationship.service';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/shared/dot-relationship-cardinality.model';
import { ContentType } from '@portlets/content-types/shared/content-type.model';

const mockRelationships = [
    {
        cardinality: 1,
        relationTypeValue: 'a'
    },
    {
        cardinality: 0,
        relationTypeValue: 'b'
    }
];

const cardinalities = [
    {
        label: 'Many to many',
        id: 0,
        name: 'MANY_TO_MANY'
    },
    {
        label: 'One to one',
        id: 1,
        name: 'ONE_TO_ONE'
    }
];

@Component({
    selector: 'dot-searchable-dropdown',
    template: ''
})
class MockSearchableDropdownComponent {
    @Input()
    data: string[];
    @Input()
    labelPropertyName: string | string[];
    @Input()
    pageLinkSize = 3;
    @Input()
    rows: number;
    @Input()
    totalRecords: number;
    @Input()
    placeholder = '';

    @Output()
    change: EventEmitter<any> = new EventEmitter();
    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();
    @Output()
    pageChange: EventEmitter<PaginationEvent> = new EventEmitter();
}

@Injectable()
class MockPaginatorService {
    url: string;

    public paginationPerPage: 10;
    public maxLinksPage: 5;
    public totalRecords: 40;

    setExtraParams(): void {

    }

    public getWithOffset(): Observable<any[]> {
        return null;
    }
}

@Injectable()
class MockRelationshipService {
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return of(cardinalities);
    }
}

describe('EditRelationshipsComponent', () => {
    const contentTypeMock: ContentType = {
        clazz: 'clazz',
        defaultType: false,
        fixed: false,
        folder: 'folder',
        host: 'host',
        name: 'content type name',
        owner: 'user',
        system: true,
        id: '1',
    };

    let comp: EditRelationshipsComponent;
    let fixture: ComponentFixture<EditRelationshipsComponent>;
    let de: DebugElement;

    let paginatorService: PaginatorService;
    let dotEditContentTypeCacheService: DotEditContentTypeCacheService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.existing.label': 'existing',
        'contenttypes.field.properties.relationship.existing.placeholder': 'Select Relationship',
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                EditRelationshipsComponent,
                MockSearchableDropdownComponent
            ],
            imports: [],
            providers: [
                DotEditContentTypeCacheService,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: RelationshipService, useClass: MockRelationshipService }
            ]
        });

        fixture = DOTTestBed.createComponent(EditRelationshipsComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        paginatorService = de.injector.get(PaginatorService);
        spyOn(paginatorService, 'setExtraParams').and.callThrough();
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(mockRelationships));

        dotEditContentTypeCacheService = de.injector.get(DotEditContentTypeCacheService);
    });

    it('should set url to get relationships', () => {
        fixture.detectChanges();
        expect(paginatorService.url).toBe('v1/relationships');
    });

    it('should has a dot-searchable-dropdown and it should has the right attributes values', () => {
        fixture.detectChanges();

        const  dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));

        expect(dotSearchableDropdown).not.toBeUndefined();
        expect(dotSearchableDropdown.componentInstance.pageLinkSize).toBe(paginatorService.maxLinksPage);
        expect(dotSearchableDropdown.componentInstance.rows).toBe(paginatorService.paginationPerPage);
        expect(dotSearchableDropdown.componentInstance.totalRecords).toBe(paginatorService.totalRecords);
        expect(dotSearchableDropdown.componentInstance.labelPropertyName).toBe('label');
        expect(dotSearchableDropdown.componentInstance.placeholder).toBe('Select Relationship');
    });

    it('should handle filter change into relationship pagination', () => {
        const newFilter = 'new filter';

        dotEditContentTypeCacheService.setContentType(contentTypeMock);

        fixture.detectChanges();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.componentInstance.filterChange.emit(newFilter);

        expect(paginatorService.filter).toBe(newFilter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith('contentTypeId', contentTypeMock.id);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);

        fixture.detectChanges();

        expect(dotSearchableDropdown.componentInstance.data).toEqual([
            {
                label: 'a   .   One to one',
                relationship: mockRelationships[0]
            },
            {
                label: 'b   .   Many to many',
                relationship: mockRelationships[1]
            }
        ]);
    });

    it('should handle page change into relationship pagination', () => {
        const event = {
            filter: 'new filter',
            first: 2
        };

        dotEditContentTypeCacheService.setContentType(contentTypeMock);

        fixture.detectChanges();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.componentInstance.pageChange.emit(event);

        expect(paginatorService.filter).toBe(event.filter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith('contentTypeId', contentTypeMock.id);
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(event.first);

        fixture.detectChanges();

        expect(dotSearchableDropdown.componentInstance.data).toEqual([
            {
                label: 'a   .   One to one',
                relationship: mockRelationships[0]
            },
            {
                label: 'b   .   Many to many',
                relationship: mockRelationships[1]
            }
        ]);
    });

    it('should tigger change event', (done) => {
        fixture.detectChanges();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.componentInstance.change.emit({
            relationship: mockRelationships[0]
        });

        comp.change.subscribe((relationshipSelect: any) => {
            expect(relationshipSelect).toBe(mockRelationships[0]);
            done();
        });
    });
});
