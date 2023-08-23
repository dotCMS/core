// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';

export default {
    title: 'PrimeNG/Form/AutoComplete',
    component: AutoComplete,
    parameters: {
        docs: {
            description: {
                component:
                    'AutoComplete is an input component that provides real-time suggestions when being typed: https://primeng.org/autocomplete'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [AutoCompleteModule, BrowserAnimationsModule]
        })
    ],
    args: {
        results: [
            { name: 'Afghanistan', code: 'AF' },
            { name: 'Albania', code: 'AL' },
            { name: 'Venezuela', code: 'VE' }
        ],
        // tslint:disable-next-line: typedef
        search() {
            this.results = [
                { name: 'Afghanistan', code: 'AF' },
                { name: 'Albania', code: 'AL' },
                { name: 'Venezuela', code: 'VE' }
            ];
        }
    }
} as Meta;

const AutocompleteTemplate = `
<p-autoComplete
(completeMethod)="search($event)"
[(ngModel)]="text"
[dropdown]="true"
[suggestions]="results"
field="name"
></p-autoComplete>
`;

const Template: Story<AutoComplete> = (props: AutoComplete) => {
    const template = AutocompleteTemplate;

    return {
        props,
        template
    };
};

export const Primary: Story = Template.bind({});

Primary.parameters = {
    docs: {
        source: {
            code: AutocompleteTemplate
        }
    }
};
