// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { Menu } from 'primeng/menu';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TabViewModule, TabView } from 'primeng/tabview';
import { ButtonModule } from 'primeng/button';

export default {
    title: 'PrimeNG/Tabs/TabView',
    component: TabView,
    parameters: {
        docs: {
            description: {
                component:
                    'TabView is a container component to group content with tabs.: https://primefaces.org/primeng/showcase/#/tabview'
            }
        }
    }
} as Meta;

const BasicTemplate = `
  <p-tabView>
    <p-tabPanel header="Header I">
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
            ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
            Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
    </p-tabPanel>
    <p-tabPanel header="Header II">
        <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi
            architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione
            voluptatem sequi nesciunt. Consectetur, adipisci velit, sed quia non numquam eius modi.</p>
    </p-tabPanel>
    <p-tabPanel header="Header III">
        <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati
            cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio.
            Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus.</p>
    </p-tabPanel>
</p-tabView>
`;

// tslint:disable-next-line: variable-name
export const Basic = (_args: Menu) => {
    return {
        props: {
            activeIndex: 0
        },
        moduleMetadata: {
            imports: [TabViewModule, ButtonModule, BrowserAnimationsModule]
        },
        template: BasicTemplate
    };
};
