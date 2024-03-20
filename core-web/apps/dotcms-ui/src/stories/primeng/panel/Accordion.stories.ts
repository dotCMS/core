import { Meta, Story } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Accordion, AccordionModule } from 'primeng/accordion';

export default {
    title: 'PrimeNG/Panel/Accordion',
    component: Accordion,
    args: { activeIndex: 0, expandIcon: 'pi pi-angle-down', collapseIcon: 'pi pi-angle-up' },
    parameters: {
        docs: {
            description: {
                component:
                    'Accordion groups a collection of contents in tabs.: https://www.primefaces.org/primeng-v15-lts/accordion'
            }
        }
    }
} as Meta;

const BasicTemplate = `
    <p-accordion [activeIndex]="activeIndex" [expandIcon]="expandIcon" [collapseIcon]="collapseIcon">
        <p-accordionTab header="Header I" iconPos="end">
            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
                ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
                Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        </p-accordionTab>
         <p-accordionTab header="Header II" iconPos="end" >
            <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi
                architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione
                voluptatem sequi nesciunt. Consectetur, adipisci velit, sed quia non numquam eius modi.</p>
        </p-accordionTab>
        <p-accordionTab header="Header III" iconPos="end" [disabled]="true">
            <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati
                cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio.
                Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus.</p>
        </p-accordionTab>
    </p-accordion>`;

export const Main: Story = (args) => {
    return {
        props: {
            activeIndex: args.activeIndex,
            expandIcon: args.expandIcon,
            collapseIcon: args.collapseIcon
        },
        moduleMetadata: { imports: [AccordionModule, BrowserAnimationsModule] },
        template: BasicTemplate
    };
};
