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
    decorators: [
        moduleMetadata({
            declarations: [DotMessagePipe],
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
            ],
            providers: []
        })
    ],
    args: {
        rows: 4,
        pageLinkSize: 2,
        optionsWidth: '300',
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
        label: 'Select something',
        labelPropertyName: 'label'
    },
} as Meta;

const Template: Story<SearchableDropdownComponent> = (args: SearchableDropdownComponent) => ({
    component: SearchableDropdownComponent,
    props: args
});

export const Default = Template.bind({});
