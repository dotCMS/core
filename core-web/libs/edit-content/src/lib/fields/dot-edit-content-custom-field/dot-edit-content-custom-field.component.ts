import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';

import { BridgeFieldComponent } from './components/bridge-field/bridge-field.components';
import { DotWCCompoment } from './components/wc-field/wc-field.compoment';

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
        DotIconModule,
        ButtonModule,
        InputTextModule,
        DialogModule,
        ReactiveFormsModule,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        BridgeFieldComponent,
        DotWCCompoment
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

    $isWC = computed(() => {
        const field = this.$field();
        if (!field) return false;

        return field.variable === 'mywc';
    });
}
