import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    DestroyRef,
    forwardRef,
    inject,
    input,
    OnInit
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { TableModule, TableRowReorderEvent } from 'primeng/table';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { RelationshipFieldStore } from './../../store/relationship-field.store';
import { FooterComponent } from './../dot-select-existing-content/components/footer/footer.component';
import { HeaderComponent } from './../dot-select-existing-content/components/header/header.component';
import { DotSelectExistingContentComponent } from './../dot-select-existing-content/dot-select-existing-content.component';
import { PaginationComponent } from './../pagination/pagination.component';

import { DotEditContentDialogComponent } from '../../../../components/dot-create-content-dialog/dot-create-content-dialog.component';
import { EditContentDialogData } from '../../../../models/dot-edit-content-dialog.interface';
import { ContentletStatusPipe } from '../../../../pipes/contentlet-status.pipe';
import { LanguagePipe } from '../../../../pipes/language.pipe';
import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';

@Component({
    selector: 'dot-relationship-field',
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        ChipModule,
        ContentletStatusPipe,
        LanguagePipe,
        PaginationComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-relationship-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotRelationshipFieldComponent)
        }
    ]
})
export class DotRelationshipFieldComponent
    extends BaseControlValueAccessor<string>
    implements OnInit
{
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
        const isNewEditorEnabled = this.store.isNewEditorEnabled();

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
                disabled: isDisabledCreateNewContent || this.$isDisabled() || !isNewEditorEnabled,
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
     * Signal that tracks whether the component has an error.
     *
     * @type {boolean}
     * @default false
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * Signal that tracks whether the component is required.
     *
     * @type {boolean}
     * @default false
     */
    $isRequired = input.required<boolean>({ alias: 'isRequired' });

    /**
     * Computed signal that holds the field and contentlet.
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    $inputs = computed(() => ({
        field: this.$field(),
        contentlet: this.$contentlet()
    }));

    /**
     * Computed signal that holds the total number of columns.
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    $totalColumns = computed(() => this.store.columns().length + this.store.staticColumns());

    /**
     * Creates an instance of DotEditContentRelationshipFieldComponent.
     * It sets the value of the field to the formatted relationship.
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    constructor() {
        super();
        this.updateValueField(this.store.formattedRelationship);
    }

    /**
     * Initializes the store with the field and contentlet.
     *
     * @memberof DotEditContentRelationshipFieldComponent
     */
    ngOnInit() {
        this.initialize(this.$inputs);
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

        const contentType = this.store.contentType();

        // Don't open dialog if contentTypeId is null (invalid field data)
        if (!contentType.id) {
            return;
        }

        this.#dialogRef = this.#dialogService.open(DotSelectExistingContentComponent, {
            appendTo: 'body',
            baseZIndex: 10000,
            closeOnEscape: false,
            draggable: false,
            keepInViewport: true,
            modal: true,
            resizable: false,
            position: 'center',
            width: '90%',
            height: '90%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field',
            style: { 'max-width': '1040px', 'max-height': '800px' },
            data: {
                contentTypeId: contentType.id,
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
     */
    showCreateNewContentDialog(): void {
        const contentType = this.store.contentType();
        if (this.$isDisabled() || !contentType) {
            return;
        }

        const dialogData: EditContentDialogData = {
            mode: 'new',
            contentTypeId: contentType.id,
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
            baseZIndex: 10000,
            closeOnEscape: true,
            draggable: false,
            keepInViewport: true,
            modal: true,
            resizable: true,
            position: 'center',
            width: '95%',
            height: '95%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            data: dialogData,
            header: `Create ${contentType.name}`
        });
    }

    /**
     * Updates the value of the field.
     *
     * @param value - The value to update.
     */
    readonly updateValueField = signalMethod<string>((value) => {
        if (value === null || value === undefined || !this.onChange || !this.onTouched) {
            return;
        }

        this.onChange(value);
        this.onTouched();
    });

    /**
     * Initializes the store with the field and contentlet.
     *
     * @param field - The field to initialize the store with.
     * @param contentlet - The contentlet to initialize the store with.
     */
    readonly initialize = signalMethod<{
        field: DotCMSContentTypeField;
        contentlet: DotCMSContentlet;
    }>((params) => {
        this.store.initialize({
            field: params.field,
            contentlet: params.contentlet
        });
    });
}
