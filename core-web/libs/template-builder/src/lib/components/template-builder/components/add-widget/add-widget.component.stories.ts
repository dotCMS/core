import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';
import { colIcon, rowIcon } from './utils/icons';

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

export const Secondary = Template.bind({});

Primary.args = {
    label: 'Add Row',
    icon: rowIcon
};

Secondary.args = {
    label: 'Add Box',
    icon: colIcon
};
