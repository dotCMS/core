import { moduleMetadata, Story, Meta } from '@storybook/angular';
import { InputFieldComponent } from './input-field.component';

export default {
    title: 'InputFieldComponent',
    component: InputFieldComponent,
    decorators: [
        moduleMetadata({
            imports: []
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
