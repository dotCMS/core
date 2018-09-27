import { Component, OnInit } from '@angular/core';
import { SelectItem } from 'primeng/primeng';

@Component({
    selector: 'dot-pattern-library',
    styleUrls: ['./pattern-library.component.scss'],
    templateUrl: 'pattern-library.component.html'
})
export class PatternLibraryComponent implements OnInit {
    dropdownOptions: SelectItem[];
    selectButtonStates: SelectItem[] = [];
    selectButtonValue: string;
    lockerModel = false;
    radioVal1 = 'Option 4';
    checkboxVal1 = ['Option 4'];

    constructor() {}

    ngOnInit() {
        this.dropdownOptions = [
            { label: 'Select City', value: null },
            { label: 'New York', value: { id: 1, name: 'New York', code: 'NY' } },
            { label: 'Rome', value: { id: 2, name: 'Rome', code: 'RM' } },
            { label: 'London', value: { id: 3, name: 'London', code: 'LDN' } },
            { label: 'Istanbul', value: { id: 4, name: 'Istanbul', code: 'IST' } },
            { label: 'Paris', value: { id: 5, name: 'Paris', code: 'PRS' } }
        ];

        this.selectButtonStates = [
            { label: 'Value 1', value: 'one' },
            { label: 'Value 2', value: 'two' },
            { label: 'Value 3', value: 'three' },
            { label: 'Value 4', value: 'four' }
        ];

        this.selectButtonValue = 'one';
    }
}
