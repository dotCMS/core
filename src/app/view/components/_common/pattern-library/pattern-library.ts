import { Component, ViewEncapsulation, ViewChild } from '@angular/core';
import { LoggerService } from '../../../../api/services/logger.service';
import { Router } from '@angular/router';
import { SelectItem, AutoComplete } from 'primeng/primeng';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'pattern-library',
    styleUrls: ['./pattern-library.scss'],
    templateUrl: 'pattern-library.html'
})

export class PatternLibrary {
    public selectedDummyData = [];
    public submitAttempt = false;
    public actionButtonLog;
    public actionHeaderLog;

    private autocompleteResults: Array<string> = [];
    private cities: SelectItem[];
    private dataTableDummyData: [any];
    private displayDialog = false;
    private model: any = {};
    private buttonActions: [any];
    private actionButtonItems: [any];
    private contentTypeColumns: any;

    private searchableForm: FormGroup;
    private totalRecords: number;
    private sitesCurrentPage: any[] = [];
    private sites: any[] = [];
    private readonly ROWS = 10;

    @ViewChild(AutoComplete) private autoCompleteComponent: AutoComplete;

    constructor(public loggerService: LoggerService, private router: Router, private fb: FormBuilder) {
        this.cities = [];
        this.cities.push({label: 'Select City', value: null});
        this.cities.push({label: 'New York', value: {id: 1, name: 'New York', code: 'NY'}});
        this.cities.push({label: 'Rome', value: {id: 2, name: 'Rome', code: 'RM'}});
        this.cities.push({label: 'London', value: {id: 3, name: 'London', code: 'LDN'}});
        this.cities.push({label: 'Istanbul', value: {id: 4, name: 'Istanbul', code: 'IST'}});
        this.cities.push({label: 'Paris', value: {id: 5, name: 'Paris', code: 'PRS'}});
        this.buttonActions = [
            {
                label: 'Group Actions',
                model: [
                    {label: 'Action One', icon: 'fa-refresh', command: () => {}},
                    {label: 'Action Two', icon: 'fa-close', command: () => {}},
                ]
            },
            {
                label: 'More Actions',
                model: [
                    {label: 'Action Three', icon: 'fa-refresh', command: () => {}},
                    {label: 'Action Four', icon: 'fa-close', command: () => {}},
                ]
            }
        ];
        this.actionButtonItems = [{
            command: () => {
                this.loggerService.info('action update');
            },
            icon: 'fa-refresh',
            label: 'Update'
        },
        {
            command: () => {
                this.loggerService.info('action delete');
            },
            icon: 'fa-close',
            label: 'Delete'
        },
        {
            icon: 'fa-link',
            label: 'Angular.io',
            url: 'http://angular.io'
        },
        {
            icon: 'fa-paint-brush',
            label: 'Theming',
            routerLink: ['/theming']
        }];

        this.contentTypeColumns = [
            {fieldName: 'name', header: 'Name', width: '20%', sortable: true},
            {fieldName: 'variable', header: 'Variable', width: '20%'},
            {fieldName: 'description', header: 'Description', width: '30%'},
            {fieldName: 'nEntries', header: 'Entries', width: '10%'},
            {fieldName: 'modDate', header: 'Last Edit Date', width: '20%', format: 'date', sortable: true}
        ];

        this.actionButtonLog = () => {
            this.loggerService.info('Primary action triggered');
        };

        this.actionHeaderLog = () => {
            this.loggerService.info('Primary command was triggered');
        };
    }

    ngOnInit(): any {

        this.model = {
            checkboxValues: ['Disabled'],
            dropdownNormal: '',
            dropdownWithFilter: '',
            inputTextFloatingLabel: '',
            inputTextRegularLabel: '',
            radioBoxValues: ['Disabled'],
            textareaFloatingLabel: '',
            textareaRegularLabel: ''
        };

        // Fake data for datatable
        this.dataTableDummyData = [
            {vin: 'a1653d4d', brand: 'VW', year: 1998, color: 'White'},
            {vin: 'ddeb9b10', brand: 'Mercedes', year: 1985, color: 'Green'},
            {vin: 'd8ebe413', brand: 'Jaguar', year: 1979, color: 'Silver'},
            {vin: 'aab227b7', brand: 'Audi', year: 1970, color: 'Black'},
            {vin: '631f7412', brand: 'Volvo', year: 1992, color: 'Red'},
            {vin: 'a1653d4d', brand: 'VW 2', year: 1998, color: 'White'},
            {vin: 'ddeb9b10', brand: 'Mercedes 2', year: 1985, color: 'Green'},
            {vin: 'd8ebe413', brand: 'Jaguar 2', year: 1979, color: 'Silver'},
            {vin: 'aab227b7', brand: 'Audi 2', year: 1970, color: 'Black'},
            {vin: '631f7412', brand: 'Volvo 2', year: 1992, color: 'Red'},
            {vin: 'a1653d4d', brand: 'VW 3', year: 1998, color: 'White'},
            {vin: 'ddeb9b10', brand: 'Mercedes 3', year: 1985, color: 'Green'},
            {vin: 'd8ebe413', brand: 'Jaguar 3', year: 1979, color: 'Silver'},
            {vin: 'aab227b7', brand: 'Audi 3', year: 1970, color: 'Black'},
            {vin: '631f7412', brand: 'Volvo 3', year: 1992, color: 'Red'}
        ];

        this.initSites();
    }

    autocompleteComplete($event): void {
        this.autocompleteResults = [];
        this.autocompleteResults = $event.query.split('');
    }

    autocompleteCompleteDropdownClick($event: {originalEvent: Event, query: string}): void {
        $event.originalEvent.preventDefault();
        $event.originalEvent.stopPropagation();
        this.autocompleteResults = [];
        if ($event.query === '') {
            this.autocompleteResults = ['Please', 'type', 'something'];
        } else {
            this.autocompleteResults = $event.query.split('');
        }
        this.autoCompleteComponent.show();
    }

    showDialog(): void {
        this.displayDialog = true;
    }

    handleFilterChange(filter): void {
        this.sitesCurrentPage = this.sites.filter( site => site.name.indexOf(filter) !== -1);
        this.totalRecords = this.sitesCurrentPage.length;
        this.sitesCurrentPage = this.sitesCurrentPage.slice(0, this.ROWS);

    }

    handlePageChange(event): void {
        this.sitesCurrentPage = this.sites.slice(event.first, event.first + this.ROWS);
    }

    private initSites(): void {
        for (let k = 0; k < 50; k++) {
            let text = '';
            let possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

            for ( let i = 0; i < 5; i++ ) {
                text += possible.charAt(Math.floor(Math.random() * possible.length));
            }

            this.sites[k] = {
                id: k,
                name: text
            };
        }

        this.sitesCurrentPage = this.sites.slice(0, this.ROWS);
        this.totalRecords = this.sites.length;
        this.searchableForm = this.fb.group({
            currentSite: '',
            fakeCurrentSite: this.sites[0]
        });
    }
}
