/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditRelationshipsComponent } from './dot-edit-relationships.component';

import { DOTTestBed } from '../../../../../../../../../test/dot-test-bed';
import {
    PaginationEvent,
    SearchableDropdownComponent
} from '../../../../../../../../../view/components/_common/searchable-dropdown/component/searchable-dropdown.component';
import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';
import { DotEditContentTypeCacheService } from '../services/dot-edit-content-type-cache.service';
import { DotRelationshipService } from '../services/dot-relationship.service';

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
    switch: EventEmitter<any> = new EventEmitter();
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

    setExtraParams(): void {}

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

describe('DotEditRelationshipsComponent', () => {
    const contentTypeMock: DotCMSContentType = {
        ...dotcmsContentTypeBasicMock,
        clazz: DotCMSClazzes.TEXT,
        defaultType: false,
        fixed: false,
        folder: 'folder',
        host: 'host',
        name: 'content type name',
        owner: 'user',
        system: true,
        id: '1'
    };

    let comp: DotEditRelationshipsComponent;
    let fixture: ComponentFixture<DotEditRelationshipsComponent>;
    let de: DebugElement;

    let paginatorService: PaginatorService;
    let dotEditContentTypeCacheService: DotEditContentTypeCacheService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.existing.label': 'existing',
        'contenttypes.field.properties.relationship.existing.placeholder': 'Select Relationship'
    });

    let cachedContentType: DotCMSContentType = { id: 'test-content-type-id' } as DotCMSContentType;

    const dotEditContentTypeCacheServiceMock = {
        get: jest.fn().mockImplementation(() => cachedContentType),
        set: jest.fn().mockImplementation((contentType: DotCMSContentType) => {
            cachedContentType = contentType;
        })
    };

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotMessagePipe, DotEditRelationshipsComponent],
            providers: [
                {
                    provide: DotEditContentTypeCacheService,
                    useValue: dotEditContentTypeCacheServiceMock
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotRelationshipService, useClass: MockRelationshipService }
            ]
        }).overrideComponent(DotEditRelationshipsComponent, {
            remove: { imports: [SearchableDropdownComponent] },
            add: { imports: [MockSearchableDropdownComponent] }
        });

        fixture = DOTTestBed.createComponent(DotEditRelationshipsComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        paginatorService = de.injector.get(PaginatorService);
        jest.spyOn(paginatorService, 'setExtraParams');
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of(mockRelationships));

        dotEditContentTypeCacheService = de.injector.get(DotEditContentTypeCacheService);
    }));

    it('should set url to get relationships', () => {
        fixture.detectChanges();
        jest.clearAllMocks();
        expect(paginatorService.url).toBe('v1/relationships');
    });

    it('should has a dot-searchable-dropdown and it should has the right attributes values', () => {
        fixture.detectChanges();
        jest.clearAllMocks();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));

        expect(dotSearchableDropdown).not.toBeUndefined();
        expect(dotSearchableDropdown.componentInstance.pageLinkSize).toBe(
            paginatorService.maxLinksPage
        );
        expect(dotSearchableDropdown.componentInstance.rows).toBe(
            paginatorService.paginationPerPage
        );
        expect(dotSearchableDropdown.componentInstance.totalRecords).toBe(
            paginatorService.totalRecords
        );
        expect(dotSearchableDropdown.componentInstance.labelPropertyName).toBe('label');
        expect(dotSearchableDropdown.componentInstance.placeholder).toBe('Select Relationship');
    });

    it('should handle filter change into relationship pagination', () => {
        const newFilter = 'new filter';

        dotEditContentTypeCacheService.set(contentTypeMock);

        fixture.detectChanges();
        jest.clearAllMocks();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.triggerEventHandler('filterChange', newFilter);

        expect(paginatorService.filter).toBe(newFilter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
            'contentTypeId',
            contentTypeMock.id
        );

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);

        expect(dotSearchableDropdown.componentInstance.data).toEqual([
            {
                label: 'a.One to one',
                relationship: mockRelationships[0]
            },
            {
                label: 'b.Many to many',
                relationship: mockRelationships[1]
            }
        ]);
    });

    it('should handle page change into relationship pagination', () => {
        const event = {
            filter: 'new filter',
            first: 2
        };

        dotEditContentTypeCacheService.set(contentTypeMock);

        fixture.detectChanges();
        jest.clearAllMocks();

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.triggerEventHandler('pageChange', event);

        expect(paginatorService.filter).toBe(event.filter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
            'contentTypeId',
            contentTypeMock.id
        );

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(event.first);
        expect(paginatorService.getWithOffset).toHaveBeenCalledTimes(1);
        expect(dotSearchableDropdown.componentInstance.data).toEqual([
            {
                label: 'a.One to one',
                relationship: mockRelationships[0]
            },
            {
                label: 'b.Many to many',
                relationship: mockRelationships[1]
            }
        ]);
    });

    it('should tigger change event', (done) => {
        fixture.detectChanges();
        jest.clearAllMocks();

        comp.switch.subscribe((relationshipSelect: any) => {
            expect(relationshipSelect).toEqual({
                cardinality: 1,
                velocityVar: 'a'
            });
            done();
        });

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.triggerEventHandler('switch', {
            relationship: mockRelationships[0]
        });
    });
});
