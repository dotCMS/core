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


export const Default: Story<SuggestionsListItemComponent> = (args) => ({
    props: args,
    template: `
        <dotcms-suggestions-list-item>Option 1</dotcms-suggestions-list-item>
    `,
});
