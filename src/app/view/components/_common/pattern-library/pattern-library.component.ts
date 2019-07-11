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
    contentTypeColumns = [
        {
            fieldName: 'name',
            header: 'Name',
            sortable: true
        },
        {
            fieldName: 'variable',
            header: 'Variable',
            sortable: true
        },
        {
            fieldName: 'description',
            header: 'Description',
            sortable: true
        },
        {
            fieldName: 'nEntries',
            header: 'Entries',
            width: '7%'
        },
        {
            fieldName: 'modDate',
            format: 'date',
            header: 'Last Edit Date',
            sortable: true,
            width: '13%'
        }
    ];

    rowActions = [
        {
            menuItem: {
                command: () => {},
                icon: 'delete',
                label: 'Remove'
            }
        }
    ];

    actionHeaderOptions = {
        primary: {
            model: {
                label: 'Delete',
                icon: 'delete'
            }
        }
    };

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

    personaData = {
        archived: false,
        baseType: 'PERSONA',
        contentType: 'persona',
        description:
            'Individual 30 - 70 years of age, with a net worth in excess of $15,000,000 or an annual net income $750,000 or higher.',
        folder: 'SYSTEM_FOLDER',
        hasTitleImage: true,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: 'd4ffa84f-8746-46f8-ac29-1f8ca2c7eaeb',
        inode: 'd475422a-e9f0-4ef5-8797-147f630df47d',
        keyTag: 'WealthyProspect',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '2017-03-02 12:10:40.293',
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        name: 'Wealthy Prospect',
        owner: 'dotcms.org.1',
        personalized: true,
        photo: '/dA/d4ffa84f-8746-46f8-ac29-1f8ca2c7eaeb/photo/wealthy-man-with-jet.jpg',
        photoContentAsset: 'd4ffa84f-8746-46f8-ac29-1f8ca2c7eaeb/photo',
        photoVersion: '/dA/d475422a-e9f0-4ef5-8797-147f630df47d/photo/wealthy-man-with-jet.jpg',
        sortOrder: 0,
        stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
        tags: 'wealth management',
        title: 'Wealthy Prospect',
        titleImage: 'photo',
        url: '/content.d2e60189-86a8-4c74-b49c-8b4702ffa6bb',
        working: true
    };


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
