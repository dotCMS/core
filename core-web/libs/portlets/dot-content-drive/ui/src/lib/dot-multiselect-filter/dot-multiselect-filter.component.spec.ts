import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelect } from 'primeng/multiselect';

import { DotMultiSelectFilterComponent } from './dot-multiselect-filter.component';

describe('DotMultiSelectFilterComponent', () => {
    let spectator: Spectator<DotMultiSelectFilterComponent>;
    let mockMultiSelect: Partial<MultiSelect>;

    const createComponent = createComponentFactory({
        component: DotMultiSelectFilterComponent,
        imports: [FormsModule, CheckboxModule, IconFieldModule, InputIconModule, InputTextModule],
        providers: [],
        detectChanges: false
    });

    beforeEach(() => {
        mockMultiSelect = {
            allSelected: jest.fn(),
            onFilterInputChange: jest.fn(),
            onToggleAll: jest.fn()
        };

        spectator = createComponent({
            providers: [{ provide: MultiSelect, useValue: mockMultiSelect }]
        });

        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should get allSelected from multiSelect', () => {
        (mockMultiSelect.allSelected as jest.Mock).mockReturnValue(true);

        expect(spectator.component.allSelected).toBe(true);
        expect(mockMultiSelect.allSelected).toHaveBeenCalled();
    });

    it('should return false when multiSelect is not available', () => {
        spectator = createComponent({
            providers: [] // No MultiSelect provider
        });
        spectator.detectChanges();

        expect(spectator.component.allSelected).toBe(false);
    });

    it('should handle filter input', () => {
        const mockEvent = new KeyboardEvent('input');

        // Use bracket notation to access protected method
        (
            spectator.component as DotMultiSelectFilterComponent & {
                onFilter: (event: Event) => void;
            }
        ).onFilter(mockEvent);

        expect(mockMultiSelect.onFilterInputChange).toHaveBeenCalledWith(mockEvent);
    });

    it('should toggle check all', () => {
        const mockEvent = {
            originalEvent: new Event('change'),
            checked: true
        };

        // Use bracket notation to access protected method
        (
            spectator.component as DotMultiSelectFilterComponent & {
                toggleCheckAll: (event: { originalEvent: Event; checked: boolean }) => void;
            }
        ).toggleCheckAll(mockEvent);

        expect(mockMultiSelect.onToggleAll).toHaveBeenCalledWith(mockEvent.originalEvent);
    });

    it('should use default filter placeholder', () => {
        expect(spectator.component.filterPlaceholder()).toBe('Search');
    });

    it('should use custom filter placeholder', () => {
        spectator.setInput('filterPlaceholder', 'Custom placeholder');

        expect(spectator.component.filterPlaceholder()).toBe('Custom placeholder');
    });

    it('should render search input with placeholder', () => {
        spectator.setInput('filterPlaceholder', 'Search items');
        spectator.detectChanges();

        const searchInput = spectator.query('input[pInputText]');
        expect(searchInput).toHaveAttribute('placeholder', 'Search items');
    });

    it('should render checkbox for select all', () => {
        (mockMultiSelect.allSelected as jest.Mock).mockReturnValue(true);
        spectator.detectChanges();

        const checkbox = spectator.query('p-checkbox');
        expect(checkbox).toExist();
    });

    it('should trigger onFilter when typing in search input', () => {
        const searchInput = spectator.query('input[pInputText]') as HTMLInputElement;

        spectator.typeInElement('test', searchInput);

        // Verify the MultiSelect method was called (integration test)
        expect(mockMultiSelect.onFilterInputChange).toHaveBeenCalled();
    });

    it('should trigger toggleCheckAll when checkbox changes', () => {
        const checkbox = spectator.query('p-checkbox');
        expect(checkbox).toBeTruthy();

        spectator.triggerEventHandler('p-checkbox', 'onChange', {
            originalEvent: new Event('change'),
            checked: true
        });

        // Verify the MultiSelect method was called (integration test)
        expect(mockMultiSelect.onToggleAll).toHaveBeenCalled();
    });
});
