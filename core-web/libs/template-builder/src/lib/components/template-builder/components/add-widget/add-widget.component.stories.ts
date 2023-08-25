import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';

export default {
    title: 'Library/Template Builder/Components/Add',
    component: AddWidgetComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
} as Meta<AddWidgetComponent>;

const Template: Story<AddWidgetComponent> = (args: AddWidgetComponent) => ({
    props: args
});

export const AddRow = Template.bind({});

export const AddBox = Template.bind({});

export const MaterialIcon = Template.bind({});

AddRow.args = {
    label: 'Add Row'
};

AddBox.args = {
    label: 'Add Box'
};

MaterialIcon.args = {
    label: 'Fallback Material Icon',
    icon: 'add'
};
