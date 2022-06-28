import { CommonModule } from '@angular/common';
import { moduleMetadata, Story } from '@storybook/angular';

import { SuggestionsListItemComponent } from '../suggestions-list-item/suggestions-list-item.component';

export default {
    title: 'Suggestions List Item',
    decorators: [
        moduleMetadata({
            declarations: [SuggestionsListItemComponent],
            imports: [CommonModule],
        }),
    ],
    component: SuggestionsListItemComponent
};

export const Icon: Story<SuggestionsListItemComponent> = (args) => ({
    props: {
        ...args,
        item: iconItem
    },
    template: `
        <dotcms-suggestions-list-item [item]='item'></dotcms-suggestions-list-item>
    `
});

export const Image: Story<SuggestionsListItemComponent> = (args) => ({
    props: {
        ...args,
        item: imageItem
    },
    template: `
        <dotcms-suggestions-list-item [item]='item'></dotcms-suggestions-list-item>
    `
});

// Test Data
const iconItem:any = {
    title: 'An Icon',
    type: 'icon',
    icon: '+'
}

const imageItem:any = {
    title: 'Landscape',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1508162942367-e4dd4cd67513?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=1100&q=80'
}