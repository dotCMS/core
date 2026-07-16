import { of } from 'rxjs';

import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { NgControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { CategoriesPropertyComponent } from './categories-property.component';

import { DOTTestBed } from '../../../../../../../../test/dot-test-bed';
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
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.category.label': 'Select category',
        search: 'search'
    });
    let paginatorService: PaginatorService;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [CategoriesPropertyComponent, TestFieldValidationMessageComponent],
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
        comp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock
            },
            name: 'categories',
            value: ''
        };
        comp.group = new UntypedFormGroup({
            categories: new UntypedFormControl('')
        });
        fixture.detectChanges();
    }));

    it('should have a form', () => {
        const divForm: DebugElement = de.query(By.css('div'));

        expect(divForm).not.toBeNull();
        expect(comp.group).toBeDefined();
    });

    it('should set PaginatorService url & placeholder empty label', () => {
        expect(paginatorService.url).toBe('v1/categories');
        expect(comp.placeholder).toBe('Select category');
    });

    it('should set stored value in placeholder label', () => {
        comp.property = {
            field: {
                ...dotcmsContentTypeFieldBasicMock
            },
            name: 'categories',
            value: 'A-Z Index'
        };
        comp.ngOnInit();
        expect(comp.placeholder).toBe('A-Z Index');
    });

    describe('Pagination events', () => {
        let spyMethod: jest.SpyInstance;

        beforeEach(() => {
            spyMethod = jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([]));
        });

        it('should change Page', () => {
            comp.handleLazyLoad({ first: 2 });

            expect('').toBe(paginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(2);
            expect(spyMethod).toHaveBeenCalledTimes(1);
        });

        it('should filter', () => {
            comp.handleFilterChange('filter');

            expect('filter').toBe(paginatorService.filter);
            expect(spyMethod).toHaveBeenCalledWith(0);
            expect(spyMethod).toHaveBeenCalledTimes(1);
        });

        it('should valuePropertyName be undefined', () => {
            expect(comp.filterValue).toBe('');
        });
    });
});
