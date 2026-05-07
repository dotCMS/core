import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotPropertiesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotCMSEditorComponent } from '@dotcms/new-block-editor';

import { DotEditContentStore } from '../../store/edit-content.store';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-block-editor',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldLabelComponent,

        DotCMSEditorComponent,
        BlockEditorModule
    ],
    templateUrl: './dot-edit-content-block-editor.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true, optional: true })
        }
    ]
})
export class DotEditContentBlockEditorComponent extends BaseWrapperField {
    /**
     * The store instance.
     * It is used to get the current language ID.
     */
    private readonly $store = inject(DotEditContentStore);

    private readonly dotPropertiesService = inject(DotPropertiesService);

    /**
     * Resolves the `FEATURE_FLAG_NEW_BLOCK_EDITOR` flag — `undefined` while the HTTP request
     * is in flight, then `true` / `false` once it returns. The template uses a truthy check
     * so the legacy editor renders for **everything except an explicit `true`** (false, missing
     * key, in-flight). That defaults to the safer, known-good editor whenever the flag's
     * answer isn't yet a definite "on".
     */
    readonly isNewBlockEditorEnabled = toSignal(
        this.dotPropertiesService.getFeatureFlagWithDefault(
            FeaturedFlags.FEATURE_FLAG_NEW_BLOCK_EDITOR,
            false
        )
    );

    /**
     * A signal that holds the field.
     * It is used to display the field in the block editor component.
     */
    $field = input.required<DotCMSContentTypeField>({
        alias: 'field'
    });
    /**
     * A signal that holds the contentlet.
     * It is used to display the contentlet in the block editor component.
     */
    $contentlet = input.required<DotCMSContentlet>({
        alias: 'contentlet'
    });

    /**
     * A signal that holds the current language ID.
     * It is used to display the language ID in the block editor component.
     */
    readonly $languageId = computed(() => this.$store.currentLocale()?.id ?? 1);
}
