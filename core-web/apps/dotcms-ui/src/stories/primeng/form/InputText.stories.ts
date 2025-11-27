import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { InputText, InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

const InputTextTemplate = `<div class="flex flex-col gap-4 mb-2">
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username">Username</label>
    <input id="username" pInputText aria-describedby="username-help" placeholder="Placeholder" autocomplete="off" />
    <small id="username-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-icon-right">Username</label>
    <span class="p-input-icon-right">
        <i class="pi pi-times"></i>
        <input id="username-icon-right" pInputText aria-describedby="username-icon-right-help" placeholder="Placeholder" autocomplete="off" style="width:100%;" />
    </span>
    <small id="username-icon-right-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-icon-right-double">Username</label>
    <span class="p-input-icon-right">
        <i class="pi pi-times"></i>
        <i class="pi pi-search"></i>
        <input id="username-icon-right-double" pInputText aria-describedby="username-icon-right-double-help" placeholder="Placeholder" autocomplete="off" style="width:100%;" />
    </span>
    <small id="username-icon-right-double-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-error">Username</label>
    <input
        class="ng-invalid ng-dirty"
        id="username-error"
        pInputText
        aria-describedby="username-help-error"
        placeholder="Placeholder"
        autocomplete="off"
    />
    <small id="username-help-error">Please enter a valid username</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-disabled">Disabled</label>
    <input
        id="username-disabled"
        pInputText
        aria-describedby="username-help-disabled"
        disabled
        placeholder="Disabled"
        autocomplete="off"
    />
</div>
</div>
<h4>Small</h4>
<div class="flex flex-col gap-4 mb-1">
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username">Username</label>
    <input class="p-inputtext-sm" id="username" pInputText aria-describedby="username-help" placeholder="Placeholder" autocomplete="off" />
    <small id="username-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-icon-right">Username</label>
    <span class="p-input-icon-right">
        <i class="pi pi-times"></i>
        <input id="username-icon-right" class="p-inputtext-sm" pInputText aria-describedby="username-icon-right-help" placeholder="Placeholder" autocomplete="off" style="width:100%;" />
    </span>
    <small id="username-icon-right-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-icon-right-double">Username</label>
    <span class="p-input-icon-right">
        <i class="pi pi-times"></i>
        <i class="pi pi-search"></i>
        <input id="username-icon-right-double" class="p-inputtext-sm" pInputText aria-describedby="username-icon-right-double-help" placeholder="Placeholder" autocomplete="off" style="width:100%;" />
    </span>
    <small id="username-icon-right-double-help">Enter your username to reset your password.</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-error">Username</label>
    <input
        class="ng-invalid ng-dirty p-inputtext-sm"
        id="username-error"
        pInputText
        aria-describedby="username-help-error"
        placeholder="Placeholder"
        autocomplete="off"
    />
    <small id="username-help-error">Please enter a valid username</small>
</div>
<div class="flex flex-col gap-2" style="width:200px;">
    <label htmlFor="username-disabled">Disabled</label>
    <input
        class="p-inputtext-sm"
        id="username-disabled"
        pInputText
        aria-describedby="username-help-disabled"
        disabled
        placeholder="Disabled"
        autocomplete="off"
    />
</div>
</div>
`;

const meta: Meta = {
    title: 'PrimeNG/Form/InputText/Default',
    component: InputText,
    parameters: {
        docs: {
            description: {
                component:
                    'InputText renders a text field to enter data.: https://primeng.org/inputtext'
            },
            source: {
                code: InputTextTemplate
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [InputTextModule, BrowserAnimationsModule, PasswordModule]
        })
    ],
    args: {
        checked: false
    },
    render: (args) => ({
        props: args,
        template: InputTextTemplate
    })
};
export default meta;

type Story = StoryObj;

export const Basic: Story = {};
