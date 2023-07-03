import { moduleMetadata } from '@storybook/angular';
import { Meta, Story } from '@storybook/angular/types-6-0';

import { ButtonModule } from 'primeng/button';

import {
    BasicTemplate,
    IconOnlyBasicTemplate,
    IconOnlyOutlinedTemplate,
    IconOnlyTextTemplate,
    OutlinedTemplate,
    TextTemplate
} from '../../utils/button';

export default {
    title: 'PrimeNG/Button/Button',
    decorators: [
        moduleMetadata({
            imports: [ButtonModule]
        })
    ],
    parameters: {
        layout: 'centered',
        docs: {
            description: {
                component: 'All the buttons, more information: https://primeng.org/button'
            }
        }
    }
} as Meta;

export const MainBasic: Story = () => {
    return {
        template: BasicTemplate
    };
};

MainBasic.parameters = {
    docs: {
        source: {
            code: BasicTemplate
        }
    }
};

export const MainOutlined: Story = () => {
    return {
        template: OutlinedTemplate
    };
};

MainOutlined.parameters = {
    docs: {
        source: {
            code: OutlinedTemplate
        }
    }
};

export const MainText: Story = () => {
    return {
        template: TextTemplate
    };
};

MainText.parameters = {
    docs: {
        source: {
            code: TextTemplate
        }
    }
};

export const IconOnlyBasic: Story = () => {
    return {
        template: IconOnlyBasicTemplate
    };
};

IconOnlyBasic.parameters = {
    docs: {
        source: {
            code: IconOnlyBasicTemplate
        }
    }
};

export const IconOnlyOutlined: Story = () => {
    return {
        template: IconOnlyOutlinedTemplate
    };
};

IconOnlyOutlined.parameters = {
    docs: {
        source: {
            code: IconOnlyOutlinedTemplate
        }
    }
};

export const IconOnlyText: Story = () => {
    return {
        template: IconOnlyTextTemplate
    };
};

IconOnlyText.parameters = {
    docs: {
        source: {
            code: IconOnlyTextTemplate
        }
    }
};
