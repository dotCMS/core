import { moduleMetadata, StoryObj, Meta } from '@storybook/angular';

import { AddWidgetComponent } from './add-widget.component';

import { rowIcon, colIcon } from '../../assets/icons';

const meta: Meta<AddWidgetComponent> = {
    title: 'Library/Template Builder/Components/Add',
    component: AddWidgetComponent,
    decorators: [
        moduleMetadata({
            imports: []
        })
    ]
};
export default meta;

type Story = StoryObj<AddWidgetComponent>;

export const AddRow: Story = {
    args: {
        label: 'Add Row',
        icon: rowIcon
    }
};

export const AddBox: Story = {
    args: {
        label: 'Add Box',
        icon: colIcon
    }
};

export const MaterialIcon: Story = {
    args: {
        label: 'Fallback Material Icon',
        icon: 'add'
    }
};
