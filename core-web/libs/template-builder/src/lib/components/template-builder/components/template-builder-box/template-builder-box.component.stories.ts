import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import {
    BANNER_CONTAINER_IDENTIFIER,
    CONTAINER_MAP_MOCK,
    DEFAULT_CONTAINER_IDENTIFIER,
    DOT_MESSAGE_SERVICE_TB_MOCK
} from '../../utils/mocks';
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

const DEFAULT_ITEM = { identifier: DEFAULT_CONTAINER_IDENTIFIER };
const BANNER_ITEM = { identifier: BANNER_CONTAINER_IDENTIFIER };

const items = [DEFAULT_ITEM, BANNER_ITEM, DEFAULT_ITEM, DEFAULT_ITEM, DEFAULT_ITEM, DEFAULT_ITEM];

export const Small = Template.bind({});

export const Medium = Template.bind({});

export const Large = Template.bind({});

const containerMap = CONTAINER_MAP_MOCK;

Small.args = {
    width: 1,
    items,
    containerMap
};
Medium.args = {
    width: 3,
    items,
    containerMap
};
Large.args = {
    width: 10,
    items,
    containerMap
};
