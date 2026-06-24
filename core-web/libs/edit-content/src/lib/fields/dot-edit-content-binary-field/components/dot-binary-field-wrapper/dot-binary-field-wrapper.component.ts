import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    input,
    OnInit,
    output
} from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCardFieldContentComponent } from '../../../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../../../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../../../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../../../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../../../shared/base-wrapper-field';
import { DotEditContentBinaryFieldComponent } from '../../dot-edit-content-binary-field.component';
import { DotLegacyImageEditorLauncherService } from '../../service/dot-legacy-image-editor/dot-legacy-image-editor-launcher.service';

/**
 * Wrapper for binary fields in the content editor card layout.
 *
 * Enables the legacy image editor by wiring {@link DotLegacyImageEditorLauncherService}
 * to the binary field web component lifecycle.
 */
@Component({
    selector: 'dot-binary-field-wrapper',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe,
        DotEditContentBinaryFieldComponent
    ],
    providers: [DialogService, DotLegacyImageEditorLauncherService],
    templateUrl: './dot-binary-field-wrapper.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true, optional: true })
        }
    ]
})
export class DotBinaryFieldWrapperComponent extends BaseWrapperField implements OnInit {
    readonly #legacyImageEditorLauncher = inject(DotLegacyImageEditorLauncherService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * A signal that holds the field.
     * It is used to display the field in the binary field wrapper component.
     */
    $field = input.required<DotCMSContentTypeField>({
        alias: 'field'
    });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the binary field wrapper component.
     */
    $contentlet = input.required<DotCMSContentlet>({
        alias: 'contentlet'
    });
    /**
     * An output signal that emits when the value is updated.
     * It is used to display the value in the binary field wrapper component.
     */
    valueUpdated = output<{ value: string; fileName: string }>();

    /**
     * Starts listening for legacy image editor events for this field and cleans up on destroy.
     */
    ngOnInit(): void {
        this.#legacyImageEditorLauncher.listen(this.$field().variable);

        this.#destroyRef.onDestroy(() => {
            this.#legacyImageEditorLauncher.stopListening();
        });
    }
}
