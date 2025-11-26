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
    styles: string;
};

const DEFAULT = 'default';

const mergeArgsClassNamesToString = (args): string => {
    const size = args.size ?? '';
    const severity = args.severity ?? '';
    const styles = args.styles ?? '';

    const classes = [args.styleClass];

    if (size !== DEFAULT) {
        classes.push(size);
    }

    if (severity !== DEFAULT) {
        classes.push(severity);
    }

    if (styles !== DEFAULT) {
        classes.push(styles);
    }

    return classes.join(' ');
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
        size: DEFAULT,
        severity: DEFAULT,
        styles: DEFAULT,
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
        styles: {
            options: [DEFAULT, 'p-chip-outlined', 'p-chip-filled', 'p-chip-dashed'],
            control: { type: 'radio' }
        },
        size: {
            options: [DEFAULT, 'p-chip-sm', 'p-chip-lg'],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the size of the chip'
        },
        severity: {
            options: [
                DEFAULT,
                'p-chip-primary',
                'p-chip-blue',
                'p-chip-secondary',
                'p-chip-warning',
                'p-chip-success',
                'p-chip-error',
                'p-chip-pink',
                'p-chip-gray',
                'p-chip-white'
            ],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the severity of the chip'
        }
    },
    render: (args) => {
        const newArgs = { ...args };
        delete newArgs.size;
        delete newArgs.severity;

        return {
            props: {
                ...args,
                styleClass: mergeArgsClassNamesToString(args)
            },
            template: `
            <p-chip ${argsToTemplate(newArgs)} >
                <ng-template pTemplate="removeicon">
                    <i class="pi pi-times"></i>
                </ng-template>
            </p-chip>`
        };
    }
};

export default meta;

type Story = StoryObj<Args>;

export const Default: Story = {};
