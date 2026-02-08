import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { map, mergeMap, pluck, take } from 'rxjs/operators';

import {
    DotContentTypesInfoService,
    DotCrudService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSWorkflow,
    DotDialogActions
} from '@dotcms/dotcms-models';

import { DotEditContentTypeCacheService } from './components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-edit-content-type-cache.service';
import { ContentTypeFieldsDropZoneComponent } from './components/fields/index';
import { FieldService } from './components/fields/service';
import { ContentTypesFormComponent } from './components/form';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class DotContentTypesEditComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-content-types-edit',
    templateUrl: './dot-content-types-edit.component.html',
    standalone: false
})
export class DotContentTypesEditComponent implements OnInit, OnDestroy {
    private contentTypesInfoService = inject(DotContentTypesInfoService);
    private crudService = inject(DotCrudService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotEventsService = inject(DotEventsService);
    private dotRouterService = inject(DotRouterService);
    private fieldService = inject(FieldService);
    private route = inject(ActivatedRoute);
    private dotMessageService = inject(DotMessageService);
    router = inject(Router);
    private dotEditContentTypeCacheService = inject(DotEditContentTypeCacheService);

    readonly $contentTypesForm = viewChild<ContentTypesFormComponent>('form');
    readonly $fieldsDropZone = viewChild<ContentTypeFieldsDropZoneComponent>('fieldsDropZone');

    contentTypeActions: MenuItem[];
    dialogCloseable = false;
    data: DotCMSContentType;
    dialogActions: DotDialogActions;
    layout: DotCMSContentTypeLayoutRow[];
    show: boolean;
    templateInfo = {
        icon: '',
        header: ''
    };

    loadingFields = signal(false);

    private destroy$: Subject<boolean> = new Subject<boolean>();
    private destroyRef = inject(DestroyRef);
    ngOnInit(): void {
        this.route.data
            .pipe(
                map((data) => data.contentType),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((contentType: DotCMSContentType) => {
                this.data = contentType;
                this.dotEditContentTypeCacheService.set(contentType);
                this.layout = contentType.layout;
                this.checkAndOpenFormDialog();
            });

        this.contentTypeActions = [
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.add'),
                command: () => this.notifyAddEvt('add-row')
            },
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.tab_divider'),
                command: () => this.notifyAddEvt('add-tab-divider')
            }
        ];

        this.dialogCloseable = this.isEditMode();
        this.setTemplateInfo();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle hide dialog
     *
     * @memberof DotContentTypesEditComponent
     */
    onDialogHide(): void {
        if (!this.isEditMode()) {
            this.dotRouterService.gotoPortlet(`/${this.dotRouterService.currentPortlet.id}`);
        } else {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParams: { 'open-config': null },
                queryParamsHandling: 'merge'
            });
        }
    }

    /**
     * Show and set options for dialog
     *
     * @memberof DotContentTypesEditComponent
     */
    startFormDialog(): void {
        this.show = true;
        this.setEditContentletDialogOptions();
    }

    /**
     * Set updated name on Content Type and send a request to save it
     * @param {string} name
     *
     * @memberof DotContentTypesEditComponent
     */
    editContentTypeName(name: string): void {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { layout, fields, ...updatedContentType } = { ...this.data, name };

        this.updateContentType(updatedContentType as DotCMSContentType);
    }

    /**
     * Set the icon, labels and placeholder in the template
     * @memberof DotContentTypesEditComponent
     */
    setTemplateInfo(): void {
        const type = this.contentTypesInfoService.getLabel(this.data.baseType);
        const contentTypeName = this.dotMessageService.get(`contenttypes.content.${type}`);

        this.templateInfo = {
            icon: this.contentTypesInfoService.getIcon(type),
            header: this.isEditMode()
                ? this.dotMessageService.get(
                      'contenttypes.content.edit.contenttype',
                      contentTypeName
                  )
                : this.dotMessageService.get(
                      'contenttypes.content.create.contenttype',
                      contentTypeName
                  )
        };
    }

    /**
     * Set the state for the ok action for the dialog
     *
     * @param {boolean} $event
     * @memberof DotContentTypesEditComponent
     */
    setDialogOkButtonState(formIsValid: boolean): void {
        this.dialogActions = {
            ...this.dialogActions,
            accept: {
                ...this.dialogActions.accept,
                disabled: !formIsValid
            }
        };
    }

    /**
     * Check if we need to update or create a content type
     *
     * @param {DotCMSContentType} value
     * @memberof DotContentTypesEditComponent
     */
    handleFormSubmit(value: DotCMSContentType): void {
        this.isEditMode() ? this.updateContentType(value) : this.createContentType(value);
    }

    /**
     * Check if the component is in edit mode
     *
     * @returns boolean
     * @memberof DotContentTypesEditComponent
     */
    isEditMode(): boolean {
        return !!(this.data && this.data.id);
    }

    /**
     * Remove fields from the content type
     * @param DotContentTypeField[] fieldsToDelete Fields to be removed
     * @memberof DotContentTypesEditComponent
     */
    removeFields(fieldsToDelete: DotCMSContentTypeField[]): void {
        this.fieldService
            .deleteFields(this.data.id, fieldsToDelete)
            .pipe(pluck('fields'), take(1))
            .subscribe(
                (fields: DotCMSContentTypeLayoutRow[]) => {
                    this.layout = fields;
                },
                (err) => {
                    this.dotHttpErrorManagerService
                        .handle(err)
                        .pipe(take(1))
                        .subscribe(() => {
                            //
                        });
                }
            );
    }

    /**
     * Save fields to the content type
     * @param layout layout to be save
     * @memberof DotContentTypesEditComponent
     */
    saveFields(layout: DotCMSContentTypeLayoutRow[]): void {
        this.loadingFields.set(true);
        this.fieldService
            .saveFields(this.data.id, layout)
            .pipe(take(1))
            .subscribe(
                (fields: DotCMSContentTypeLayoutRow[]) => {
                    this.layout = fields;
                    this.loadingFields.set(false);
                },
                (err) => {
                    this.dotHttpErrorManagerService
                        .handle(err)
                        .pipe(take(1))
                        .subscribe(() => {
                            this.$fieldsDropZone().cancelLastDragAndDrop();
                            this.loadingFields.set(false);
                        });
                }
            );
    }

    /**
     * Edit the properties of a field
     *
     * @param {DotCMSContentTypeField} fieldsToEdit field to be edit
     * @memberof DotContentTypesEditComponent
     */
    editField(fieldsToEdit: DotCMSContentTypeField): void {
        this.loadingFields.set(true);
        this.fieldService
            .updateField(this.data.id, fieldsToEdit)
            .pipe(take(1))
            .subscribe(
                (fields: DotCMSContentTypeLayoutRow[]) => {
                    this.layout = fields;
                    this.loadingFields.set(false);
                },
                (err) => {
                    this.dotHttpErrorManagerService
                        .handle(err)
                        .pipe(take(1))
                        .subscribe(() => {
                            this.$fieldsDropZone().cancelLastDragAndDrop();
                            this.loadingFields.set(false);
                        });
                }
            );
    }

    /**
     * Send a notification of Add Row event to be handle elsewhere
     *
     * @memberof DotContentTypesEditComponent
     */
    notifyAddEvt(typeEvt: string): void {
        this.dotEventsService.notify(typeEvt);
    }

    private setEditContentletDialogOptions(): void {
        this.dialogActions = {
            accept: {
                disabled: true,
                label: this.isEditMode()
                    ? this.dotMessageService.get('contenttypes.action.update')
                    : this.dotMessageService.get('contenttypes.action.create'),
                action: () => {
                    this.$contentTypesForm().submitForm();
                }
            },
            cancel: {
                label: 'Cancel',
                action: () => {
                    this.onDialogHide();
                }
            }
        };
    }

    private createContentType(value: DotCMSContentType): void {
        const createdContentType: DotCMSContentType = this.cleanUpFormValue({
            ...value
        });

        this.crudService
            .postData<DotCMSContentType[], DotCMSContentType>('v1/contenttype', createdContentType)
            .pipe(
                mergeMap((contentTypes: DotCMSContentType[]) => contentTypes),
                take(1)
            )
            .subscribe(
                (contentType: DotCMSContentType) => {
                    this.data = contentType;
                    this.layout = this.data.layout;
                    this.dotRouterService.goToEditContentType(
                        this.data.id,
                        this.dotRouterService.currentPortlet.id
                    );
                    this.show = false;
                },
                (err) => {
                    this.handleHttpError(err);
                }
            );
    }

    private handleHttpError(err: HttpErrorResponse) {
        this.dotHttpErrorManagerService.handle(err).pipe(take(1));
    }

    private updateContentType(value: DotCMSContentType): void {
        const updatedContentType = this.cleanUpFormValue({
            ...value,
            id: this.data.id
        });

        this.crudService
            .putData<DotCMSContentType>(`v1/contenttype/id/${this.data.id}`, updatedContentType)
            .pipe(take(1))
            .subscribe(
                (contentType: DotCMSContentType) => {
                    this.data = contentType;
                    this.show = false;
                },
                (err) => {
                    this.handleHttpError(err);
                }
            );
    }

    // The Content Types endpoint returns workflows (plural) but receive workflow (singular)
    private cleanUpFormValue(value: DotCMSContentType): DotCMSContentType {
        if (value.workflows) {
            value['workflow'] = this.getWorkflowsIds(value.workflows);
            delete value.workflows;
        }

        return value;
    }

    private getWorkflowsIds(workflows: DotCMSWorkflow[]): string[] {
        return workflows.map((workflow: DotCMSWorkflow) => workflow.id);
    }

    /**
     * Checks conditions to open the form dialog
     * @private
     * @memberof DotContentTypesEditComponent
     */
    private checkAndOpenFormDialog(): void {
        // Subscribe to query params only if we're in edit mode
        if (this.isEditMode()) {
            this.route.queryParams.pipe(take(1)).subscribe((params) => {
                if (params['open-config'] === 'true') {
                    this.startFormDialog();
                }
            });
        } else {
            // Always open form dialog in create mode
            this.startFormDialog();
        }
    }
}
