import { moduleMetadata, StoryObj, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { SuggestionsListItemComponent } from './suggestions-list-item.component';

type Args = SuggestionsListItemComponent & { item: Record<string, string> };

const meta: Meta<Args> = {
    title: 'Library/Block Editor/Components/Suggestions List Item',
    decorators: [
        moduleMetadata({
            declarations: [SuggestionsListItemComponent],
            imports: [CommonModule]
        })
    ],
    component: SuggestionsListItemComponent,
    render: (args) => ({
        props: args,
        template: `
            <dot-suggestions-list-item [item]='item' />
        `
    })
};
export default meta;

type Story = StoryObj<Args>;

const iconItem: Record<string, string> = {
    title: 'An Icon',
    type: 'icon',
    icon: '+'
};

export const Icon: Story = {
    args: {
        item: iconItem
    }
};

const imageItem: Record<string, string> = {
    title: 'Landscape',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1508162942367-e4dd4cd67513?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=1100&q=80'
};

export const Image: Story = {
    args: {
        item: imageItem
    }
};
