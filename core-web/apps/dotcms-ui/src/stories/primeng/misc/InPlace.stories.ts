import { Story, Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { InputTextModule } from 'primeng/inputtext';
import { moduleMetadata } from '@storybook/angular';
import { InplaceModule } from 'primeng/inplace';
import { TableModule } from 'primeng/table';

export default {
    title: 'PrimeNG/Misc/InPlace',
    parameters: {
        docs: {
            description: {
                component:
                    'Inplace provides an easy to do editing and display at the same time where clicking the output displays the actual content.: https://primefaces.org/primeng/showcase/#/inplace'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                InplaceModule,
                InputTextModule,
                TableModule,
                HttpClientModule
            ]
        })
    ],
    args: {
        records: [
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
        ]
    }
} as Meta;

const InPlaceTemplate = `
  <p-inplace>
    <ng-template pTemplate="display">
        <div class="p-d-inline-flex p-ai-center">
            <span class="pi pi-table" style="vertical-align: middle"></span>
            <span class="p-ml-2">View Data</span>
        </div>
    </ng-template>
    <ng-template pTemplate="content">
        <p-table [value]="records">
            <ng-template pTemplate="header">
                <tr>
                    <th>Vin</th>
                    <th>Year</th>
                    <th>Brand</th>
                    <th>Color</th>
                </tr>
            </ng-template>
            <ng-template pTemplate="body" let-car>
                <tr>
                    <td>{{car.vin}}</td>
                    <td>{{car.year}}</td>
                    <td>{{car.brand}}</td>
                    <td>{{car.color}}</td>
                </tr>
            </ng-template>
        </p-table>
    </ng-template>
</p-inplace>
`;

type Car = {
    brand: string;
    color: string;
    vin: string;
};

const Template: Story<{ records: Car[] }> = (props: { records: Car[] }) => {
    const template = InPlaceTemplate;

    return {
        props,
        template
    };
};

export const Primary: Story = Template.bind({});

Primary.parameters = {
    docs: {
        source: {
            code: InPlaceTemplate
        }
    }
};
