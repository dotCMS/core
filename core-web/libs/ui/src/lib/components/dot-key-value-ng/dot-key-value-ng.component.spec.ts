import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { MockProvider } from 'ng-mocks';

import { Table, TableModule } from 'primeng/table';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValue, DotKeyValueComponent } from './dot-key-value-ng.component';
import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row/dot-key-value-table-input-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

export const mockKeyValue: DotKeyValue[] = [
    {
        key: 'name',
        hidden: false,
        value: 'John'
    },
    {
        key: 'password',
        hidden: true,
        value: '123456'
    }
];

const messageServiceMock = new MockDotMessageService({
    'keyValue.actions_header.label': 'Actions',
    'keyValue.value_header.label': 'Value',
    'keyValue.key_header.label': 'Key',
    'keyValue.value_no_rows.label': 'No Rows',
    'keyValue.hidden_header.label': 'Hidden',
    Save: 'Save',
    Cancel: 'Cancel'
});

describe('DotKeyValueComponent', () => {
    let spectator: Spectator<DotKeyValueComponent>;

    const createComponent = createComponentFactory({
        component: DotKeyValueComponent,
        imports: [
            DotIconModule,
            TableModule,
            DotKeyValueTableRowComponent,
            DotKeyValueTableInputRowComponent,
            DotMessagePipe
        ],
        providers: [
            MockProvider(DotMessageDisplayService),
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                showHiddenField: false,
                variables: mockKeyValue
            },
            detectChanges: false
        });
    });

    it('should load the component with no rows', () => {
        spectator.setInput('variables', []);
        spectator.detectChanges();

        const tableHeaders = spectator
            .queryAll('[data-testId="header"] th')
            .map((el) => el.textContent.trim());
        const noRowsElement = spectator.query('[data-testId="no-rows"] td');

        expect(tableHeaders).toEqual(['Key', 'Value', 'Actions']);
        expect(noRowsElement.textContent.trim()).toBe('No Rows');
    });

    it('should load the component with data', () => {
        spectator.detectChanges();
        const table = spectator.query(Table);
        const tableRow = spectator.queryAll(DotKeyValueTableRowComponent);

        expect(tableRow.length).toBe(2);
        expect(tableRow[0].variable).toEqual(mockKeyValue[0]);
        expect(tableRow[1].variable).toEqual(mockKeyValue[1]);
        expect(table.value).toEqual(mockKeyValue);
    });

    it('should call `event.stopPropagation()` when keydown.enter event is triggered', () => {
        const event = new KeyboardEvent('keydown', { bubbles: true, key: 'Enter' });
        const stopPropagationSpy = spyOn(event, 'stopPropagation');
        const table = spectator.query('p-table'); // Use Table directly
        spectator.detectChanges();

        table.dispatchEvent(event);

        expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
    });

    it('should update an existing variable', () => {
        const spyUpdatedVariable = spyOn(spectator.component.updatedVariable, 'emit');
        const spyUpdatedList = spyOn(spectator.component.updatedList, 'emit');

        spectator.detectChanges();

        const tableRow = spectator.query(DotKeyValueTableRowComponent);
        const updatedVariable = {
            ...mockKeyValue[0],
            value: 'new value'
        };

        tableRow.save.emit(updatedVariable);
        expect(spyUpdatedVariable).toHaveBeenCalledWith({
            variable: updatedVariable,
            oldVariable: mockKeyValue[0]
        });
        expect(spyUpdatedList).toHaveBeenCalledWith([updatedVariable, mockKeyValue[1]]);
    });

    it('should save a new variable', () => {
        const spySavedVariable = spyOn(spectator.component.savedVariable, 'emit');
        const spyUpdatedList = spyOn(spectator.component.updatedList, 'emit');
        const newVariable = {
            key: 'newKey',
            value: 'newValue',
            hidden: false
        };

        spectator.detectChanges();

        const tableInput = spectator.query(DotKeyValueTableInputRowComponent);
        tableInput.save.emit(newVariable);

        expect(spySavedVariable).toHaveBeenCalledWith(newVariable);
        expect(spyUpdatedList).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
    });

    it('should delete a variable from the server', () => {
        const spyDeletedVariable = spyOn(spectator.component.deletedVariable, 'emit');
        const spyUpdatedList = spyOn(spectator.component.updatedList, 'emit');

        spectator.detectChanges();

        const tableRow = spectator.query(DotKeyValueTableRowComponent);
        tableRow.delete.emit(mockKeyValue[0]);

        expect(spyDeletedVariable).toHaveBeenCalledWith(mockKeyValue[0]);
        expect(spyUpdatedList).toHaveBeenCalledWith([mockKeyValue[1]]);
    });

    describe('with hidden field', () => {
        beforeEach(() => {
            spectator.setInput('showHiddenField', true);
            spectator.detectChanges();
        });

        it('should load the component with hidden header', () => {
            const tableHeaders = spectator
                .queryAll('[data-testId="header"] th')
                .map((el) => el.textContent.trim());
            expect(tableHeaders).toEqual(['Key', 'Value', 'Hidden', 'Actions']);
        });

        it('should save a hidden variable', () => {
            const spySavedVariable = spyOn(spectator.component.savedVariable, 'emit');
            const spyUpdatedList = spyOn(spectator.component.updatedList, 'emit');

            const newVariable = {
                key: 'newKey',
                value: 'newValue',
                hidden: false
            };

            spectator.detectChanges();

            const tableInput = spectator.query(DotKeyValueTableInputRowComponent);
            tableInput.save.emit(newVariable);

            expect(spySavedVariable).toHaveBeenCalledWith(newVariable);
            expect(spyUpdatedList).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
        });

        it('should update an existing variable', () => {
            const spyUpdatedVariable = spyOn(spectator.component.updatedVariable, 'emit');
            const spyUpdatedList = spyOn(spectator.component.updatedList, 'emit');

            spectator.detectChanges();

            const tableRow = spectator.query(DotKeyValueTableRowComponent);
            const updatedVariable = {
                ...mockKeyValue[0],
                hidden: true
            };

            tableRow.save.emit(updatedVariable);
            expect(spyUpdatedVariable).toHaveBeenCalledWith({
                variable: updatedVariable,
                oldVariable: mockKeyValue[0]
            });
            expect(spyUpdatedList).toHaveBeenCalledWith([updatedVariable, mockKeyValue[1]]);
        });
    });
});
