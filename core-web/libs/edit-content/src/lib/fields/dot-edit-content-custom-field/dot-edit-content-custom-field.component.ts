import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotRenderModes,
    NEW_RENDER_MODE_VARIABLE_KEY
} from '@dotcms/dotcms-models';

import { IframeFieldComponent } from './components/iframe-field/iframe-field.component';
import { NativeFieldComponent } from './components/native-field/native-field.component';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

/**
 * This component is used to render a custom field in the DotCMS content editor.
 * It uses an iframe to render the custom field and provides a form bridge to communicate with the custom field.
 */
@Component({
    selector: 'dot-edit-content-custom-field',
    imports: [
        ButtonModule,
        InputTextModule,
        DialogModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        IframeFieldComponent,
        NativeFieldComponent
    ],
    templateUrl: './dot-edit-content-custom-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCustomFieldComponent extends BaseWrapperField {
    /**
     * The field to render.
     */
    $field = input<DotCMSContentTypeField>(null, { alias: 'field' });
    /**
     * The content type to render the field for.
     */
    $contentType = input<string>(null, { alias: 'contentType' });
    /**
     * The contentlet to render the field for.
     */
    $contentlet = input<DotCMSContentlet>(null, { alias: 'contentlet' });
    /**
     * The render mode to use.
     */
    $renderMode = computed(() => {
        const field = this.$field();
        if (!field) return DotRenderModes.IFRAME;

        const renderMode = field.fieldVariables?.find(
            (variable) => variable.key === NEW_RENDER_MODE_VARIABLE_KEY
        )?.value;
        return renderMode || DotRenderModes.IFRAME;
    });
    /**
     * Whether the render mode is IFRAME.
     */
    $isIframeStrategy = computed(() => this.$renderMode() === DotRenderModes.IFRAME);
}
