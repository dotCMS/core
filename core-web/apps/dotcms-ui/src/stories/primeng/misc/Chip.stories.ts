import {
    Meta,
    StoryObj,
    moduleMetadata,
    componentWrapperDecorator,
    argsToTemplate
} from '@storybook/angular';

import { Chip, ChipModule } from 'primeng/chip';

type Args = Chip & {
    size: string;
    severity: string;
};

const meta: Meta<Args> = {
    title: 'PrimeNG/Misc/Chip',
    component: Chip,
    decorators: [
        moduleMetadata({
            imports: [ChipModule]
        }),
        componentWrapperDecorator(
            (story) => `<div class="card flex justify-content-center">${story}</div>`
        )
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Chip represents entities using icons, labels and images: https://www.primefaces.org/primeng-v15-lts/chip'
            }
        }
    },
    args: {
        label: 'Text',
        icon: 'pi pi-image',
        size: 'default',
        severity: 'default',
        styleClass: '',
        removable: true
    },
    argTypes: {
        label: { description: 'Defines the text to display' },
        icon: { description: 'Defines the Prime Icon to display.' },
        styleClass: { description: 'Class of the element' },
        style: {
            control: { type: 'object' },
            description: 'Inline style of the element'
        },
        removable: {
            control: { type: 'boolean' },
            description: 'Whether to display a remove icon.'
        },
        size: {
            options: ['p-chip-sm'],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the size of the chip'
        },
        severity: {
            options: [
                'p-chip-secondary',
                'p-chip-warning',
                'p-chip-success',
                'p-chip-error',
                'p-chip-gray'
            ],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the severity of the chip'
        }
    },
    render: (args: Args) => ({
        props: {
            ...args
        },
        template: `
         <p-chip ${argsToTemplate(args)} />`
    })
};

export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {};
