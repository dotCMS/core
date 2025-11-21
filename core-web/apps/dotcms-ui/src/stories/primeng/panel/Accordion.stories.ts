import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Accordion, AccordionModule } from 'primeng/accordion';

const BasicTemplate = `
    <p-accordion [value]="activeIndex" [expandIcon]="expandIcon" [collapseIcon]="collapseIcon">
        <p-accordion-panel>
            <p-accordion-header>Header I</p-accordion-header>
            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation
                ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
                Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        </p-accordion-panel>
         <p-accordion-panel>
            <p-accordion-header>Header II</p-accordion-header>
            <p>Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi
                architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione
                voluptatem sequi nesciunt. Consectetur, adipisci velit, sed quia non numquam eius modi.</p>
        </p-accordion-panel>
        <p-accordion-panel [disabled]="true">
            <p-accordion-header>Header III</p-accordion-header>
            <p>At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati
                cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio.
                Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus.</p>
        </p-accordion-panel>
    </p-accordion>`;

const meta: Meta<Accordion> = {
    title: 'PrimeNG/Panel/Accordion',
    component: Accordion,
    decorators: [
        moduleMetadata({
            imports: [AccordionModule, BrowserAnimationsModule]
        })
    ],
    args: { activeIndex: 0, expandIcon: 'pi pi-angle-down', collapseIcon: 'pi pi-angle-up' },
    parameters: {
        docs: {
            description: {
                component:
                    'Accordion groups a collection of contents in tabs.: https://www.primefaces.org/primeng-v15-lts/accordion'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: BasicTemplate
    })
};
export default meta;

type Story = StoryObj<Accordion>;

export const Main: Story = {};
