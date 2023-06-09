import { moduleMetadata, Story, Meta } from '@storybook/angular';
import { of } from 'rxjs';

import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';

import { MOCK_SELECTED_STYLE_CLASSES, MOCK_STYLE_CLASSES_FILE } from '../../utils/mocks';

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
                        data: {
                            classes: of(
                                MOCK_STYLE_CLASSES_FILE.classes.map((klass) => ({ klass }))
                            ),
                            selectedClasses: MOCK_SELECTED_STYLE_CLASSES
                        }
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
