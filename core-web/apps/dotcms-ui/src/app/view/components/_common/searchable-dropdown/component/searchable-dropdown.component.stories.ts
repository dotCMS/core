import { faker } from '@faker-js/faker';
import { action } from '@storybook/addon-actions';
import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { PopoverModule } from 'primeng/popover';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { SearchableDropdownComponent } from '.';

const generateFakeOption = () => ({
    label: faker.lorem.words(3),
    value: faker.lorem.slug(2)
});

const data = faker.helpers.multiple(generateFakeOption, { count: 10 });

const meta: Meta<SearchableDropdownComponent> = {
    title: 'DotCMS/Searchable Dropdown',
    component: SearchableDropdownComponent,
    parameters: {
        docs: {
            description: {
                component:
                    'Dropdown with pagination and global search. Please be mindful that the <code>width</code> property is <strong>required</strong>.'
            }
        }
    },
    decorators: [
        moduleMetadata({
            declarations: [SearchableDropdownComponent],
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                CommonModule,
                DataViewModule,
                DotIconModule,
                FormsModule,
                InputTextModule,
                PopoverModule,
                DotMessagePipe,
                HttpClientModule
            ]
        })
    ],
    args: {
        rows: 4,
        pageLinkSize: 2,
        placeholder: 'Select something',
        labelPropertyName: 'label',
        width: '300px',
        cssClass: '',
        data: [...data],
        action: action('action')
    },
    argTypes: {
        width: {
            name: 'width',
            type: { name: 'string', required: true },
            defaultValue: '300',
            description:
                "Setting a width prevents the dropdown from jumping when an option is larger than the dropdown's width",
            control: {
                type: 'text'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: `<dot-searchable-dropdown ${argsToTemplate(args)} />`
    })
};
export default meta;

type Story = StoryObj<SearchableDropdownComponent>;

export const Default: Story = {};

export const CustomTemplate: Story = {
    render: (args) => ({
        props: args,
        template: `
        <dot-searchable-dropdown ${argsToTemplate(args)}>
            <ng-template let-data="data" pTemplate="list">
                @for(item of data; track $index) {
                    <div class="w-full">
                        <p>{{ item.label }} --</p>
                    </div>
                }
            </ng-template>
        </dot-searchable-dropdown>`
    })
};

export const CustomSelectedTemplate: Story = {
    render: (args) => ({
        props: args,
        template: `
        <dot-searchable-dropdown [externalSelectTemplate]="selectTemplate" ${argsToTemplate(args)}>
            <ng-template let-item="item" pTemplate="select">
                <p>--Choose--</p>
            </ng-template>
        </dot-searchable-dropdown>`
    })
};
