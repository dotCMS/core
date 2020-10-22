// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TableModule, Table } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
export default {
    title: 'PrimeNG/Data/Table',
    component: Table,
    parameters: {
        docs: {
            description: {
                component: 'A listing data table: https://primefaces.org/primeng/showcase/#/table'
            }
        }
    }
} as Meta;

const cars = [
    {
        vin: '123',
        year: '2020',
        brand: 'Hyundai',
        color: 'Red'
    },
    {
        vin: '456',
        year: '2010',
        brand: 'Kia',
        color: 'Blue'
    },
    {
        vin: '789',
        year: '2008',
        brand: 'Ford',
        color: 'Gray'
    },
    {
        vin: '987',
        year: '2018',
        brand: 'Fiat',
        color: 'Green'
    },
    {
        vin: '213',
        year: '2020',
        brand: 'Hyundai',
        color: 'Red'
    },
    {
        vin: '343',
        year: '2010',
        brand: 'Kia',
        color: 'Blue'
    },
    {
        vin: '454',
        year: '2008',
        brand: 'Ford',
        color: 'Gray'
    },
    {
        vin: '897',
        year: '2018',
        brand: 'Fiat',
        color: 'Green'
    },
    {
        vin: '234',
        year: '2020',
        brand: 'Hyundai',
        color: 'Red'
    },
    {
        vin: '892',
        year: '2010',
        brand: 'Kia',
        color: 'Blue'
    },
    {
        vin: '092',
        year: '2008',
        brand: 'Ford',
        color: 'Gray'
    },
    {
        vin: '567',
        year: '2018',
        brand: 'Fiat',
        color: 'Green'
    }
];

const PrimaryTemplate = `
<p-table
  #dt
  [filterDelay]="0"
  [globalFilterFields]="['color', 'brand', 'year']"
  [loading]="loading"
  [paginator]="true"
  [rowsPerPageOptions]="[10,25,50]"
  [rows]="5"
  [showCurrentPageReport]="true"
  [value]="cars"
  currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"
  dataKey="vin"
  styleClass="p-datatable-customers"
  [(selection)]="selectedCars"
>
  <ng-template pTemplate="caption">
    <div class="table-header">
      List of Cars
      <span class="p-input-icon-left">
        <i class="pi pi-search"></i>
        <input
          pInputText
          type="text"
          (input)="dt.filterGlobal($event.target.value, 'contains')"
          placeholder="Global Search"
        />
      </span>
    </div>
  </ng-template>
  <ng-template pTemplate="header">
    <tr>
      <th style="width: 3rem">
        <p-tableHeaderCheckbox></p-tableHeaderCheckbox>
      </th>
      <th pSortableColumn="vin">Vin <p-sortIcon field="vin"></p-sortIcon></th>
      <th pSortableColumn="year">Year <p-sortIcon field="year"></p-sortIcon></th>
      <th pSortableColumn="brand">Brand <p-sortIcon field="brand"></p-sortIcon></th>
      <th pSortableColumn="color">Color <p-sortIcon field="color"></p-sortIcon></th>
      <th>Action</th>
    </tr>
  </ng-template>
  <ng-template pTemplate="body" let-car>
    <tr>
    <td><p-tableCheckbox [value]="car"></p-tableCheckbox></td>
      <td>{{car.vin}}</td>
      <td>{{car.year}}</td>
      <td>{{car.brand}}</td>
      <td>{{car.color}}</td>
      <td>
        <button pButton type="button" icon="pi pi-ellipsis-v" class="p-button-rounded p-button-text" (click)="menu.toggle($event)"></button>
        <p-menu #menu [popup]="true" [model]="items"></p-menu>
      </td>
    </tr>
  </ng-template>
</p-table>
`;
export const Primary = (_args: Table) => {
    return {
        props: {
            cars,
            selectedCars: [],
            car: {},
            handleClick: (e) => {
                console.log(e);
            },
            items: [
                {
                    label: 'Update',
                    icon: 'pi pi-refresh',
                    command: () => {
                        console.log('update');
                    }
                },
                {
                    label: 'Delete',
                    icon: 'pi pi-times',
                    command: () => {
                        console.log('delete');
                    }
                },
                {
                    label: 'Angular.io',
                    icon: 'pi pi-info',
                    command: () => {
                        console.log('angular');
                    }
                },
                { separator: true },
                {
                    label: 'Setup',
                    icon: 'pi pi-cog',
                    command: () => {
                        console.log('setup');
                    }
                }
            ]
        },
        moduleMetadata: {
            imports: [
                TableModule,
                BrowserAnimationsModule,
                ButtonModule,
                MenuModule,
                InputTextModule
            ]
        },
        template: PrimaryTemplate
    };
};

Primary.parameters = {
    docs: {
        source: {
            code: PrimaryTemplate
        },
        iframeHeight: 500
    }
};
