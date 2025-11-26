import { moduleMetadata, StoryObj, Meta } from '@storybook/angular';

import { CommonModule } from '@angular/common';

import { SuggestionsListItemComponent } from './components/suggestions-list-item/suggestions-list-item.component';
import { SuggestionListComponent } from './suggestion-list.component';

const meta: Meta = {
    title: 'Library/Block Editor/Components/Suggestion List',
    decorators: [
        moduleMetadata({
            declarations: [SuggestionListComponent, SuggestionsListItemComponent],
            imports: [CommonModule]
        })
    ],
    component: SuggestionListComponent,
    render: (args) => ({
        props: args,
        template: `
            <dot-suggestion-list>
                <dot-suggestions-list-item>Option 1</dot-suggestions-list-item>
                <dot-suggestions-list-item>Option 2</dot-suggestions-list-item>
                <dot-suggestions-list-item>Option 3</dot-suggestions-list-item>
            </dot-suggestion-list>
        `
    })
};
export default meta;

type Story = StoryObj;

export const ManyItems: Story = {};
