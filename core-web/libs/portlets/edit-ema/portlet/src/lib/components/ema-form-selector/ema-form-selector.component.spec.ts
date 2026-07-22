import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';

import { DotContentTypeService, DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaFormSelectorComponent } from './ema-form-selector.component';

const mockForms = [{ id: '1', name: 'Test Form', description: 'Description' }];
const mockPagination = { totalEntries: 1, currentPage: 1, perPage: 40 };

describe('EmaFormSelectorComponent', () => {
    let spectator: Spectator<EmaFormSelectorComponent>;
    const createComponent = createComponentFactory({
        component: EmaFormSelectorComponent,
        imports: [
            TableModule,
            ButtonModule,
            IconFieldModule,
            InputIconModule,
            InputTextModule,
            PaginatorModule,
            ReactiveFormsModule
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    name: 'Name',
                    description: 'Description',
                    Actions: 'Actions',
                    search: 'Search',
                    'No-Results-Found': 'No results found'
                })
            },
            {
                provide: DotContentTypeService,
                useValue: {
                    getContentTypesWithPagination: jest
                        .fn()
                        .mockReturnValue(
                            of({ contentTypes: mockForms, pagination: mockPagination })
                        )
                }
            },
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('should have table headers', () => {
        const tableHeaders = spectator.queryAll('th');
        expect(tableHeaders.length).toBeGreaterThan(0);
    });

    it('should display content types in the table', () => {
        const tableRows = spectator.queryAll('tr');
        expect(tableRows.length).toBeGreaterThan(1);
    });

    it('should emit selected event when button is clicked', () => {
        jest.spyOn(spectator.component.selected, 'emit');
        const selectButton = spectator.query('p-button');
        spectator.click(selectButton);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith('1');
    });

    it('should render search input', () => {
        expect(spectator.query('[data-testid="form-search-input"]')).not.toBeNull();
    });

    it('should call getContentTypesWithPagination with filter after debounce', () => {
        const service = spectator.inject(DotContentTypeService);
        const searchInput = spectator.query<HTMLInputElement>('[data-testid="form-search-input"]');
        spectator.typeInElement('test form', searchInput);
        jest.advanceTimersByTime(300);
        expect(service.getContentTypesWithPagination).toHaveBeenCalledWith(
            expect.objectContaining({ filter: 'test form', page: 1 })
        );
    });

    it('should reset to page 1 and re-fetch when search changes', () => {
        const service = spectator.inject(DotContentTypeService);
        (service.getContentTypesWithPagination as jest.Mock).mockClear();
        const searchInput = spectator.query<HTMLInputElement>('[data-testid="form-search-input"]');
        spectator.typeInElement('form', searchInput);
        jest.advanceTimersByTime(300);
        expect(service.getContentTypesWithPagination).toHaveBeenCalledWith(
            expect.objectContaining({ filter: 'form', page: 1 })
        );
    });

    it('should render paginator', () => {
        expect(spectator.query('[data-testid="form-paginator"]')).not.toBeNull();
    });

    it('should show empty state when no forms are returned', () => {
        const service = spectator.inject(DotContentTypeService);
        (service.getContentTypesWithPagination as jest.Mock).mockReturnValue(
            of({ contentTypes: [], pagination: { totalEntries: 0, currentPage: 1, perPage: 40 } })
        );
        spectator.component['fetch$'].next();
        spectator.detectChanges();
        expect(spectator.query('td[colspan="3"]')).not.toBeNull();
    });

    it('should fetch page 2 and update $first when paginator changes', () => {
        const service = spectator.inject(DotContentTypeService);
        (service.getContentTypesWithPagination as jest.Mock).mockClear();
        spectator.component.onPageChange({ page: 1, first: 40, rows: 40, pageCount: 3 });
        expect(service.getContentTypesWithPagination).toHaveBeenCalledWith(
            expect.objectContaining({ page: 2 })
        );
        expect(spectator.component['$first']()).toBe(40);
    });

    it('should handle HTTP errors without terminating the stream', () => {
        const service = spectator.inject(DotContentTypeService);
        const errorManager = spectator.inject(DotHttpErrorManagerService);
        (service.getContentTypesWithPagination as jest.Mock).mockReturnValue(
            throwError(() => new Error('Network error'))
        );
        spectator.component['fetch$'].next();
        spectator.detectChanges();
        expect(errorManager.handle).toHaveBeenCalled();

        // Stream must still be alive: a subsequent fetch with a good response should work
        (service.getContentTypesWithPagination as jest.Mock).mockReturnValue(
            of({ contentTypes: mockForms, pagination: mockPagination })
        );
        spectator.component['fetch$'].next();
        spectator.detectChanges();
        expect(spectator.component['$forms']()).toEqual(mockForms);
    });
});
