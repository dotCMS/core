/* eslint-disable no-console */
import { moduleMetadata, Meta, StoryObj, componentWrapperDecorator } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { TabsModule } from 'primeng/tabs';

import { DotMessageService } from '@dotcms/data-access';
import { DotApiLinkComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPortletBaseComponent } from './dot-portlet-base.component';

const MessageMocks = new MockDotMessageService({
    cancel: 'Cancel',
    actions: 'Actions'
});

const meta: Meta = {
    title: 'DotCMS/Portlet',
    component: DotPortletBaseComponent,
    decorators: [
        moduleMetadata({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: MessageMocks
                }
            ],
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                CheckboxModule,
                DotPortletBaseComponent,
                DotApiLinkComponent,
                TabsModule
            ]
        }),
        componentWrapperDecorator(
            (story) =>
                `<div style="background-color: #f1f3f4; width: 100%; overflow: hidden; border: solid 1px #ccc">${story}</div>`
        )
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'DotCMS portlets have the same spacing and toolbar arragement, to create a new porlet you have to use the <dot-portlet-base> and <dot-portlet-toolbar> components togheter.'
            },
            iframeHeight: 800
        }
    }
};
export default meta;

type Story = StoryObj;

const portletContent = (text = `Hello, I'm the portlet content`) => {
    return `
    <div style="width: 100%; height: 600px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
        <h2>${text}</h2>
    </div>`;
};

const NoActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title"></dot-portlet-toolbar>
    ${portletContent()}
</dot-portlet-base>
`;
export const NoActions: Story = {
    parameters: {
        docs: {
            source: {
                code: NoActionsTemplate
            }
        }
    },
    args: {
        title: 'This is the portlet title'
    },
    render: (args) => ({
        props: args,
        template: NoActionsTemplate
    })
};

const BasicActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions"></dot-portlet-toolbar>
    ${portletContent()}
</dot-portlet-base>
`;
export const BasicActions: Story = {
    parameters: {
        docs: {
            source: {
                code: BasicActionsTemplate
            }
        }
    },
    args: {
        title: 'Adding Save and Cancel Button',
        portletActions: {
            primary: [
                {
                    label: 'Save',
                    command: (e) => {
                        console.log(e);
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        }
    },
    render: (args) => ({
        props: args,
        template: NoActionsTemplate
    })
};

const MultipleActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions"></dot-portlet-toolbar>
    ${portletContent()}
</dot-portlet-base>
`;

export const MultipleActions: Story = {
    parameters: {
        docs: {
            source: {
                code: MultipleActionsTemplate
            }
        }
    },
    args: {
        title: 'Multiple Actions',
        portletActions: {
            primary: [
                {
                    label: 'Publish',
                    command: (e) => {
                        console.log(e);
                    }
                },
                {
                    label: 'Lock',
                    command: (e) => {
                        console.log(e);
                    }
                },
                {
                    label: 'Execute',
                    command: (e) => {
                        console.log(e);
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        }
    },
    render: (args) => ({
        props: args,
        template: MultipleActionsTemplate
    })
};

const ExtraActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions">
    <ng-container left>
        <dot-api-link href="#"></dot-api-link>
        <p-checkbox label="Some stuff"></p-checkbox>
    </ng-container>
    <ng-container right>
        <button pButton label="Another action" class="p-button-secondary"></button>
        <p-checkbox label="Whatever"></p-checkbox>
    </ng-container>
    </dot-portlet-toolbar>
    ${portletContent()}
</dot-portlet-base>
`;
export const ExtraActions: Story = {
    parameters: {
        docs: {
            source: {
                code: ExtraActionsTemplate
            }
        }
    },
    args: {
        title: 'Extra Actions',
        portletActions: {
            primary: [
                {
                    label: 'Action',
                    command: (e) => {
                        console.log(e);
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        }
    },
    render: (args) => ({
        props: args,
        template: ExtraActionsTemplate
    })
};

const WithTabsTemplate = `
<dot-portlet-base [boxed]="false">
    <dot-portlet-toolbar [title]="title"></dot-portlet-toolbar>
    <p-tabs>
        <p-tab>
            <ng-template pTemplate="header">Tab 1</ng-template>
            ${portletContent('Content for Tab 1')}
        </p-tab>
        <p-tab>
            <ng-template pTemplate="header">Tab 2</ng-template>
            ${portletContent('Content for Tab 2')}
        </p-tab>
        <p-tab>
            <ng-template pTemplate="header">Tab 3</ng-template>
            ${portletContent('Content for Tab 3')}
        </p-tab>
    </p-tabs>
</dot-portlet-base>
`;
export const WithTabs: Story = {
    parameters: {
        docs: {
            source: {
                code: WithTabsTemplate
            }
        }
    },
    args: {
        title: 'Tabbed Portlet',
        portletActions: {
            primary: [
                {
                    label: 'Action',
                    command: (e) => {
                        console.log(e);
                    }
                }
            ],
            cancel: () => {
                console.log('cancel');
            }
        }
    },
    render: (args) => ({
        props: args,
        template: WithTabsTemplate
    })
};
