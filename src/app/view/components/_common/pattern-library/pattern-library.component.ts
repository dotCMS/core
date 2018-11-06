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
    dialogShow = false;
    dialog2Show = false;
    dateFieldMinDate = new Date();

    cols2 = [
        { field: 'vin', header: 'Vin' },
        { field: 'year', header: 'Year' },
        { field: 'brand', header: 'Brand' },
        { field: 'color', header: 'Color' }
    ];


    cars = [
        { brand: 'VW', year: 2012, color: 'Orange', vin: 'dsad231ff' },
        { brand: 'Audi', year: 2011, color: 'Black', vin: 'gwregre345' },
        { brand: 'Renault', year: 2005, color: 'Gray', vin: 'h354htr' },
        { brand: 'BMW', year: 2003, color: 'Blue', vin: 'j6w54qgh' },
        { brand: 'Mercedes', year: 1995, color: 'Orange', vin: 'hrtwy34' },
        { brand: 'Volvo', year: 2005, color: 'Black', vin: 'jejtyj' },
        { brand: 'Honda', year: 2012, color: 'Yellow', vin: 'g43gr' },
        { brand: 'Jaguar', year: 2013, color: 'Orange', vin: 'greg34' },
        { brand: 'Ford', year: 2000, color: 'Black', vin: 'h54hw5' },
        { brand: 'Fiat', year: 2013, color: 'Red', vin: '245t2s' }
    ];

    selectedCar1 = {brand: 'VW', year: 2012, color: 'Orange', vin: 'dsad231ff'};


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

    onBeforeClose($event: { close: () => void }): void {
        if (confirm('Are you sure you want to close the dialog?')) {
            $event.close();
        }
    }
}
