import { Story, Meta } from '@storybook/angular/types-6-0';
import { DialogModule } from 'primeng/dialog';
import { moduleMetadata } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ButtonModule } from 'primeng/button';

export default {
    title: 'PrimeNG/Overlay/Dialog',
    parameters: {
        docs: {
            description: {
                component:
                    'Dialog is a container to display content in an overlay window: https://primefaces.org/primeng/showcase/#/dialog'
            }
        }
    },
    decorators: [
        moduleMetadata({
            imports: [DialogModule, ButtonModule, BrowserAnimationsModule]
        })
    ],
    args: {
        displayBasic: false,
        showBasicDialog(): void {
            this.displayBasic = true;
        }
    }
} as Meta;

const DialogTemplate = `
  <p-button (click)="showBasicDialog()" icon="pi pi-external-link" label="Show"></p-button>
  <p-dialog header="Header" [(visible)]="displayBasic" [style]="{width: '50vw'}" [baseZIndex]="10000">
    <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
        Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
        cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
    <ng-template pTemplate="footer">
        <p-button icon="pi pi-check" (click)="displayBasic=false" label="Yes" styleClass="p-button-text"></p-button>
        <p-button icon="pi pi-times" (click)="displayBasic=false" label="No"></p-button>
    </ng-template>
  </p-dialog>
`;

const Template: Story<any> = (props: any) => {
    const template = DialogTemplate;
    return {
        props,
        template
    };
};

export const Basic: Story = Template.bind({});

Basic.argTypes = {
    displayBasic: {
        name: 'displayBasic',
        description: 'display the modal'
    }
};

Basic.parameters = {
    docs: {
        source: {
            code: DialogTemplate
        },
        iframeHeight: 300
    }
};
