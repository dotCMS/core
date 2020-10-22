import { CommonModule } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { moduleMetadata } from '@storybook/angular';
import { Story, Meta } from '@storybook/angular/types-6-0';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { SearchableDropdownComponent } from '.';

@Pipe({
    name: 'dm'
})
class DotMessagePipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

export default {
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
            declarations: [DotMessagePipe, SearchableDropdownComponent],
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                CommonModule,
                DataViewModule,
                DotIconButtonModule,
                DotIconModule,
                FormsModule,
                InputTextModule,
                OverlayPanelModule
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
    args: {
        rows: 4,
        pageLinkSize: 2,
        optionsWidth: '300',
        paginate: () => {},
        showOverlayHandler: () => {},
        hideOverlayHandler: () => {},
        handleClick: () => {},
        data: [
            {
                label: 'This is an option',
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
        ],
        placeholder: 'Select something',
        labelPropertyName: 'label',
        width: '300'
    }
} as Meta;

const getTemplate = (extraAttr = '') => {
    const template = `
        <dot-searchable-dropdown
            ${extraAttr}
            [rows]="rows"
            [pageLinkSize]="pageLinkSize"
            [data]="data"
            [width]="width"
            [labelPropertyName]="labelPropertyName"
            [placeholder]="placeholder"
        >
        </dot-searchable-dropdown>
    `;
    return template;
};

export const Primary: Story<SearchableDropdownComponent> = (props: SearchableDropdownComponent) => {
    return {
        moduleMetadata: {
            declarations: [SearchableDropdownComponent]
        },
        component: SearchableDropdownComponent,
        props,
        template: getTemplate()
    };
};

export const Secondary = (props: SearchableDropdownComponent) => ({
    component: SearchableDropdownComponent,
    props,
    template: getTemplate(`class="d-secondary"`)
});
