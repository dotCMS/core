import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ChipModule } from 'primeng/chip';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { RadioButtonModule } from 'primeng/radiobutton';
import { RippleModule } from 'primeng/ripple';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

const meta: Meta = {
    title: 'DotCMS/Forms',
    parameters: {
        docs: {
            description: {
                component:
                    'Text, icon, buttons and other content can be grouped next to an input.: https://primefaces.org/primeng/showcase/#/inputgroup'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [
                FormsModule,
                InputTextModule,
                ButtonModule,
                CheckboxModule,
                RadioButtonModule,
                RippleModule,
                BrowserAnimationsModule,
                TextareaModule,
                SelectModule,
                ChipModule,
                AutoCompleteModule,
                DatePickerModule,
                MultiSelectModule,
                SelectButtonModule
            ]
        })
    ],
    args: {
        options: [
            { label: 'Select Host', value: null },
            { label: 'demo.dotcms.com', value: { id: 1, name: 'demo.dotcms.com' } },
            { label: 'System Host', value: { id: 2, name: 'System Host' } }
        ],
        actions: [
            { label: 'System Workflow', value: null },
            { label: 'Save', value: { id: 1, name: 'Save' } },
            { label: 'Publish', value: { id: 2, name: 'Publish' } },
            { label: 'Delete', value: { id: 3, name: 'Delete' } }
        ],
        values: ['hiking'],
        results: [
            { name: 'Blogs', id: 1 },
            { name: 'System Workflow', id: 2 }
        ],
        cities: [
            { name: 'New York', code: 'NY' },
            { name: 'Rome', code: 'RM' },
            { name: 'London', code: 'LDN' },
            { name: 'Istanbul', code: 'IST' },
            { name: 'Paris', code: 'PRS' }
        ],
        selectedCities: [{ name: 'Paris', code: 'PRS' }],
        paymentOptions: [
            { name: 'Option 1', value: 1 },
            { name: 'Option 2', value: 2 },
            { name: 'Option 3', value: 3 }
        ],
        search() {
            this.results = [
                { name: 'Blogs', id: 1 },
                { name: 'System Workflow', id: 2 }
            ];
        },
        width: '200'
    }
};
export default meta;

type Story = StoryObj;

const TextInputDefault = `
<div class="p-field">
    <label for="widgetName">Widget Name</label>
    <input id="name" aria-describedby="widgetName-help" pInputText />
    <small id="widgetName-help">Enter the widget name.</small>
</div>
<div class="p-field">
    <label for="slug">Slug</label>
    <input id="slug" aria-describedby="slug-help" class="p-invalid" pInputText />
    <small id="slug-help" class="p-invalid">Slug must be unique.</small>
</div>
`;

const TextInputFloatingLabel = `
<div class="p-field">
    <span class="p-float-label">
        <input id="pInputText" type="text" pInputText>
        <label for="pInputText">Input Text</label>
    </span>
</div>
`;

const InputTemplate = (input) => {
    return `
    <div class="p-fluid" style="width: 400px; margin: 0 auto;">
    ${input}
    <div class="p-field">
        <label for="autoComplete">AutoComplete</label>
        <p-autoComplete
            id="autoComplete"
            (completeMethod)="search($event)"
            [(ngModel)]="text"
            [dropdown]="true"
            [suggestions]="results"
            field="name"
        ></p-autoComplete>
    </div>

    <div class="p-field">
        <label for="dropdown">Dropdown</label>
        <p-dropdown
            id="dropdown"
            inputId="dropdown"
            selectId="dropdown"
            [options]="actions"
        ></p-dropdown>
    </div>

    <div class="p-field">
        <label for="calendar">Calendar</label>
        <p-calendar [showTime]="true" inputId="calendar"></p-calendar>
    </div>

    <div class="p-field">
        <label for="tags">Tags</label>
        <p-chips id="tags" [(ngModel)]="values"></p-chips>
    </div>

    <div class="p-field">
        <label for="multiSelect">Multiselect</label>
        <p-multiSelect
            [options]="cities"
            id="multiSelect"
            placeholder="Select a City"
            optionLabel="name"
        ></p-multiSelect>
    </div>

    <div class="p-field">
        <label for="buttonselect">Button Select</label>
        <p-selectButton [options]="paymentOptions" [(ngModel)]="value2" multiple="multiple" optionLabel="name"></p-selectButton>
    </div>

    <div class="p-field">
        <label>Checkbox</label>

        <div class="field-checkbox">
            <p-checkbox name="group1" value="New York" inputId="ny"></p-checkbox>
            <label for="ny">New York</label>
        </div>
        <div class="field-checkbox">
            <p-checkbox name="group1" value="San Francisco" inputId="sf"></p-checkbox>
            <label for="sf">San Francisco</label>
        </div>
        <div class="field-checkbox">
            <p-checkbox name="group1" value="Los Angeles" inputId="la"></p-checkbox>
            <label for="la">Los Angeles</label>
        </div>
        <div class="field-checkbox">
            <p-checkbox name="group1" value="Chicago" inputId="ch"></p-checkbox>
            <label for="ch">Chicago</label>
        </div>
    </div>

    <div class="p-field">
        <label>Radio</label>
        <div class="field-checkbox">
            <p-radioButton
                name="size"
                [(ngModel)]="city"
                value="Small"
                inputId="size1"
            ></p-radioButton>
            <label for="size1">Small</label>
        </div>
        <div class="field-checkbox">
            <p-radioButton
                name="size"
                [(ngModel)]="city"
                value="Medium"
                inputId="size2"
            ></p-radioButton>
            <label for="size2">Medium</label>
        </div>
        <div class="field-checkbox">
            <p-radioButton
                name="size"
                [(ngModel)]="city"
                value="Big"
                inputId="size3"
            ></p-radioButton>
            <label for="size3">Big</label>
        </div>
    </div>

    <div class="p-field">
        <label for="comments">Comments</label>
        <textarea
            id="comments"
            pInputTextarea
            placeholder="Some placeholder"
            [rows]="10"
        ></textarea>
    </div>
</div>
    `;
};

