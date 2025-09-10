import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { NgControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { CategoriesPropertyComponent } from './categories-property.component';

import { DOTTestBed } from '../../../../../../../../test/dot-test-bed';
import { PaginationEvent } from '../../../../../../../../view/components/_common/searchable-dropdown/component/searchable-dropdown.component';

@Component({
    selector: 'dot-searchable-dropdown',
    template: '',
    standalone: false
})
class TestSearchableDropdownComponent {
    @Input()
    data: string[];
    @Input()
    labelPropertyName;
    @Input()
    valuePropertyName;
    @Input()
    pageLinkSize = 3;
    @Input()
    rows: number;
    @Input()
    totalRecords: number;
    @Input()
    placeholder = '';

    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();
    @Output()
    pageChange: EventEmitter<PaginationEvent> = new EventEmitter();
}

@Component({
    selector: 'dot-field-validation-message',
    template: '',
    standalone: false
})
class TestFieldValidationMessageComponent {
    @Input()
    field: NgControl;
    @Input()
    message: string;
}

@Injectable()
class TestPaginatorService {
    getWithOffset() {
        return of([]);
    }
}

describe('CategoriesPropertyComponent', () => {
    let comp: CategoriesPropertyComponent;
    let fixture: ComponentFixture<CategoriesPropertyComponent>;
    let de: DebugElement;
    let searchableDropdown: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.category.label': 'Select category',
        search: 'search'
    });
    let paginatorService: PaginatorService;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                CategoriesPropertyComponent,
                TestFieldValidationMessageComponent,
                TestSearchableDropdownComponent
            ],
            imports: [DotMessagePipe],
            providers: [
                { provide: PaginatorService, useClass: TestPaginatorService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(CategoriesPropertyComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        paginatorService = de.injector.get(PaginatorService);
    }));

    it('should have a form', () => {
        const group = new UntypedFormGroup({});
        comp.group = group;
        const divForm: DebugElement = de.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(divForm.componentInstance.group).toEqual(group);
    });

    it('should set PaginatorService url & placeholder empty label', () => {
        comp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock
            },
            name: 'categories',
            value: ''
        };
        comp.ngOnInit();
        expect(paginatorService.url).toBe('v1/categories');
        expect(comp.placeholder).toBe('Select category');
    });

    it('should set stored value in placeholder label', () => {
        comp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock
            },
            name: 'categories',
            value: {
                categoryName: 'A-Z Index',
                description: '',
                key: 'azindex',
                sortOrder: 0,
                inode: '3297fcca-d88a-45a7-aef4-7960bc6964aa'
            }
        };
        comp.ngOnInit();
        expect(comp.placeholder).toBe(comp.property.value as string);
    });

    describe('Pagination events', () => {
        let spyMethod: jest.SpyInstance;

        beforeEach(() => {
            const divForm: DebugElement = de.query(By.css('div'));
            searchableDropdown = divForm.query(By.css('dot-searchable-dropdown'));
            spyMethod = jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([]));
        });

        it('should change Page', () => {
            searchableDropdown.triggerEventHandler('pageChange', {
                filter: 'filter',
                first: 2
            });

            expect('filter').toBe(paginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(2);
        });

        it('should filter', () => {
            searchableDropdown.triggerEventHandler('filterChange', 'filter');

            expect('filter').toBe(paginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(0);
        });

        it('should valuePropertyName be undefined', () => {
            expect(searchableDropdown.componentInstance.valuePropertyName).toBeUndefined();
        });
    });
});
