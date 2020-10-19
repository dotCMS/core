import { Meta, Story } from '@storybook/angular/types-6-0';
import { moduleMetadata } from '@storybook/angular';
import { ButtonModule } from 'primeng/button';

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
                component:
                    'All the buttons, more information: https://primefaces.org/primeng/showcase/#/button'
            }
        }
    }
} as Meta;

const PrimaryTemplate = `
    <p><button pButton label="Submit"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check" iconPos="right"></button></p>
    <p><button pButton label="Disabled" disabled="true"></button></p>
    <hr />
    <p><button pButton label="Small Button" class="p-button-sm"></button></p>
    <p><button pButton label="Big Button" class="p-button-lg"></button></p>
`;

const SecondaryTemplate = `
    <p><button pButton label="Submit" class="p-button-secondary"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check" class="p-button-secondary"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check" iconPos="right" class="p-button-secondary"></button></p>
    <p><button pButton label="Disabled" disabled="true" class="p-button-secondary"></button></p>
    <hr />
    <p><button pButton label="Small Button" class="p-button-secondary p-button-sm"></button></p>
    <p><button pButton label="Big Button" class="p-button-secondary p-button-lg"></button></p>
`;

const TextTemplate = `
    <p><button pButton label="Submit" class="p-button-text"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check" class="p-button-text"></button></p>
    <p><button pButton label="Submit" icon="pi pi-check" iconPos="right" class="p-button-text"></button></p>
    <p><button pButton label="Disabled" disabled="true" class="p-button-text"></button></p>
    <hr />
    <p><button pButton label="Small Button" class="p-button-text p-button-sm"></button></p>
    <p><button pButton label="Big Button" class="p-button-text p-button-lg"></button></p>
`;

export const Primary: Story = () => {
    return {
        template: PrimaryTemplate
    };
};

Primary.parameters = {
    docs: {
        source: {
            code: PrimaryTemplate
        }
    }
};

export const Secondary: Story = () => {
    return {
        template: SecondaryTemplate
    };
};
Secondary.parameters = {
    docs: {
        source: {
            code: SecondaryTemplate
        }
    }
};

const IconsTemplate = `
    <p><button pButton type="button" icon="pi pi-check" class="p-button-rounded p-button-text"></button></p>
    <p><button pButton type="button" icon="pi pi-sitemap" class="p-button-rounded p-button-text"></button></p>
    <p><button pButton type="button" disabled="true" icon="pi pi-shopping-cart" class="p-button-rounded p-button-text"></button></p>
`;
export const Icons: Story = () => {
    return {
        template: IconsTemplate
    };
};
Icons.parameters = {
    docs: {
        source: {
            code: IconsTemplate
        }
    }
};

export const Text: Story = () => {
    return {
        template: TextTemplate
    };
};

Text.parameters = {
    docs: {
        source: {
            code: TextTemplate
        }
    }
};