export const Vertical: Story = {
    parameters: {
        docs: {
            source: {
                code: InputTemplate(TextInputDefault)
            }
        }
    },
    render: (props) => {
        return {
            template: InputTemplate(TextInputDefault),
            props
        };
    }
};

const HorizontalTemplate = `
<h3>Inline</h3>
<div class="formgroup-inline">
    <div class="p-field">
        <label for="firstname" class="p-sr-only">Firstname</label>
        <input id="firstname" type="text" pInputText placeholder="Firstname" />
    </div>
    <div class="p-field">
        <label for="dropdown" class="p-sr-only">Dropdown</label>
        <p-dropdown
            id="dropdown"
            inputId="dropdown"
            selectId="dropdown"
            [options]="options"
            [style]="{'width': width + 'px'}"
        ></p-dropdown>
    </div>
    <div class="p-field">
        <label for="lastname" class="p-sr-only">AutoComplete</label>
        <p-autoComplete
            id="workflow"
            (completeMethod)="search($event)"
            [(ngModel)]="text"
            [dropdown]="true"
            [suggestions]="results"
            field="name"
        ></p-autoComplete>
    </div>
    <div class="p-field">
        <label for="lastname" class="p-sr-only">AutoComplete</label>
        <p-multiSelect
            [options]="cities"
            placeholder="Select a City"
            optionLabel="name"
        ></p-multiSelect>
    </div>
    <button pButton type="button">
        <span pButtonLabel>Submit</span>
    </button>
</div>


<h3>Grid</h3>
<div class="p-fluid p-formgrid grid">
    <div class="p-field p-col">
        <label for="firstname1">Firstname</label>
        <input id="firstname1" type="text" pInputText>
    </div>
    <div class="p-field p-col">
        <label for="lastname1">Lastname</label>
        <input id="lastname1" type="text" pInputText>
    </div>
</div>

<h3>Small</h3>
<div class="formgroup-inline">
    <div class="p-field">
        <label for="firstname" class="p-sr-only">Firstname</label>
        <input id="firstname" type="text" pInputText placeholder="Firstname" class="p-inputtext-sm" />
    </div>
    <div class="p-field">
        <label for="dropdown" class="p-sr-only">Dropdown</label>
        <p-dropdown
            class="p-dropdown-sm"
            id="dropdown"
            inputId="dropdown"
            selectId="dropdown"
            [options]="options"
        ></p-dropdown>
    </div>
    <button pButton type="button" class="p-button-sm">
        <span pButtonLabel>Submit</span>
    </button>
</div>
`;

export const Horizontal: Story = {
    parameters: {
        layout: 'centered',
        docs: {
            source: {
                code: HorizontalTemplate
            }
        }
    },
    render: (props) => {
        return {
            template: HorizontalTemplate,
            props
        };
    }
};

export const FloatingLabel: Story = {
    parameters: {
        docs: {
            source: {
                code: InputTemplate(TextInputFloatingLabel)
            }
        }
    },
    render: (props) => {
        return {
            template: TextInputFloatingLabel,
            props
        };
    }
};
