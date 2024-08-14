import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { SearchableDropdownComponent } from '.';

const data = [
    {
        label: 'This is a really long option to test the power of the ellipsis in this component',
        value: 'option1'
    },
    {
        label: 'Hola Mundo',
        value: 'option2'
    },
    {
        label: 'Freddy',
        value: 'option3'
    },
    {
        label: 'DotCMS',
        value: 'option4'
    },
    {
        label: 'Hybrid CMS',
        value: 'option5'
    },
    {
        label: 'Trying a really long long long option to see what happen',
        value: 'option6'
    },
    {
        label: 'Option',
        value: 'option'
    },
    {
        label: 'Be here now',
        value: 'beherenow'
    },
    {
        label: 'And now what?',
        value: 'nowwhat'
    },
    {
        label: 'More and more',
        value: 'more'
    },
    {
        label: 'And the last one',
        value: 'lastone'
    }
];

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
                OverlayPanelModule,
                DotMessagePipe,
                HttpClientModule
            ]
        })
    ],
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

export const Default: Story = {
    args: {
        rows: 4,
        pageLinkSize: 2,
        placeholder: 'Select something',
        labelPropertyName: 'label',
        width: '300px',
        cssClass: '',
        data: [...data]
    }
};

export const Secondary: Story = {
    args: {
        ...Default.args,
        cssClass: 'd-secondary'
    },
    render: (args) => ({
        props: args,
        template: `
        <ng-template let-data="data" #rowTemplate>
            <div class="w-full" *ngFor="let item of data">
                <p>{{ item.label }} --</p>
            </div>
        </ng-template>
        <dot-searchable-dropdown [externalItemListTemplate]="rowTemplate" ${argsToTemplate(args)} />`
    })
};
