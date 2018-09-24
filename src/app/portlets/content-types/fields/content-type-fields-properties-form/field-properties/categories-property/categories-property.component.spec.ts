import { CategoriesPropertyComponent } from './categories-property.component';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Injectable, Input, Output, EventEmitter, Component } from '@angular/core';
import { MockDotMessageService } from '../../../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { PaginatorService } from '@services/paginator';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginationEvent } from '@components/_common/searchable-dropdown/component';

import { NgControl, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'dot-searchable-dropdown',
    template: ''
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
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input()
    field: NgControl;
    @Input()
    message: string;
}

@Injectable()
class TestPaginatorService {}

describe('CategoriesPropertyComponent', () => {
    let comp: CategoriesPropertyComponent;
    let fixture: ComponentFixture<CategoriesPropertyComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.category.label': 'Select category',
        search: 'search'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [CategoriesPropertyComponent, TestFieldValidationMessageComponent, TestSearchableDropdownComponent],
            imports: [],
            providers: [
                { provide: PaginatorService, useClass: TestPaginatorService },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(CategoriesPropertyComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should have a form', () => {
        const group = new FormGroup({});
        comp.group = group;
        const divForm: DebugElement = fixture.debugElement.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(group).toEqual(divForm.componentInstance.group);
    });

    it('should set PaginatorService url & placeholder empty label', () => {
        comp.property = {
            field: {},
            name: 'categories',
            value: ''
        };
        comp.ngOnInit();
        const mockFieldPaginatorService = fixture.debugElement.injector.get(PaginatorService);
        expect('v1/categories').toBe(mockFieldPaginatorService.url);
        expect(comp.placeholder).toBe('Select category');
    });

    it('should set stored value in placeholder label', () => {
        comp.property = {
            field: {},
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
        expect(comp.placeholder).toBe(comp.property.value);
    });

    describe('Pagination events', () => {
        beforeEach(async(() => {
            const divForm: DebugElement = fixture.debugElement.query(By.css('div'));
            this.searchableDropdown = divForm.query(By.css('dot-searchable-dropdown'));
        }));

        it('should change Page', () => {
            const mockFieldPaginatorService = fixture.debugElement.injector.get(PaginatorService);
            const spyMethod = spyOn(mockFieldPaginatorService, 'getWithOffset');

            this.searchableDropdown.componentInstance.pageChange.next({
                filter: 'filter',
                first: 2
            });

            expect('filter').toBe(mockFieldPaginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(2);
        });

        it('should filter', () => {
            const mockFieldPaginatorService = fixture.debugElement.injector.get(PaginatorService);
            const spyMethod = spyOn(mockFieldPaginatorService, 'getWithOffset');

            this.searchableDropdown.componentInstance.filterChange.next('filter');

            expect('filter').toBe(mockFieldPaginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(0);
        });
    });
});
