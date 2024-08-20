import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AccordionModule } from 'primeng/accordion';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { FocusTrapModule } from 'primeng/focustrap';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';

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
    <button pButton type="button" icon="pi pi-check" label="Check"></button>

    <h5>Disabled Button</h5>
    <button pButton type="button" icon="pi pi-check" [disabled]="true" label="Disabled"></button>

    <h5>Button with tabindex -1</h5>
    <button pButton type="button" icon="pi pi-check" tabindex="-1"  label="Check"></button>

    <h5>Dropdown</h5>
    <p-dropdown [options]="cities" [(ngModel)]="selectedCity" placeholder="Select a City" optionLabel="name" [showClear]="true"></p-dropdown>
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
                CalendarModule,
                MultiSelectModule,
                DropdownModule,
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
