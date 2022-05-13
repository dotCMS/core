import { CommonModule } from '@angular/common';
import { moduleMetadata, Story } from '@storybook/angular';


import { SuggestionsListItemComponent } from '../suggestions-list-item/suggestions-list-item.component';
import { SuggestionListComponent } from "./suggestion-list.component";

export default {
    title: 'Suggestion List',
    decorators: [
        moduleMetadata({
            declarations: [SuggestionListComponent, SuggestionsListItemComponent],
            imports: [CommonModule],
        }),
    ],
    component: SuggestionListComponent
};


export const ManyItems: Story<SuggestionListComponent> = (args) => ({
    props: args,
    template: `
        <dotcms-suggestion-list>
            <dotcms-suggestions-list-item>Option 1</dotcms-suggestions-list-item>
            <dotcms-suggestions-list-item>Option 2</dotcms-suggestions-list-item>
            <dotcms-suggestions-list-item>Option 3</dotcms-suggestions-list-item>
        </dotcms-suggestion-list>
    `,
});
