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
     * is in flight, then `true` / `false` once it returns. Per the project-wide rule, a missing
     * flag resolves to `true` (`getFeatureFlag`), so the new editor renders unless the flag is
     * explicitly `false`. The template's truthy check still keeps the legacy editor in-flight.
     */
    readonly isNewBlockEditorEnabled = toSignal(
        this.dotPropertiesService.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_BLOCK_EDITOR)
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
