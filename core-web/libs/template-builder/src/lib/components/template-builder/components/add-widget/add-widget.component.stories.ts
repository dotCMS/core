import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';

import { rowIcon } from '../../assets/icons';

export default {
    title: 'AddWidgetComponent',
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

export const Primary = Template.bind({});

export const MaterialIcon = Template.bind({});

Primary.args = {
    label: 'Add Row',
    icon: rowIcon
};

MaterialIcon.args = {
    label: 'Add Box',
    icon: 'add'
};
