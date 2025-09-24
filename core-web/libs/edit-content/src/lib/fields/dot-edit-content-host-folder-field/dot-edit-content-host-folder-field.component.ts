import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ReactiveFormsModule, FormsModule, ControlContainer } from '@angular/forms';

import { TreeSelectModule } from 'primeng/treeselect';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotHostFolderFieldComponent } from './components/host-folder-field/host-folder-field.component';
import { HostFolderFiledStore } from './store/host-folder-field.store';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

/**
 * Component for editing content site or folder field.
 *
 * @export
 * @class DotEditContentHostFolderFieldComponent
 */
@Component({
    selector: 'dot-edit-content-host-folder-field',
    imports: [
        TreeSelectModule,
        ReactiveFormsModule,
        FormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotHostFolderFieldComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-host-folder-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [HostFolderFiledStore]
})
export class DotEditContentHostFolderFieldComponent extends BaseWrapperField {
    /**
     * A signal that holds the field.
     * It is used to display the field in the component.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the component.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
}
