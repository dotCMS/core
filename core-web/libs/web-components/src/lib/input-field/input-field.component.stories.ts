import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { InputFieldComponent } from './input-field.component';

export default {
    title: 'Library/Web Components/Input Field',
    component: InputFieldComponent,
    decorators: [
        moduleMetadata({
            imports: [CommonModule, FormsModule, InputTextModule]
        })
    ]
} as Meta<InputFieldComponent>;

const Template: Story<InputFieldComponent> = (args: InputFieldComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {
    label: 'Name',
    placeholder: 'Enter Name',
    type: 'text',
    value: ''
};
