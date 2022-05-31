// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { DropdownModule, Dropdown } from 'primeng/dropdown';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

export default {
    title: 'PrimeNG/Form/Dropdown',
    component: Dropdown,
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component:
                    'Dropdown is used to select an item from a list of options: https://primefaces.org/primeng/showcase/#/dropdown'
            }
        }
    },
    argTypes: {
        width: {
            name: 'width',
            type: { name: 'string', required: true },
            defaultValue: '300',
            description:
                "Setting a width prevents the dropdown from jumping when an option is larger than the dropdown's width",
            control: {
                type: 'text'
            }
        }
    },
    args: {
        options: [
            { label: 'Select City', value: null },
            { label: 'New York', value: { id: 1, name: 'New York', code: 'NY' } },
            { label: 'Rome', value: { id: 2, name: 'Rome', code: 'RM' } },
            { label: 'London', value: { id: 3, name: 'London', code: 'LDN' } },
            { label: 'Istanbul', value: { id: 4, name: 'Istanbul', code: 'IST' } },
            { label: 'Paris', value: { id: 5, name: 'Paris', code: 'PRS' } }
        ],
        width: '300'
    }
} as Meta;

const DropdownTemplate = `
    <p><p-dropdown [options]="options" showClear="true" [style]="{'width': width + 'px'}"></p-dropdown></p>
    <p><p-dropdown [options]="options" disabled="true" [style]="{'width': width + 'px'}"></p-dropdown></p>
    <hr />
    <p><p-dropdown class="p-dropdown-sm" [options]="options" [style]="{'width': width + 'px'}"></p-dropdown></p>
`;
const Template: Story<Dropdown> = (args: Dropdown) => {
    return {
        props: args,
        moduleMetadata: {
            imports: [DropdownModule, BrowserAnimationsModule]
        },
        template: DropdownTemplate
    };
};

export const Primary: Story = Template.bind({});
Primary.parameters = {
    docs: {
        source: {
            code: DropdownTemplate
        },
        iframeHeight: 300
    }
};
