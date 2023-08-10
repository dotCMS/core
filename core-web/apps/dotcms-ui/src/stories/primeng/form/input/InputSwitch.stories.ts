import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputSwitch, InputSwitchModule } from 'primeng/inputswitch';

import { InputSwitchTemplate } from './templates';

export default {
    title: 'PrimeNG/Form/InputSwitch',
    component: InputSwitch,
    parameters: {
        docs: {
            description: {
                component:
                    'InputSwitch is used to select a boolean value.: https://primefaces.org/primeng/showcase/#/inputswitch'
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
