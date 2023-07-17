import { Meta, moduleMetadata, Story } from '@storybook/angular';
import { of } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';
import { DotAddStyleClassesDialogStore } from './store/add-style-classes-dialog.store';

import {
    DOT_MESSAGE_SERVICE_TB_MOCK,
    MOCK_SELECTED_STYLE_CLASSES,
    MOCK_STYLE_CLASSES_FILE
} from '../../utils/mocks';

export default {
    title: 'Template Builder/Components/Add Style Classes',
    component: AddStyleClassesDialogComponent,
    decorators: [
        moduleMetadata({
            imports: [
                AutoCompleteModule,
                FormsModule,
                ButtonModule,
                DotMessagePipe,
                NgIf,
                AsyncPipe,
                HttpClientModule,
                NoopAnimationsModule
            ],
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: {
                            selectedClasses: MOCK_SELECTED_STYLE_CLASSES
                        }
                    }
                },
                {
                    provide: HttpClient,
                    useValue: {
                        get: (_: string) => of(MOCK_STYLE_CLASSES_FILE)
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: DOT_MESSAGE_SERVICE_TB_MOCK
                },
                DynamicDialogRef,
                DotAddStyleClassesDialogStore
            ]
        })
    ]
} as Meta<AddStyleClassesDialogComponent>;

const Template: Story<AddStyleClassesDialogComponent> = (args: AddStyleClassesDialogComponent) => ({
    props: args
});

export const Primary = Template.bind({});
