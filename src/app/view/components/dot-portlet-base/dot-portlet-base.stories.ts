import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { moduleMetadata } from '@storybook/angular';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DotPortletBaseComponent } from './dot-portlet-base.component';
import { DotPortletBaseModule } from './dot-portlet-base.module';

const MessageMocks = new MockDotMessageService({
    cancel: 'Cancel',
    actions: 'Actions'
});

export default {
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
                DotPortletBaseModule,
                DotApiLinkModule
            ]
        }),
        (storyFunc) => {
            const story = storyFunc();

            return {
                ...story,
                template: `<div style="background-color: #f1f3f4; width: 100%; overflow: hidden; border: solid 1px #ccc">${story.template}</div>`
            };
        }
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

const portletContent = `
    <div style="width: 100%; height: 600px; display: flex; flex-direction: column; align-items: center; justify-content: center;">
        <h2>Hello, I'm the portlet content</h2>
    </div>`;

const NoActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title"></dot-portlet-toolbar>
    ${portletContent}
</dot-portlet-base>
`;
export const NoActions = () => ({
    props: {
        title: 'This is the portlet title'
    },
    template: NoActionsTemplate
});
NoActions.parameters = {
    docs: {
        source: {
            code: NoActionsTemplate
        }
    }
};

const BasicActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions"></dot-portlet-toolbar>
    ${portletContent}
</dot-portlet-base>
`;
export const BasicActions = () => ({
    props: {
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
    template: BasicActionsTemplate
});
BasicActions.parameters = {
    docs: {
        source: {
            code: BasicActionsTemplate
        }
    }
};

const MultipleActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions"></dot-portlet-toolbar>
    ${portletContent}
</dot-portlet-base>
`;
export const MultipleActions = () => ({
    props: {
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
    template: MultipleActionsTemplate
});
MultipleActions.parameters = {
    docs: {
        source: {
            code: MultipleActionsTemplate
        }
    }
};

const ExtraActionsTemplate = `
<dot-portlet-base>
    <dot-portlet-toolbar [title]="title" [actions]="portletActions">
    <ng-container id="left">
        <dot-api-link href="#"></dot-api-link>
        <p-checkbox label="Some stuff"></p-checkbox>
    </ng-container>
    <ng-container id="right">
        <button pButton label="Another action" class="p-button-secondary"></button>
        <p-checkbox label="Whatever"></p-checkbox>
    </ng-container>
    </dot-portlet-toolbar>
    ${portletContent}
</dot-portlet-base>
`;
export const ExtraActions = () => ({
    props: {
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
    template: ExtraActionsTemplate
});
ExtraActions.parameters = {
    docs: {
        source: {
            code: ExtraActionsTemplate
        }
    }
};
