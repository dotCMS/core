// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { AutoCompleteModule, AutoComplete } from 'primeng/autocomplete';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
    title: 'PrimeNG/Form/AutoComplete',
    component: AutoComplete,
    parameters: {
        docs: {
            description: {
                component:
                    'AutoComplete is an input component that provides real-time suggestions when being typed: https://primefaces.org/primeng/showcase/#/autocomplete'
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
