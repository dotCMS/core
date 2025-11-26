import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator/jest';

import { Table, TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValue, DotKeyValueComponent } from './dot-key-value-ng.component';
import { DotKeyValueTableHeaderRowComponent } from './dot-key-value-table-header-row/dot-key-value-table-header-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

import { DotIconComponent } from '../../dot-icon/dot-icon.component';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

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
    'keyValue.key_input.required': 'This field is required',
    'keyValue.key_input.duplicated': 'This key already exists'
});

describe('DotKeyValueComponent', () => {
    let spectator: Spectator<DotKeyValueComponent>;

    const createComponent = createComponentFactory({
        component: DotKeyValueComponent,
        imports: [
            DotIconComponent,
            TableModule,
            DotKeyValueTableRowComponent,
            DotKeyValueTableHeaderRowComponent,
            DotMessagePipe
        ],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                showHiddenField: false,
                variables: [...mockKeyValue]
            } as unknown
        });
        spectator.detectChanges();
    });

    it('should load the component with no rows', () => {
        spectator.setInput('variables', []);
        spectator.detectChanges();

        const headerRow = spectator.query(DotKeyValueTableHeaderRowComponent);
        const noRowsElement = spectator.query(byTestId('no-rows'));

        expect(headerRow).toBeTruthy();
        expect(noRowsElement.textContent).toContain('No Rows');
    });

    it('should load the component with data', () => {
        const table = spectator.query(Table);
        const tableRow = spectator.queryAll(DotKeyValueTableRowComponent);

        expect(tableRow.length).toBe(2);
        expect(tableRow[0].$variable()).toEqual(mockKeyValue[0]);
        expect(tableRow[1].$variable()).toEqual(mockKeyValue[1]);
        expect(table.value).toEqual(mockKeyValue);
    });

    it('should update an existing variable', () => {
        const spyUpdate = jest.spyOn(spectator.component.update, 'emit');
        const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');

        const update = {
            ...mockKeyValue[0],
            value: 'new value'
        };

        const rows = spectator.queryAll(DotKeyValueTableRowComponent);
        rows[0].save.emit(update);
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

        spectator.triggerEventHandler(DotKeyValueTableHeaderRowComponent, 'save', newVariable);
        spectator.detectChanges();

        expect(spySave).toHaveBeenCalledWith(newVariable);
        expect(spyUpdatedList).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
    });

    it('should delete a variable from the list', () => {
        const spyDelete = jest.spyOn(spectator.component.delete, 'emit');
        const spyUpdatedList = jest.spyOn(spectator.component.updatedList, 'emit');
        const rows = spectator.queryAll(DotKeyValueTableRowComponent);

        rows[0].delete.emit();
        spectator.detectChanges();

        expect(spyDelete).toHaveBeenCalledWith(mockKeyValue[0]);
        expect(spyUpdatedList).toHaveBeenCalledWith([mockKeyValue[1]]);
    });
});
