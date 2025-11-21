import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { Tabs, TabsModule } from 'primeng/tabs';

const BasicTemplate = `
  <p-tabs>
    <p-tab header="Header I">
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
            ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
            Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
    </p-tab>
    <p-tab header="Header II">
        <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi
            architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione
            voluptatem sequi nesciunt. Consectetur, adipisci velit, sed quia non numquam eius modi.</p>
    </p-tab>
    <p-tab header="Header III">
        <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati
            cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio.
            Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus.</p>
    </p-tab>
</p-tabs>
`;

const meta: Meta<Tabs> = {
    title: 'PrimeNG/Panel/Tabs',
    component: Tabs,
    decorators: [
        moduleMetadata({
            imports: [TabsModule, ButtonModule, BrowserAnimationsModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'TabView is a container component to group content with tabs.: https://primefaces.org/primeng/showcase/#/tabview'
            }
        }
    },
    args: {
        activeIndex: 0
    },
    render: (args) => ({
        props: args,
        template: BasicTemplate
    })
};
export default meta;

type Story = StoryObj<Tabs>;

export const Basic: Story = {};
