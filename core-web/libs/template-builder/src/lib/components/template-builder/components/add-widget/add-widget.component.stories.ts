import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';

import { rowIcon, colIcon } from '../../assets/icons';

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
    label: 'Add Row',
    icon: rowIcon
};

AddBox.args = {
    label: 'Add Box',
    icon: colIcon
};

MaterialIcon.args = {
    label: 'Fallback Material Icon',
    icon: 'add'
};
