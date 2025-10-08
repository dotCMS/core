import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from '../../store/edit-content.store';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-block-editor',
    imports: [
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotMessagePipe,
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
