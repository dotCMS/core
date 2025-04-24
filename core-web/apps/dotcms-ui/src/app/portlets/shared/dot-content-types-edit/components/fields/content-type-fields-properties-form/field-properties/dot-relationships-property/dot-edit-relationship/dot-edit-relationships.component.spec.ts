/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { PaginationEvent } from '@components/_common/searchable-dropdown/component';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeBasicMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotRelationshipCardinality } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { DotEditContentTypeCacheService } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-edit-content-type-cache.service';
import { DotRelationshipService } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';

import { DotEditRelationshipsComponent } from './dot-edit-relationships.component';

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
        clazz: 'clazz',
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

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditRelationshipsComponent, MockSearchableDropdownComponent],
            imports: [DotMessagePipe],
            providers: [
                DotEditContentTypeCacheService,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotRelationshipService, useClass: MockRelationshipService }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditRelationshipsComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        paginatorService = de.injector.get(PaginatorService);
        spyOn(paginatorService, 'setExtraParams').and.callThrough();
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(mockRelationships));

        dotEditContentTypeCacheService = de.injector.get(DotEditContentTypeCacheService);
    }));

    it('should set url to get relationships', () => {
        fixture.detectChanges();
        expect(paginatorService.url).toBe('v1/relationships');
    });

    it('should has a dot-searchable-dropdown and it should has the right attributes values', () => {
        fixture.detectChanges();

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

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.triggerEventHandler('filterChange', newFilter);

        expect(paginatorService.filter).toBe(newFilter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
            'contentTypeId',
            contentTypeMock.id
        );

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);

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

        const dotSearchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        dotSearchableDropdown.triggerEventHandler('pageChange', event);

        expect(paginatorService.filter).toBe(event.filter);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith(
            'contentTypeId',
            contentTypeMock.id
        );

        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(event.first);
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
