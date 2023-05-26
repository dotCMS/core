import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { DragBoxComponent } from './drag-box.component';

export default {
    title: 'DragBoxComponent',
    component: DragBoxComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
} as Meta<DragBoxComponent>;

const Template: Story<DragBoxComponent> = (args: DragBoxComponent) => ({
    props: args
});

export const Primary = Template.bind({});

Primary.args = {
    label: 'Add Row',
    icon: 'row'
};
