import {
    ChangeDetectionStrategy,
    Component,
    forwardRef,
    inject,
    input,
    signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectExistingContentComponent } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/components/dot-select-existing-content/dot-select-existing-content.component';
import { DotMessagePipe } from '@dotcms/ui';

import { RelationshipFieldStore } from './store/relationship-field.store';

@Component({
    selector: 'dot-edit-content-relationship-field',
    standalone: true,
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotSelectExistingContentComponent,
        DotMessagePipe,
        ChipModule
    ],
    providers: [
        RelationshipFieldStore,
        DialogService,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentRelationshipFieldComponent)
        }
    ],
    templateUrl: './dot-edit-content-relationship-field.component.html',
    styleUrls: ['./dot-edit-content-relationship-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentRelationshipFieldComponent implements ControlValueAccessor {
    /**
     * A readonly private field that injects the DotMessageService.
     * This service is used for handling message-related functionalities within the component.
     */
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * A signal that controls the visibility of the existing content dialog.
     * When true, the dialog is shown allowing users to select existing content.
     * When false, the dialog is hidden.
     */
    $showExistingContentDialog = signal(false);

    /**
     * A signal that holds the menu items for the relationship field.
     * These items control the visibility of the existing content dialog and the creation of new content.
     */
    $menuItems = signal<MenuItem[]>([
        {
            label: this.#dotMessageService.get(
                'dot.file.relationship.field.table.existing.content'
            ),
            command: () => {
                this.$showExistingContentDialog.update((value) => !value);
            }
        },
        {
            label: this.#dotMessageService.get('dot.file.relationship.field.table.new.content'),
            command: () => {
                // TODO: Implement new content
            }
        }
    ]);

    /**
     * A readonly instance of the RelationshipFieldStore injected into the component.
     * This store is used to manage the state and actions related to the relationship field.
     */
    readonly store = inject(RelationshipFieldStore);

    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Set the value of the field.
     * If the value is empty, nothing happens.
     * If the value is not empty, the store is called to get the asset data.
     *
     * @param value the value to set
     */
    writeValue(value: string): void {
        if (!value) {
            return;
        }
    }
    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     * This function is passed to the {@link NG_VALUE_ACCESSOR} token.
     *
     * @param fn The callback function to register.
     */
    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    /**
     * A callback function that is called when the value of the field changes.
     * It is used to update the value of the field in the parent component.
     */
    private onChange: ((value: string) => void) | null = null;

    /**
     * A callback function that is called when the field is touched.
     */
    private onTouched: (() => void) | null = null;
}
