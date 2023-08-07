import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import { CONTAINER_MAP_MOCK, DOT_MESSAGE_SERVICE_TB_MOCK, ITEMS_MOCK } from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

export default {
    title: 'Library/Template Builder/Components/Box',
    component: TemplateBuilderBoxComponent,
    decorators: [
        moduleMetadata({
            imports: [
                ButtonModule,
                ScrollPanelModule,
                RemoveConfirmDialogComponent,
                BrowserAnimationsModule
            ],
            providers: [
                ConfirmationService,
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                {
                    provide: DotContainersService,
                    useValue: new DotContainersServiceMock()
                }
            ]
        })
    ]
} as Meta<TemplateBuilderBoxComponent>;

const Template: Story<TemplateBuilderBoxComponent> = (args: TemplateBuilderBoxComponent) => ({
    props: args
});

export const Small = Template.bind({});

export const Medium = Template.bind({});

export const Large = Template.bind({});

const containerMap = CONTAINER_MAP_MOCK;

Small.args = {
    width: 1,
    ITEMS: ITEMS_MOCK,
    containerMap
};
Medium.args = {
    width: 3,
    ITEMS: ITEMS_MOCK,
    containerMap
};
Large.args = {
    width: 10,
    ITEMS: ITEMS_MOCK,
    containerMap
};
