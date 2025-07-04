import { EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    forwardRef,
    inject,
    input,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { TableRowReorderEvent, TableModule } from 'primeng/table';

import { catchError, filter } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { ContentletStatusPipe } from '@dotcms/edit-content/pipes/contentlet-status.pipe';
import { LanguagePipe } from '@dotcms/edit-content/pipes/language.pipe';
import { DotMessagePipe } from '@dotcms/ui';

import { FooterComponent } from './components/dot-select-existing-content/components/footer/footer.component';
import { HeaderComponent } from './components/dot-select-existing-content/components/header/header.component';
import { DotSelectExistingContentComponent } from './components/dot-select-existing-content/dot-select-existing-content.component';
import { PaginationComponent } from './components/pagination/pagination.component';
import { RelationshipFieldStore } from './store/relationship-field.store';
import { getContentTypeIdFromRelationship } from './utils';

import { DotEditContentDialogComponent } from '../../components/dot-create-content-dialog/dot-create-content-dialog.component';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';

@Component({
    selector: 'dot-edit-content-relationship-field',
    standalone: true,
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        ChipModule,
        ContentletStatusPipe,
        LanguagePipe,
        PaginationComponent
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
     * A readonly instance of the RelationshipFieldStore injected into the component.
     * This store is used to manage the state and actions related to the relationship field.
     */
    readonly store = inject(RelationshipFieldStore);
    /**
     * A readonly private field that injects the DotMessageService.
     * This service is used for handling message-related functionalities within the component.
     */
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * A readonly private field that holds a reference to the `DestroyRef` service.
     * This service is injected into the component to manage the destruction lifecycle.
     */
    readonly #destroyRef = inject(DestroyRef);

    /**
     * A readonly private field that holds an instance of the DialogService.
     * This service is injected using Angular's dependency injection mechanism.
     * It is used to manage dialog interactions within the component.
     */
    readonly #dialogService = inject(DialogService);

    /**
     * A readonly private field that injects the DotContentTypeService.
     * This service is used for retrieving content type information and metadata.
     */
    readonly #dotContentTypeService = inject(DotContentTypeService);

    /**
     * A readonly private field that injects the DotHttpErrorManagerService.
     * This service is used for handling HTTP errors in a consistent manner.
     */
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Signal that tracks whether the component is disabled.
     * This is used to disable all interactive elements in the component.
     */
    readonly $isDisabled = signal(false);

    /**
     * Reference to the dynamic dialog. It can be null if no dialog is currently open.
     *
     * @type {DynamicDialogRef | null}
     */
    #dialogRef: DynamicDialogRef | null = null;

    /**
     * A signal that holds the menu items for the relationship field.
     * These items control the visibility of the existing content dialog and the creation of new content.
     */
    $menuItems = computed<MenuItem[]>(() => {
        const isDisabledCreateNewContent = this.store.isDisabledCreateNewContent();

        return [
            {
                label: this.#dotMessageService.get(
                    'dot.file.relationship.field.table.existing.content'
                ),
                disabled: isDisabledCreateNewContent || this.$isDisabled(),
                command: () => {
                    this.showExistingContentDialog();
                }
            },
            {
                label: this.#dotMessageService.get('dot.file.relationship.field.table.new.content'),
                disabled: isDisabledCreateNewContent || this.$isDisabled(),
                command: () => {
                    this.showCreateNewContentDialog();
                }
            }
        ];
    });

    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * DotCMS Contentlet
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    /**
     * Creates an instance of DotEditContentRelationshipFieldComponent.
     * It sets the cardinality of the relationship field based on the field's cardinality.
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    constructor() {
        effect(
            () => {
                const field = this.$field();
                const contentlet = this.$contentlet();

                const cardinality = field?.relationships?.cardinality ?? null;

                if (cardinality === null || !field?.variable) {
                    return;
                }

                this.store.initialize({
                    cardinality,
                    contentlet,
                    variable: field?.variable
                });
            },
            {
                allowSignalWrites: true
            }
        );

        effect(() => {
            if (this.onChange && this.onTouched) {
                const value = this.store.formattedRelationship();
                this.onChange(value);
                this.onTouched();
            }
        });
    }

    /**
     * A computed signal that holds the attributes for the relationship field.
     * This attributes are used to get the content type fields.
     */
    $attributes = computed(() => {
        const field = this.$field();

        return {
            contentTypeId: getContentTypeIdFromRelationship(field),
            hitText: field?.hint || null
        };
    });

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

    /**
     * Sets the disabled state of the component.
     * This method is called by Angular when the form control's disabled state changes.
     *
     * @param isDisabled Whether the component should be disabled
     */
    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Deletes an item from the store at the specified index.
     *
     * @param index - The index of the item to delete.
     */
    deleteItem(inode: string) {
        if (this.$isDisabled()) {
            return;
        }

        this.store.deleteItem(inode);
    }

    /**
     * Shows the existing content dialog.
     */
    showExistingContentDialog() {
        if (this.$isDisabled()) {
            return;
        }

        this.#dialogRef = this.#dialogService.open(DotSelectExistingContentComponent, {
            appendTo: 'body',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: false,
            position: 'center',
            width: '90%',
            height: '90%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field',
            style: { 'max-width': '1040px', 'max-height': '800px' },
            data: {
                contentTypeId: this.$attributes().contentTypeId,
                selectionMode: this.store.selectionMode(),
                currentItemsIds: this.store.data().map((item) => item.inode)
            },
            templates: {
                header: HeaderComponent,
                footer: FooterComponent
            }
        });

        this.#dialogRef.onClose
            .pipe(
                filter((items) => !!items),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((items: DotCMSContentlet[]) => {
                this.store.setData(items);
            });
    }

    /**
     * Shows the create new content dialog based on the relationship content type.
     * It checks the CONTENT_EDITOR2_ENABLED flag in the content type metadata to determine
     * whether to use the new Angular-based content editor or the legacy iframe-based editor.
     */
    showCreateNewContentDialog() {
        if (this.$isDisabled()) {
            return;
        }

        const contentTypeId = this.$attributes().contentTypeId;

        // Get content type information to check the CONTENT_EDITOR2_ENABLED flag
        this.#dotContentTypeService
            .getContentType(contentTypeId)
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                catchError((error) => {
                    this.#dotHttpErrorManagerService.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentType) => {
                const isNewEditorEnabled =
                    contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] ===
                    true;

                if (isNewEditorEnabled) {
                    this.openNewContentDialog(contentType);
                } else {
                    this.openLegacyContentDialog(contentType);
                }
            });
    }

    /**
     * Reorders the data in the store.
     * @param {TableRowReorderEvent} event - The event containing the drag and drop indices.
     */
    onRowReorder(event: TableRowReorderEvent) {
        if (this.$isDisabled() || event?.dragIndex == null || event?.dropIndex == null) {
            return;
        }

        this.store.setData(this.store.data());
    }

    /**
     * Opens the new content dialog for creating content using the Angular editor
     *
     * @private
     * @param {DotCMSContentType} _contentType - The content type to create content for
     */
    private openNewContentDialog(_contentType: DotCMSContentType): void {
        const dialogData: EditContentDialogData = {
            mode: 'new',
            contentTypeId: _contentType.id,
            relationshipInfo: {
                parentContentletId: this.$contentlet()?.inode,
                relationshipName: this.$field()?.variable,
                isParent: true // This could be determined based on relationship configuration
            },
            onContentSaved: (contentlet: DotCMSContentlet) => {
                // Add the created contentlet to the relationship
                const currentData = this.store.data();
                this.store.setData([...currentData, contentlet]);
            }
        };

        this.#dialogRef = this.#dialogService.open(DotEditContentDialogComponent, {
            appendTo: 'body',
            closeOnEscape: true,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: true,
            position: 'center',
            width: '95%',
            height: '95%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            data: dialogData,
            header: `Create ${_contentType.name}`
        });
    }

    /**
     * Opens the legacy content dialog for creating content using the iframe editor
     * This is a placeholder for the legacy implementation
     *
     * @private
     * @param {DotCMSContentType} _contentType - The content type to create content for
     */
    private openLegacyContentDialog(_contentType: DotCMSContentType): void {
        // TODO: Implement legacy dialog opening
        // Legacy content creation not yet implemented for content type: contentType
        // This would eventually open an iframe dialog with the legacy editor
        // Similar to how it's done in the existing legacy JSP files
    }
}
