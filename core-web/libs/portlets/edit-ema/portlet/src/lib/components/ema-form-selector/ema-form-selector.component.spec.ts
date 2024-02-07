import { byText, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaFormSelectorComponent } from './ema-form-selector.component';

describe('EmaFormSelectorComponent', () => {
    let spectator: Spectator<EmaFormSelectorComponent>;
    let mockDotContentTypeService: Partial<DotContentTypeService>;
    const createComponent = createComponentFactory({
        component: EmaFormSelectorComponent,
        imports: [CommonModule, TableModule, ButtonModule],
        mocks: [DotContentTypeService],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    name: 'Name',
                    description: 'Description',
                    Actions: 'Actions'
                })
            }
        ]
    });

    beforeEach(() => {
        // Mock the DotContentTypeService and its methods
        mockDotContentTypeService = {
            getByTypes: jest
                .fn()
                .mockReturnValue(of([{ id: '1', name: 'Test Type', description: 'Description' }]))
        };

        spectator = createComponent({
            providers: [{ provide: DotContentTypeService, useValue: mockDotContentTypeService }]
        });
    });

    it('should have table headers', () => {
        const tableHeaders = spectator.queryAll('th');
        expect(tableHeaders.length).toBeGreaterThan(0);
        expect(byText('Name')).not.toBeNull();
        expect(byText('Description')).not.toBeNull();
    });

    it('should display content types in the table', () => {
        const tableRows = spectator.queryAll('tr');
        expect(tableRows.length).toBeGreaterThan(1); // More than the header row
        expect(byText('Test Type')).not.toBeNull();
        expect(byText('Description')).not.toBeNull();
    });

    it('should emit selected event when button is clicked', () => {
        jest.spyOn(spectator.component.selected, 'emit');
        const selectButton = spectator.query('p-button');
        spectator.click(selectButton);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith('1');
    });
});
