import { Meta, Story } from '@storybook/angular';

import { Chip, ChipModule } from 'primeng/chip';

const DEFAULT = 'default';
const mergeArgsClassNamesToString = (args): string => {
    const argsContainClassNames = ['style', 'size', 'severity'];

    let classes = '';

    argsContainClassNames.forEach((arg) => {
        if (args[arg] && args[arg] !== DEFAULT) {
            classes += args[arg] + ' ';
        }
    });

    return classes;
};

export default {
    title: 'PrimeNG/Chip',
    component: Chip,
    args: {
        label: 'Text',
        icon: 'pi pi-image',
        size: DEFAULT,
        severity: DEFAULT,
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
            options: [DEFAULT, 'p-chip-sm'],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the size of the chip'
        },
        severity: {
            options: [
                DEFAULT,
                'p-chip-secondary',
                'p-chip-warning',
                'p-chip-success',
                'p-chip-error',
                'p-chip-gray'
            ],
            control: { type: 'radio' },
            description: 'Class name used in `styleClass` for the severity of the chip'
        }
    }
} as Meta;

const ComponentStory: Story = (args) => ({
    moduleMetadata: {
        imports: [ChipModule]
    },
    props: {
        ...args,
        styleClass: mergeArgsClassNamesToString(args)
    },
    template: `
   <p-chip
    [label]="label"
    [icon]="icon"
    [styleClass]="styleClass"
    [removable]="removable"
    [image]="image"></p-chip>`
});

export const Default = ComponentStory.bind({});
