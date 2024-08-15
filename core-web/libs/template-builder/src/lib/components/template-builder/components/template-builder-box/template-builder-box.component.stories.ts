import { moduleMetadata, StoryObj, Meta } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ScrollPanelModule } from 'primeng/scrollpanel';

import { DotContainersService, DotMessageService } from '@dotcms/data-access';
import { DotContainersServiceMock } from '@dotcms/utils-testing';

import { TemplateBuilderBoxComponent } from './template-builder-box.component';

import { CONTAINER_MAP_MOCK, DOT_MESSAGE_SERVICE_TB_MOCK, ITEMS_MOCK } from '../../utils/mocks';
import { RemoveConfirmDialogComponent } from '../remove-confirm-dialog/remove-confirm-dialog.component';

const meta: Meta<TemplateBuilderBoxComponent> = {
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
};
export default meta;

type Story = StoryObj<TemplateBuilderBoxComponent>;

const containerMap = CONTAINER_MAP_MOCK;

export const Small: Story = {
    args: {
        width: 1,
        items: ITEMS_MOCK,
        containerMap
    }
};

export const Medium: Story = {
    args: {
        width: 3,
        items: ITEMS_MOCK,
        containerMap
    }
};

export const Large: Story = {
    args: {
        width: 10,
        items: ITEMS_MOCK,
        containerMap
    }
};
