import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
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
            }
        });
        spectator.detectChanges();
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
        const table = spectator.query(Table);
        const tableRow = spectator.queryAll(DotKeyValueTableRowComponent);

        expect(tableRow.length).toBe(2);
        expect(tableRow[0].variable).toEqual(mockKeyValue[0]);
        expect(tableRow[1].variable).toEqual(mockKeyValue[1]);
        expect(table.value).toEqual(mockKeyValue);
    });

    it('should call `event.stopPropagation()` when keydown.enter event is triggered', () => {
        const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
        const stopPropagationSpy = jest.spyOn(event, 'stopPropagation');
        const table = spectator.query('p-table');

        table.dispatchEvent(event);
        spectator.detectChanges();

        expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
    });

    it('should update an existing variable', () => {
        const spyUpdate = jest.spyOn(spectator.component.update, 'emit');
        const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
        const tableRow = spectator.query(DotKeyValueTableRowComponent);

        const update = {
            ...mockKeyValue[0],
            value: 'new value'
        };

        tableRow.save.emit(update);
        spectator.detectChanges();

        expect(spyUpdate).toHaveBeenCalledWith({
            variable: update,
            oldVariable: mockKeyValue[0]
        });
        expect(spyUpdatedList).toHaveBeenCalledWith([update, mockKeyValue[1]]);
    });

    it('should save a new variable', () => {
        const spySave = jest.spyOn(spectator.component.save, 'emit');
        const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
        const newVariable = {
            key: 'newKey',
            value: 'newValue',
            hidden: false
        };

        const tableInput = spectator.query(DotKeyValueTableInputRowComponent);
        tableInput.save.emit(newVariable);
        spectator.detectChanges();

        expect(spySave).toHaveBeenCalledWith(newVariable);
        expect(spyUpdatedList).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
    });

    it('should delete a variable from the list', () => {
        const spyDelete = jest.spyOn(spectator.component.delete, 'emit');
        const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
        const tableRow = spectator.query(DotKeyValueTableRowComponent);

        tableRow.delete.emit(mockKeyValue[0]);
        spectator.detectChanges();

        expect(spyDelete).toHaveBeenCalledWith(mockKeyValue[0]);
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
            const spySave = jest.spyOn(spectator.component.save, 'emit');
            const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
            const newVariable = {
                key: 'newKey',
                value: 'newValue',
                hidden: true
            };

            const tableInput = spectator.query(DotKeyValueTableInputRowComponent);
            tableInput.save.emit(newVariable);
            spectator.detectChanges();

            expect(spySave).toHaveBeenCalledWith(newVariable);
            expect(spyUpdatedList).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
        });

        it('should update an existing variable', () => {
            const spyUpdate = jest.spyOn(spectator.component.update, 'emit');
            const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
            const tableRow = spectator.query(DotKeyValueTableRowComponent);

            const update = {
                ...mockKeyValue[0],
                hidden: true
            };

            tableRow.save.emit(update);
            spectator.detectChanges();

            expect(spyUpdate).toHaveBeenCalledWith({
                variable: update,
                oldVariable: mockKeyValue[0]
            });
            expect(spyUpdatedList).toHaveBeenCalledWith([update, mockKeyValue[1]]);
        });
    });
});
