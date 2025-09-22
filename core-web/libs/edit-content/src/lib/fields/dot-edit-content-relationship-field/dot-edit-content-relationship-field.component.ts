import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    DestroyRef,
    inject,
    input,
    signal,
    OnInit,
    forwardRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { TableRowReorderEvent, TableModule } from 'primeng/table';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { FooterComponent } from './components/dot-select-existing-content/components/footer/footer.component';
import { HeaderComponent } from './components/dot-select-existing-content/components/header/header.component';
import { DotSelectExistingContentComponent } from './components/dot-select-existing-content/dot-select-existing-content.component';
import { PaginationComponent } from './components/pagination/pagination.component';
import { RelationshipFieldStore } from './store/relationship-field.store';

import { DotEditContentDialogComponent } from '../../components/dot-create-content-dialog/dot-create-content-dialog.component';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { ContentletStatusPipe } from '../../pipes/contentlet-status.pipe';
import { LanguagePipe } from '../../pipes/language.pipe';
import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseFieldComponent } from '../shared/base-field.component';

@Component({
    selector: 'dot-edit-content-relationship-field',
    imports: [
        TableModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        ChipModule,
        ContentletStatusPipe,
        LanguagePipe,
        PaginationComponent,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-edit-content-relationship-field.component.html',
    styleUrls: ['./dot-edit-content-relationship-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true, optional: true })
        }
    ],
    providers: [
        RelationshipFieldStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentRelationshipFieldComponent)
        }
    ]
})
export class DotEditContentRelationshipFieldComponent extends BaseFieldComponent implements OnInit {
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
        this.statusChanges$.subscribe(() => {
            this.changeDetectorRef.detectChanges();
        });
    }

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

        const contentType = this.store.contentType();

        // Don't open dialog if contentTypeId is null (invalid field data)
        if (!contentType.id) {
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
