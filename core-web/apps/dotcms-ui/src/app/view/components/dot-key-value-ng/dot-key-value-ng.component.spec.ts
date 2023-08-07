import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { TableModule } from 'primeng/table';

import { DotKeyValueTableRowModule } from '@components/dot-key-value-ng/dot-key-value-table-row/dot-key-value-table-row.module';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';

import { DotKeyValueComponent } from './dot-key-value-ng.component';
import { DotKeyValueTableInputRowModule } from './dot-key-value-table-input-row/dot-key-value-table-input-row.module';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

export const mockKeyValue = [
    {
        key: 'name',
        hidden: false,
        value: 'John'
    },
    {
        key: 'password',
        hidden: true,
        value: '*****'
    }
];

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-key-value-ng
            [showHiddenField]="showHiddenField"
            [variables]="value"
        ></dot-key-value-ng>
    `
})
class TestHostComponent {
    showHiddenField: boolean;
    value: DotKeyValue[] = mockKeyValue;
}

describe('DotKeyValueComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let deHost: DebugElement;
    let component: DotKeyValueComponent;
    let de: DebugElement;
    let tableRow: DotKeyValueTableRowComponent;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'keyValue.actions_header.label': 'Actions',
            'keyValue.value_header.label': 'Value',
            'keyValue.key_header.label': 'Key',
            'keyValue.value_no_rows.label': 'No Rows',
            'keyValue.hidden_header.label': 'Hidden',
            Save: 'Save',
            Cancel: 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent, DotKeyValueComponent],
            imports: [
                DotIconModule,
                TableModule,
                DotKeyValueTableRowModule,
                DotKeyValueTableInputRowModule,
                DotMessagePipe
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotMessageDisplayService
            ]
        });

        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-key-value-ng'));
        component = de.componentInstance;
    });

    it('should load the component with no rows', () => {
        componentHost.value = [];
        fixtureHost.detectChanges();

        const labels = de
            .queryAll(By.css('[data-testId="header"] th'))
            .map((el) => el.nativeElement.textContent.replace(/^\s+|\s+$/gm, ''));
        expect(labels).toEqual(['Key', 'Value', 'Actions']);

        const noRows = de.query(By.css('[data-testId="no-rows"] td')).nativeElement.innerText;
        expect(noRows).toBe('No Rows');
    });

    it('should load the component with data', () => {
        fixtureHost.detectChanges();
        const dataTable = de.query(By.css('p-table'));
        tableRow = de.query(By.css('dot-key-value-table-row')).componentInstance;
        expect(tableRow.variablesList).toEqual(mockKeyValue);
        expect(dataTable.componentInstance.value).toEqual(mockKeyValue);
        expect(dataTable.listeners[0].name).toBe('keydown.enter');
    });

    it('should save an existing variable', () => {
        spyOn(component.save, 'emit');

        fixtureHost.detectChanges();

        tableRow = de.query(By.css('dot-key-value-table-row')).componentInstance;
        tableRow.save.emit(mockKeyValue[0]);
        expect(component.save.emit).toHaveBeenCalledWith(mockKeyValue[0]);
        expect(component.variables.length).toBe(2);
    });

    it('should save a new variable', () => {
        spyOn(component.save, 'emit');

        fixtureHost.detectChanges();

        tableRow = de.query(By.css('dot-key-value-table-input-row')).componentInstance;
        tableRow.save.emit(mockKeyValue[0]);
        expect(component.save.emit).toHaveBeenCalledWith(mockKeyValue[0]);
    });

    it('should delete a variable from the server', () => {
        spyOn(component.delete, 'emit');
        fixtureHost.detectChanges();

        tableRow = de.query(By.css('dot-key-value-table-row')).componentInstance;
        tableRow.delete.emit(mockKeyValue[0]);

        expect(component.delete.emit).toHaveBeenCalledWith(mockKeyValue[0]);
    });

    it('should restore a variable value with saved value in the UI, when cancelled', () => {
        fixtureHost.detectChanges();

        tableRow = de.query(By.css('dot-key-value-table-row')).componentInstance;
        tableRow.cancel.emit(0);

        expect(component.variables[0]).toEqual(component.variablesBackup[0]);
    });

    it('should stop propagation of keydown.enter event in the datatable', () => {
        fixtureHost.detectChanges();
        const stopPropagationSpy = jasmine.createSpy('spy');
        const dataTable = de.query(By.css('p-table'));
        dataTable.triggerEventHandler('keydown.enter', {
            stopPropagation: stopPropagationSpy
        });

        expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
    });

    it('should load the component with hidden header', () => {
        componentHost.value = [];
        componentHost.showHiddenField = true;
        fixtureHost.detectChanges();

        const labels = de
            .queryAll(By.css('[data-testId="header"] th'))
            .map((el) => el.nativeElement.textContent.replace(/^\s+|\s+$/gm, ''));
        expect(labels).toEqual(['Key', 'Value', 'Hidden', 'Actions']);
    });

    it('should save a hidden variable', () => {
        spyOn(component.save, 'emit');

        fixtureHost.detectChanges();

        tableRow = de.query(By.css('dot-key-value-table-input-row')).componentInstance;
        tableRow.save.emit(mockKeyValue[1]);
        expect(component.save.emit).toHaveBeenCalledWith(mockKeyValue[1]);
        expect(component.variables[1]).toEqual({ ...mockKeyValue[1], value: '********' });
    });
});
