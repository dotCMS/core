import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AccordionModule } from 'primeng/accordion';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { FocusTrapModule } from 'primeng/focustrap';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';

const FocusTrapTemplate = `
  <div pFocusTrap class="card">
    <h5>Input</h5>
    <input id="input" type="text" size="30" pInputText>

    <h5>Float Label</h5>
    <span class="p-float-label">
        <input id="float-input" type="text" size="30" pInputText>
        <label for="float-input">Username</label>
    </span>

    <h5>Disabled Input</h5>
    <input id="disabled-input" type="text" size="30" pInputText [disabled]="true" />

    <h5>Input with tabindex -1</h5>
    <input type="text" size="30" pInputText tabindex="-1" />

    <h5>Button</h5>
    <button pButton type="button">
        <i class="pi pi-check" pButtonIcon></i>
        <span pButtonLabel>Check</span>
    </button>

    <h5>Disabled Button</h5>
    <button pButton type="button" [disabled]="true">
        <i class="pi pi-check" pButtonIcon></i>
        <span pButtonLabel>Disabled</span>
    </button>

    <h5>Button with tabindex -1</h5>
    <button pButton type="button" tabindex="-1">
        <i class="pi pi-check" pButtonIcon></i>
        <span pButtonLabel>Check</span>
    </button>

    <h5>Dropdown</h5>
    <p-select [options]="cities" [(ngModel)]="selectedCity" placeholder="Select a City" optionLabel="name" [showClear]="true"></p-select>
</div>
`;

const meta: Meta = {
    title: 'PrimeNG/Misc/FocusTrap',
    parameters: {
        docs: {
            description: {
                component:
                    'Focus Trap keeps focus within a certain DOM element while tabbing: https://primefaces.org/primeng/showcase/#/focustrap'
            },
            source: {
                code: FocusTrapTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                FormsModule,
                DialogModule,
                ButtonModule,
                InputTextModule,
                AccordionModule,
                FocusTrapModule,
                AutoCompleteModule,
                DatePickerModule,
                MultiSelectModule,
                SelectModule,
                HttpClientModule
            ]
        })
    ],
    args: {
        cities: [
            { name: 'New York', code: 'NY' },
            { name: 'Rome', code: 'RM' },
            { name: 'London', code: 'LDN' },
            { name: 'Istanbul', code: 'IST' },
            { name: 'Paris', code: 'PRS' }
        ]
    },
    render: (args) => ({
        props: args,
        template: FocusTrapTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Primary: Story = {};
