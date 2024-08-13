import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { CheckboxModule } from 'primeng/checkbox';

const cities = [
    { name: 'Chicago', code: 'CHI' },
    { name: 'New York', code: 'NY' },
    { name: 'Los Angeles', code: 'LA' },
    { name: 'Houston', code: 'HOU' },
    { name: 'Philadelphia', code: 'PHI' },
    { name: 'Phoenix', code: 'PHO' },
    { name: 'San Antonio', code: 'SA' },
    { name: 'San Diego', code: 'SD' },
    { name: 'Dallas', code: 'DAL' },
    { name: 'San Jose', code: 'SJ' },
    { name: 'Austin', code: 'AUS' },
    { name: 'Indianapolis', code: 'IND' }
];

const meta: Meta = {
    title: 'PrimeNG/Form/Checkbox',
    parameters: {
        docs: {
            description: {
                component:
                    'Checkbox is an extension to standard checkbox element with theming: https://primeng.org/checkbox'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [CheckboxModule, BrowserAnimationsModule, FormsModule, NgFor]
        })
    ],
    args: {
        cities: [...cities],
        disabled: false,
        invalid: false
    },
    argTypes: {
        cities: {
            control: 'object',
            description: 'List of cities to display as radio buttons'
        },
        disabled: {
            control: 'boolean',
            description: 'Whether the radio buttons are disabled'
        },
        invalid: {
            control: 'boolean',
            description: 'Whether the radio buttons are invalid'
        }
    },
    render: () => ({
        template: `<div class="flex flex-column gap-2">
            <p-checkbox *ngFor="let city of cities" name="city" [value]="city" [(ngModel)]="selectedCity" [inputId]="city.code" [label]="city.name" [disabled]="disabled" [class]="invalid ? 'ng-dirty ng-invalid' : ''"></p-checkbox>
        </div>`
    })
};
export default meta;

type Story = StoryObj;

export const Main: Story = {};
