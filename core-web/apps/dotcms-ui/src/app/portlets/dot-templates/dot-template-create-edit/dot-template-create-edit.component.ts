import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';

import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { DialogService } from 'primeng/dynamicdialog';

import { DotTemplate } from '@shared/models/dot-edit-layout-designer/dot-template.model';

import { DotTemplatePropsComponent } from './dot-template-props/dot-template-props.component';
import { DotTemplateItem, DotTemplateState, DotTemplateStore } from './store/dot-template.store';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DynamicDialogRef } from 'primeng/dynamicdialog/dynamicdialog-ref';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { DotLayout } from '@models/dot-edit-layout-designer/dot-layout.model';

@Component({
    selector: 'dot-template-create-edit',
    templateUrl: './dot-template-create-edit.component.html',
    styleUrls: ['./dot-template-create-edit.component.scss'],
    providers: [DotTemplateStore]
})
export class DotTemplateCreateEditComponent implements OnInit, OnDestroy {
    vm$ = this.store.vm$;
    didTemplateChanged$ = this.store.didTemplateChanged$;

    form: UntypedFormGroup;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotTemplateStore,
        private fb: UntypedFormBuilder,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService,
        private dotSiteService: SiteService
    ) {}

    ngOnInit() {
        this.vm$
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ original, working }: DotTemplateState) => {
                const template = original.type === 'design' ? working : original;
                if (this.form) {
                    const value = this.getFormValue(template);

                    this.form.setValue(value);
                } else {
                    this.form = this.getForm(template);
                }

                if (!template.identifier) {
                    this.createTemplate();
                }
            });
        this.setSwitchSiteListener();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Start template properties edition
     *
     * @memberof DotTemplateCreateEditComponent
     */
    editTemplateProps(): void {
        this.dialogService.open(DotTemplatePropsComponent, {
            header: this.dotMessageService.get('templates.properties.title'),
            width: '30rem',
            data: {
                template: this.form.value,
                onSave: (value: DotTemplateItem) => {
                    // If it is a template Desing, save entire template
                    if (value.type === 'design') {
                        this.store.saveTemplate(value);
                    } else {
                        this.store.saveProperties(value);
                    }
                }
            }
        });
    }

    /**
     * Save and publish template to store
     *
     * @param DotTemplate template
     * @memberof DotTemplateCreateEditComponent
     */
    saveAndPublishTemplate(template: DotTemplate): void {
        this.store.saveAndPublishTemplate({
            ...this.form.value,
            ...this.formatTemplateItem(template)
        });
    }

    /**
     * Update Working Template
     *
     * @param DotTemplate template
     * @memberof DotTemplateCreateEditComponent
     */
    updateWorkingTemplate(template: DotTemplate): void {
        this.store.saveWorkingTemplate({
            ...this.form.value,
            ...this.formatTemplateItem(template)
        });
    }

    /**
     * Save template to store
     *
     * @param DotTemplate template
     * @memberof DotTemplateCreateEditComponent
     */
    saveTemplate(template: DotTemplate): void {
        this.store.saveTemplate({
            ...this.form.value,
            ...this.formatTemplateItem(template)
        });
    }

    /**
     * Handle cancel button from designer
     *
     * @memberof DotTemplateCreateEditComponent
     */
    cancelTemplate() {
        this.store.goToTemplateList();
    }

    /**
     * Handle the custom event emitted by the History Tab
     *
     * @param CustomEvent $event
     * @memberof DotTemplateBuilderComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.store.goToEditTemplate($event.detail.data.id, $event.detail.data.inode);
    }

    private createTemplate(): void {
        const ref: DynamicDialogRef = this.dialogService.open(DotTemplatePropsComponent, {
            header: this.dotMessageService.get('templates.create.title'),
            width: '40rem',
            closable: false,
            closeOnEscape: false,
            data: {
                template: this.form.value,
                onSave: (value: DotTemplateItem) => {
                    this.store.createTemplate(value);
                }
            }
        });
        ref.onClose.pipe(takeUntil(this.destroy$)).subscribe((goToListing: boolean) => {
            if (goToListing || goToListing === undefined) {
                this.cancelTemplate();
            }
        });
    }

    private getForm(template: DotTemplateItem): UntypedFormGroup {
        if (template.type === 'design') {
            return this.fb.group({
                type: template.type,
                title: [template.title, Validators.required],
                layout: this.fb.group(template.layout),
                identifier: template.identifier,
                friendlyName: template.friendlyName,
                theme: template.theme,
                image: template.image
            });
        }

        return this.fb.group({
            type: template.type,
            title: [template.title, Validators.required],
            body: template.body,
            identifier: template.identifier,
            friendlyName: template.friendlyName,
            image: template.image
        });
    }

    private getFormValue(template: DotTemplateItem): { [key: string]: string | DotLayout } {
        if (template.type === 'design') {
            return {
                type: template.type,
                title: template.title,
                layout: template.layout,
                identifier: template.identifier,
                friendlyName: template.friendlyName,
                theme: template.theme,
                image: template.image
            };
        }

        return {
            type: template.type,
            title: template.title,
            body: template.body,
            identifier: template.identifier,
            friendlyName: template.friendlyName,
            image: template.image
        };
    }

    private setSwitchSiteListener(): void {
        /**
         * When the portlet reload (from the browser reload button), the site service emits
         * the switchSite$ because the `currentSite` was undefined and the loads the site, that trigger
         * an unwanted reload.
         *
         * This extra work in the filter is to prevent that extra reload.
         *
         */
        let currentHost = this.dotSiteService.currentSite?.hostname || null;
        this.dotSiteService.switchSite$
            .pipe(
                takeUntil(this.destroy$),
                filter((site: Site) => {
                    if (currentHost === null) {
                        currentHost = site?.hostname;

                        return false;
                    }

                    return true;
                })
            )
            .subscribe(() => {
                this.store.goToTemplateList();
            });
    }

    private formatTemplateItem({ layout, body, themeId }: DotTemplate): DotTemplateItem {
        let value = {
            ...this.form.value,
            body
        };

        if (layout) {
            value = {
                ...this.form.value,
                layout,
                theme: themeId
            };
        }

        return value;
    }
}
