import { DotRenderedPageState } from './../../shared/models/dot-rendered-page-state.model';
import { DotAlertConfirmService } from './../../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Component, OnInit, ViewChild, ElementRef, Input } from '@angular/core';
import { PageViewService } from '@services/page-view/page-view.service';
import { DotMessageService } from '@services/dot-messages-service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { ResponseView } from 'dotcms-js/dotcms-js';
import * as _ from 'lodash';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotRenderedPage } from '../../shared/models/dot-rendered-page.model';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotLayoutSideBar } from '../../shared/models/dot-layout-sidebar.model';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotTheme } from '../../shared/models/dot-theme.model';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { Observable } from 'rxjs';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { tap } from 'rxjs/operators';
import { DotLayout } from '../../shared/models/dot-layout.model';

@Component({
    selector: 'dot-edit-layout-designer',
    templateUrl: './dot-edit-layout-designer.component.html',
    styleUrls: ['./dot-edit-layout-designer.component.scss']
})
export class DotEditLayoutDesignerComponent implements OnInit {
    @ViewChild('templateName')
    templateName: ElementRef;
    @Input()
    editTemplate = false;
    @Input()
    pageState: DotRenderedPageState;

    form: FormGroup;
    initialFormValue: any;
    isModelUpdated = false;
    themeDialogVisibility = false;
    currentTheme: DotTheme;

    saveAsTemplate: boolean;
    showTemplateLayoutSelectionDialog = false;

    constructor(
        private dotEditLayoutService: DotEditLayoutService,
        private dotEventsService: DotEventsService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotDialogService: DotAlertConfirmService,
        private fb: FormBuilder,
        private pageViewService: PageViewService,
        private templateContainersCacheService: TemplateContainersCacheService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService,
        public dotMessageService: DotMessageService,
        private dotThemesService: DotThemesService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'common.validation.name.error.required',
                'dot.common.message.saved',
                'dot.common.message.saving',
                'dot.common.cancel',
                'editpage.layout.dialog.edit.page',
                'editpage.layout.dialog.edit.template',
                'editpage.layout.dialog.header',
                'editpage.layout.dialog.info',
                'editpage.layout.toolbar.action.save',
                'editpage.layout.toolbar.save.template',
                'editpage.layout.toolbar.template.name',
                'editpage.layout.theme.button.label',
                'org.dotcms.frontend.content.submission.not.proper.permissions'
            ])
            .subscribe();

        this.setupLayout();

        if (this.shouldShowDialog()) {
            this.showTemplateLayoutDialog();
        } else {
            this.setEditLayoutMode();
        }
    }

    /**
     * Check if the template is a template or a layout by comparing the name of the template.
     * Templates with the name "anonymous_layout_TIMESTAMP" are layout assigned to an specific page.
     *
     * @returns boolean
     * @memberof DotEditLayoutDesignerComponent
     */
    isLayout(): boolean {
        return !this.pageState.template || this.pageState.template.anonymous;
    }

    /**
     * Go to edit page when user click cancel
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    onCancel(): void {
        this.dotRouterService.goToEditPage(this.pageState.page.pageURI);
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
    saveLayout(): void {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );
        const dotLayout: DotLayout = this.form.value;
        this.pageViewService.save(this.pageState.page.identifier, dotLayout).subscribe(
            (updatedPage: DotRenderedPage) => {
                this.dotGlobalMessageService.display(
                    this.dotMessageService.get('dot.common.message.saved')
                );
                this.setupLayout(
                    new DotRenderedPageState(this.loginService.auth.user, updatedPage)
                );
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

    /**
     * Handle the changes in the Theme Selector component.
     * @param DotTheme theme
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    changeThemeHandler(theme: DotTheme): void {
        this.currentTheme = theme;
        this.form.get('themeId').setValue(theme.inode);
        this.themeDialogVisibility = false;
    }

    /**
     * Close the Theme Dialog.
     *
     *  @memberof DotEditLayoutDesignerComponent
     */
    closeThemeDialog(): void {
        this.themeDialogVisibility = false;
    }

    private setupLayout(pageState?: DotRenderedPageState): void {
        if (pageState) {
            this.pageState.dotRenderedPageState = pageState;
        }
        this.templateContainersCacheService.set(this.pageState.containers);
        this.initForm();
        this.saveAsTemplateHandleChange(false);
        this.dotThemesService.get(this.form.get('themeId').value).subscribe(
            (theme: DotTheme) => {
                this.currentTheme = theme;
            },
            (error: ResponseView) => this.errorHandler(error)
        );
        // Emit event to redraw the grid when the sidebar change
        this.form.get('layout.sidebar').valueChanges.subscribe(() => {
            this.dotEventsService.notify('layout-sidebar-change');
        });
    }

    private initForm(): void {
        this.form = this.fb.group({
            title: this.isLayout() ? null : this.pageState.template.title,
            themeId: this.pageState.template.theme,
            layout: this.fb.group({
                body:
                    this.dotEditLayoutService.cleanupDotLayoutBody(this.pageState.layout.body) ||
                    {},
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
                    message: this.dotMessageService.get(
                        'editpage.layout.dialog.info',
                        this.pageState.template.name
                    ),
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

    // tslint:disable-next-line:cyclomatic-complexity
    private createSidebarForm(): DotLayoutSideBar {
        return {
            location: this.pageState.layout.sidebar ? this.pageState.layout.sidebar.location : '',
            containers: this.pageState.layout.sidebar
                ? this.pageState.layout.sidebar.containers
                : [],
            width: this.pageState.layout.sidebar ? this.pageState.layout.sidebar.width : 'small'
        };
    }

    private shouldShowDialog(): boolean {
        return this.editTemplate && !this.isLayout() && this.pageState.template.canEdit;
    }

    private errorHandler(err: ResponseView): Observable<any> {
        return this.dotHttpErrorManagerService.handle(err).pipe(
            tap((res: DotHttpErrorHandled) => {
                if (!res.redirected) {
                    this.dotRouterService.goToSiteBrowser();
                }
                this.currentTheme = err.response.status === 403 ? null : this.currentTheme;
            })
        );
    }
}
