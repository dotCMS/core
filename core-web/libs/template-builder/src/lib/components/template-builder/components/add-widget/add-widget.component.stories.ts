import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';

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

Primary.args = {
    label: 'Add Row',
    type: 'row'
};
