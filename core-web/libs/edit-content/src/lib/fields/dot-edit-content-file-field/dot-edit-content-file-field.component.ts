import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFileFieldComponent } from './components/dot-file-field/dot-file-field.component';
import { IMAGE_EDITOR_LAUNCHER, LegacyDialogImageEditorLauncher } from './services/image-editor';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-file-field',
    imports: [
        DotMessagePipe,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotFileFieldComponent,
        DotMessagePipe,
        ReactiveFormsModule
    ],
    providers: [
        DotFileFieldUploadService,
        FileFieldStore,
        DialogService,
        // The new editor embeds the legacy image editor JSP in a dialog iframe
        // (the global Dojo "open-image-editor" listener only exists in the old
        // editor). Show "Edit image" for any field whose file is actually an
        // image (gated by $canEditImage).
        { provide: IMAGE_EDITOR_LAUNCHER, useClass: LegacyDialogImageEditorLauncher }
    ],
    templateUrl: './dot-edit-content-file-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentFileFieldComponent extends BaseWrapperField {
    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * DotCMS Contentlet
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * Emits when the field value changes due to a user action. Bubbled from the
     * inner file field so the parent can sync FileAsset title/fileName.
     */
    valueUpdated = output<{ value: string; fileName: string }>();
}
