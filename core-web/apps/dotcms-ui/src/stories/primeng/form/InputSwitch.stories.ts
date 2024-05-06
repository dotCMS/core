import { Meta, moduleMetadata, Story } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';

export default {
    title: 'PrimeNG/Form/InputSwitch',
    component: InputSwitch,
    parameters: {
        docs: {
            description: {
                component:
                    'InputSwitch is used to select a boolean value.: https://primeng.org/inputswitch'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputSwitchModule, BrowserAnimationsModule]
        })
    ],
    args: {
        checked: false
    }
} as Meta;

const InputSwitchTemplate = `<p-inputSwitch [(ngModel)]="checked"></p-inputSwitch>`;

const Template: Story<InputSwitch> = (props: InputSwitch) => {
    const template = InputSwitchTemplate;

    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.argTypes = {
    checked: {
        name: 'checked',
        description: 'Boolean'
    }
};

Basic.parameters = {
    docs: {
        source: {
            code: InputSwitchTemplate
        }
    }
};
