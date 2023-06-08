import { moduleMetadata, Story, Meta } from '@storybook/angular';

import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';

import { MOCK_STYLE_CLASSES_FILE } from '../../utils/mocks';

export default {
    title: 'Components/Add Style Classes Dialog',
    component: AddStyleClassesDialogComponent,
    decorators: [
        moduleMetadata({
            imports: [AutoCompleteModule, FormsModule, NoopAnimationsModule, ButtonModule],
            providers: [
                {
                    provide: DynamicDialogConfig,
                    useValue: {
                        data: MOCK_STYLE_CLASSES_FILE
                    }
                },
                DynamicDialogRef
            ]
        })
    ]
} as Meta<AddStyleClassesDialogComponent>;

const Template: Story<AddStyleClassesDialogComponent> = (args: AddStyleClassesDialogComponent) => ({
    props: args
});

export const Primary = Template.bind({});
