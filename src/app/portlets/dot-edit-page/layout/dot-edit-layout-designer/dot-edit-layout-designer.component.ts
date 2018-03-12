import { DotRenderedPageState } from './../../shared/models/dot-rendered-page-state.model';
import { DotDialogService } from './../../../../api/services/dot-dialog/dot-dialog.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, Input } from '@angular/core';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotLayout } from '../../shared/models/dot-layout.model';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';
import { ResponseView } from 'dotcms-js/dotcms-js';
import * as _ from 'lodash';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotGlobalMessageService } from '../../../../view/components/_common/dot-global-message/dot-global-message.service';

@Component({
    selector: 'dot-edit-layout-designer',
    templateUrl: './dot-edit-layout-designer.component.html',
    styleUrls: ['./dot-edit-layout-designer.component.scss']
})
export class DotEditLayoutDesignerComponent implements OnInit {
    @ViewChild('templateName') templateName: ElementRef;

    @Input() pageState: DotRenderedPageState;

    form: FormGroup;
    initialFormValue: any;
    isModelUpdated = false;

    saveAsTemplate: boolean;
    showTemplateLayoutSelectionDialog = false;

    constructor(
        private dotEditLayoutService: DotEditLayoutService,
        private dotEventsService: DotEventsService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotDialogService: DotDialogService,
        private fb: FormBuilder,
        private pageViewService: PageViewService,
        private templateContainersCacheService: TemplateContainersCacheService,
        public dotMessageService: DotMessageService,
        public router: Router
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'editpage.layout.toolbar.action.save',
                'editpage.layout.toolbar.action.cancel',
                'editpage.layout.toolbar.template.name',
                'editpage.layout.toolbar.save.template',
                'editpage.layout.dialog.edit.page',
                'editpage.layout.dialog.edit.template',
                'editpage.layout.dialog.info',
                'editpage.layout.dialog.header',
                'dot.common.message.saving',
                'dot.common.message.saved',
                'common.validation.name.error.required'
            ])
            .subscribe();

        this.setupLayout();

        if (!this.isLayout() && this.pageState.template.canEdit) {
            this.showTemplateLayoutDialog();
        } else {
            this.setEditLayoutMode();
        }
    }

    /**
     * Check if the template is a template or a layout by comparing the name of the template.
     * Templates with the name "anonymous_layout_TIMESTAMP" are layout assigned to an specific page.
     *
     * @returns {boolean}
     * @memberof DotEditLayoutDesignerComponent
     */
    isLayout(): boolean {
        return !this.pageState.template || this.pageState.template.anonymous;
    }

    /**
     * Handle the change when user update save as template checkbox value
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    saveAsTemplateHandleChange(value: boolean): void {
        const titleFormControl = this.form.get('title');
        titleFormControl.markAsUntouched();
        titleFormControl.markAsPristine();

        this.saveAsTemplate = value;

        if (this.saveAsTemplate) {
            titleFormControl.setValidators(Validators.required);
            /*
                Need the timeout so the textfield it's loaded in the DOM before focus, wasn't able to find a better solution
            */
            setTimeout(() => {
                this.templateName.nativeElement.focus();
            }, 0);
        } else {
            titleFormControl.setValidators(null);
            if (this.initialFormValue.title === null) {
                titleFormControl.setValue(null);
            }
        }
        titleFormControl.updateValueAndValidity();
    }

    /**
     * Get the LayoutBody and call the service to save the layout
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    saveLayout(_event): void {
        this.dotGlobalMessageService.loading(this.dotMessageService.get('dot.common.message.saving'));
        const dotLayout: DotLayout = this.form.value;
        this.pageViewService.save(this.pageState.page.identifier, dotLayout).subscribe(
            () => {
                this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));

                // TODO: This extra request will change once the this.pageViewService.save return a DotPageView object.
                // this.pageViewService.get(this.route.snapshot.queryParams.url).subscribe((pageView: DotPageView) => {
                //     this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));
                //     this.setupLayout(pageView);
                // });
            },
            (err: ResponseView) => {
                this.dotGlobalMessageService.error(err.response.statusText);
            }
        );
    }

    /**
     * Set component to edit layout mode, template have no name.
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    setEditLayoutMode(): void {
        this.initialFormValue.title = null;
        this.form.get('title').setValue(null);

        if (this.pageState.template) {
            this.pageState.template.anonymous = true;
        }
    }

    private setupLayout(pageState?: DotRenderedPageState): void {
        if (pageState) {
            this.pageState = pageState;
        }
        this.templateContainersCacheService.set(this.pageState.containers);
        this.initForm();
        this.saveAsTemplateHandleChange(false);

        // Emit event to redraw the grid when the sidebar change
        this.form.get('layout.sidebar').valueChanges.subscribe(() => {
            this.dotEventsService.notify('layout-sidebar-change');
        });
    }

    private initForm(): void {
        this.form = this.fb.group({
            title: this.isLayout() ? null : this.pageState.template.title,
            layout: this.fb.group({
                body: this.dotEditLayoutService.cleanupDotLayoutBody(this.pageState.layout.body) || {},
                header: this.pageState.layout.header,
                footer: this.pageState.layout.footer,
                sidebar: this.createSidebarForm()
            })
        });

        this.initialFormValue = _.cloneDeep(this.form.value);
        this.isModelUpdated = false;
        this.form.valueChanges.subscribe(() => {
            this.isModelUpdated = !_.isEqual(this.form.value, this.initialFormValue);
            // TODO: Set sidebar to null if sidebar location is empty, we're expecting a change in the backend to accept null value
        });
    }

    private showTemplateLayoutDialog(): void {
        this.dotMessageService
            .getMessages([
                'editpage.layout.dialog.header',
                'editpage.layout.dialog.info',
                'editpage.layout.dialog.edit.page',
                'editpage.layout.dialog.edit.template'
            ])
            .subscribe(() => {
                this.dotDialogService.alert({
                    header: this.dotMessageService.get('editpage.layout.dialog.header'),
                    message: this.dotMessageService.get('editpage.layout.dialog.info', this.pageState.template.name),
                    footerLabel: {
                        accept: this.dotMessageService.get('editpage.layout.dialog.edit.page'),
                        reject: this.dotMessageService.get('editpage.layout.dialog.edit.template')
                    },
                    accept: () => {
                        this.setEditLayoutMode();
                    }
                });
            });
    }

    private createSidebarForm() {
        if (this.pageState.layout.sidebar) {
            return this.fb.group({
                location: this.pageState.layout.sidebar.location,
                containers: this.fb.array(this.pageState.layout.sidebar.containers || []),
                width: this.pageState.layout.sidebar.width
            });
        } else {
            return this.fb.group({
                location: '',
                containers: [],
                width: 'small'
            });
        }
    }
}
