import { take, mergeMap, pluck, takeUntil } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';

import { ContentType } from '../shared/content-type.model';
import { ContentTypesFormComponent } from '../form';
import { CrudService } from '@services/crud';
import { ContentTypeField } from '../fields/index';
import { FieldService } from '../fields/service';
import { DotMessageService } from '@services/dot-messages-service';
import { ContentTypesInfoService } from '@services/content-types-info';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ResponseView } from 'dotcms-js';

import { HotkeysService, Hotkey } from 'angular2-hotkeys';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { MenuItem } from 'primeng/primeng';
import { Subject } from 'rxjs';


/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-content-types-edit',
    templateUrl: './content-types-edit.component.html',
    styleUrls: ['./content-types-edit.component.scss']
})
export class ContentTypesEditComponent implements OnInit, OnDestroy {
    @ViewChild('form')
    form: ContentTypesFormComponent;
    contentTypeActions: MenuItem[];
    data: ContentType;
    fields: ContentTypeField[];
    show: boolean;
    templateInfo = {
        icon: '',
        header: ''
    };
    messagesKey: { [key: string]: string } = {};
    editButtonLbl: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotEventsService: DotEventsService,
        private dotRouterService: DotRouterService,
        private fieldService: FieldService,
        private hotkeysService: HotkeysService,
        private route: ActivatedRoute,
        public dotMessageService: DotMessageService,
        public router: Router
    ) {}

    ngOnInit(): void {
        this.route.data.pipe(pluck('contentType'), takeUntil(this.destroy$)).subscribe((contentType: ContentType) => {
            this.data = contentType;

            if (contentType.fields) {
                this.fields = contentType.fields;
            }
        });

        this.dotMessageService
            .getMessages([
                'contenttypes.action.create',
                'contenttypes.action.edit',
                'contenttypes.action.update',
                'contenttypes.content.content',
                'contenttypes.content.create.contenttype',
                'contenttypes.content.edit.contenttype',
                'contenttypes.content.fileasset',
                'contenttypes.content.form',
                'contenttypes.content.htmlpage',
                'contenttypes.content.key_value',
                'contenttypes.content.persona',
                'contenttypes.content.vanity_url',
                'contenttypes.content.variable',
                'contenttypes.content.widget',
                'contenttypes.form.identifier',
                'contenttypes.dropzone.rows.add',
                'contenttypes.dropzone.rows.tab_divider'

            ])
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
                this.contentTypeActions = [{
                    label: this.messagesKey['contenttypes.dropzone.rows.add'],
                    command: () => this.notifyAddEvt('add-row')
                },
                {
                    label: this.messagesKey['contenttypes.dropzone.rows.tab_divider'],
                    command: () => this.notifyAddEvt('add-tab-divider')
                }];
            });

        this.setTemplateInfo();

        this.show = !this.isEditMode();

        if (!this.isEditMode()) {
            this.bindEscKey();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle cancel button in dialog
     *
     * @memberof ContentTypesEditComponent
     */
    cancelForm(): void {
        this.show = false;

        if (!this.isEditMode()) {
            this.dotRouterService.gotoPortlet('/content-types-angular');
        }
    }

    /**
     * Set the icon, labels and placeholder in the template
     * @memberof ContentTypesEditComponent
     */
    setTemplateInfo(): void {
        this.dotMessageService.messageMap$.subscribe(() => {
            const type = this.contentTypesInfoService.getLabel(this.data.baseType);
            const contentTypeName = this.messagesKey[`contenttypes.content.${type}`];

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
        });
    }

    /**
     * Check if we need to update or create a content type
     *
     * @param * value;
     * @memberof ContentTypesEditComponent
     */
    handleFormSubmit(value: any): void {
        this.show = false;
        this.isEditMode() ? this.updateContentType(value) : this.createContentType(value);
    }

    /**
     * Check if the component is in edit mode
     *
     * @returns boolean
     * @memberof ContentTypesEditComponent
     */
    isEditMode(): boolean {
        return !!(this.data && this.data.id);
    }

    /**
     * Remove fields from the content type
     * @param fieldsToDelete Fields to be removed
     * @memberof ContentTypesEditComponent
     */
    removeFields(fieldsToDelete: ContentTypeField[]): void {
        this.fieldService
            .deleteFields(this.data.id, fieldsToDelete)
            .pipe(pluck('fields'))
            .subscribe(
                (fields: ContentTypeField[]) => {
                    this.fields = fields;
                },
                (err: ResponseView) => {
                    this.dotHttpErrorManagerService.handle(err).subscribe(() => {});
                }
            );
    }

    /**
     * Save fields to the content type
     * @param fieldsToSave Fields to be save
     * @memberof ContentTypesEditComponent
     */
    saveFields(fieldsToSave: ContentTypeField[]): void {

        this.fieldService.saveFields(this.data.id, fieldsToSave).subscribe(
            (fields: ContentTypeField[]) => {
                if (this.updateOrNewField(fieldsToSave)) {
                    this.fields = fields;
                }
            },
            (err: ResponseView) => {
                this.dotHttpErrorManagerService.handle(err).subscribe(() => {});
            }
        );
    }

    /**
     * Send a notification of Add Row event to be handle elsewhere
     *
     * @memberof ContentTypesEditComponent
     */
    notifyAddEvt(typeEvt: string): void {
        this.dotEventsService.notify(typeEvt);
    }

    private updateOrNewField(fieldsToSave: ContentTypeField[]): boolean {
        return !fieldsToSave[0].id || fieldsToSave.length === 1;
    }

    private bindEscKey(): void {
        this.hotkeysService.add(
            new Hotkey(
                'esc',
                (_event: KeyboardEvent): boolean => {
                    this.cancelForm();
                    return false;
                }
            )
        );
    }

    private createContentType(value: ContentType): void {
        this.crudService
            .postData('v1/contenttype', value)
            .pipe(
                mergeMap((contentTypes: ContentType[]) => contentTypes),
                take(1)
            )
            .subscribe(
                (contentType: ContentType) => {
                    this.data = contentType;
                    this.fields = this.data.fields;
                    this.dotRouterService.goToEditContentType(this.data.id);
                    this.show = false;
                },
                (err: ResponseView) => {
                    this.handleHttpError(err);
                }
            );
    }

    private handleHttpError(err: ResponseView) {
        this.dotHttpErrorManagerService.handle(err).subscribe((_handled: DotHttpErrorHandled) => {
            this.dotRouterService.gotoPortlet('/content-types-angular');
        });
    }

    private updateContentType(value: any): void {
        const data = Object.assign({}, value, { id: this.data.id });

        this.crudService.putData(`v1/contenttype/id/${this.data.id}`, data).subscribe(
            (contentType: ContentType) => {
                this.data = contentType;
                this.show = false;
            },
            (err: ResponseView) => {
                this.handleHttpError(err);
            }
        );
    }
}
