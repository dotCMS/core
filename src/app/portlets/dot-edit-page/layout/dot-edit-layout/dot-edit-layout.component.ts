import { FormGroup, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { DotEditLayoutGridComponent } from '../dot-edit-layout-grid/dot-edit-layout-grid.component';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotLayout } from '../../shared/models/dot-layout.model';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation/index';
import { ResponseView } from 'dotcms-js/dotcms-js';
import * as _ from 'lodash';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotGlobalMessageService } from '../../../../view/components/_common/dot-global-message/dot-global-message.service';

@Component({
    selector: 'dot-edit-layout',
    templateUrl: './dot-edit-layout.component.html',
    styleUrls: ['./dot-edit-layout.component.scss']
})
export class DotEditLayoutComponent implements OnInit {
    @ViewChild('editLayoutGrid') editLayoutGrid: DotEditLayoutGridComponent;
    @ViewChild('templateName') templateName: ElementRef;

    form: FormGroup;
    initialFormValue: FormGroup;
    isModelUpdated = false;

    pageView: DotPageView;
    saveAsTemplate: boolean;
    showTemplateLayoutSelectionDialog = false;

    constructor(
        private dotConfirmationService: DotConfirmationService,
        private dotEventsService: DotEventsService,
        private fb: FormBuilder,
        private pageViewService: PageViewService,
        private route: ActivatedRoute,
        private templateContainersCacheService: TemplateContainersCacheService,
        public dotMessageService: DotMessageService,
        public router: Router,
        private dotEditLayoutService: DotEditLayoutService,
        private dotGlobalMessageService: DotGlobalMessageService
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
                'dot.common.message.saved'
            ])
            .subscribe();

        this.route.data.pluck('pageView').subscribe((pageView: DotPageView) => {
            this.setupLayout(pageView);
            if (!this.isLayout()) {
                this.showTemplateLayoutDialog();
            }
            // Emit event to redraw the grid when the sidebar change
            this.form.get('layout.sidebar').valueChanges.subscribe(() => {
                this.dotEventsService.notify('layout-sidebar-change');
            });
        });
    }

    /**
     * Add a grid box to the ng grid layout component
     *
     * @returns {() => void}
     * @memberof DotEditLayoutComponent
     */
    addGridBox(): void {
        this.editLayoutGrid.addBox();
    }

    /**
     * Check if the template is a template or a layout by comparing the name of the template.
     * Templates with the name "anonymous_layout_TIMESTAMP" are layout assigned to an specific page.
     *
     * @returns {boolean}
     * @memberof DotEditLayoutComponent
     */
    isLayout(): boolean {
        return this.pageView.template.anonymous;
    }

    /**
     * Handle the change when user update save as template checkbox value
     *
     * @memberof DotEditLayoutComponent
     */
    saveAsTemplateHandleChange(value: boolean): void {
        this.saveAsTemplate = value;

        if (this.saveAsTemplate) {
            /*
                Need the timeout so the textfield it's loaded in the DOM before focus, wasn't able to find a better solution
            */
            setTimeout(() => {
                this.templateName.nativeElement.focus();
            }, 0);
        }
    }

    /**
     * Get the LayoutBody and call the service to save the layout
     *
     * @memberof DotEditLayoutComponent
     */
    saveLayout(event): void {
        this.dotGlobalMessageService.loading(this.dotMessageService.get('dot.common.message.saving'));
        const dotLayout: DotLayout = this.form.value;
        this.pageViewService.save(this.pageView.page.identifier, dotLayout).subscribe(
            response => {
                // TODO: This extra request will change once the this.pageViewService.save return a DotPageView object.
                this.pageViewService.get(this.route.snapshot.queryParams.url).subscribe((pageView: DotPageView) => {
                    this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));
                    this.setupLayout(pageView);
                });
            },
            (err: ResponseView) => {
                this.dotGlobalMessageService.error(err.response.statusText);
            }
        );
    }

    /**
     * Set component to edit layout mode, template have no name.
     *
     * @memberof DotEditLayoutComponent
     */
    setEditLayoutMode(): void {
        this.form.get('title').setValue(null);
        this.showTemplateLayoutSelectionDialog = false;
    }

    private setupLayout(pageView: DotPageView): void {
        this.pageView = pageView;
        this.templateContainersCacheService.set(this.pageView.containers);
        this.initForm();
    }

    private initForm(): void {
        this.form = this.fb.group({
            title: this.isLayout() ? null : this.pageView.template.title,
            layout: this.fb.group({
                body: this.dotEditLayoutService.cleanupDotLayoutBody(this.pageView.layout.body) || {},
                header: this.pageView.layout.header,
                footer: this.pageView.layout.footer,
                sidebar: this.fb.group(
                    this.pageView.layout.sidebar || {
                        location: '',
                        containers: [],
                        width: '',
                        widthPercent: '',
                        preview: false
                    }
                )
            })
        });
        this.initialFormValue = _.cloneDeep(this.form);
        this.isModelUpdated = false;
        this.form.valueChanges.subscribe(() => {
            this.isModelUpdated = !_.isEqual(this.form.value, this.initialFormValue.value);
        });
    }

    private showTemplateLayoutDialog(): void {
        this.showTemplateLayoutSelectionDialog = true;
    }
}
