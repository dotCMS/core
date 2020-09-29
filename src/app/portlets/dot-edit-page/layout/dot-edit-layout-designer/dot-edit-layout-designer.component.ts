import { DotLayoutColumn } from './../../shared/models/dot-layout-column.model';
import { DotLayoutRow } from './../../shared/models/dot-layout-row.model';
import { Subject } from 'rxjs/internal/Subject';
import { DotEditLayoutService } from '@portlets/dot-edit-page/shared/services/dot-edit-layout.service';
import { DotPageRenderState } from './../../shared/models/dot-rendered-page-state.model';
import { DotAlertConfirmService } from './../../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Component, OnInit, ViewChild, ElementRef, Input, OnDestroy } from '@angular/core';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { ResponseView } from 'dotcms-js';
import * as _ from 'lodash';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotPageRender } from '../../shared/models/dot-rendered-page.model';
import { LoginService } from 'dotcms-js';
import { DotLayoutSideBar } from '../../shared/models/dot-layout-sidebar.model';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotTheme } from '../../shared/models/dot-theme.model';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { Observable } from 'rxjs';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { tap, take, takeUntil } from 'rxjs/operators';
import { DotLayoutBody } from '@portlets/dot-edit-page/shared/models/dot-layout-body.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    selector: 'dot-edit-layout-designer',
    templateUrl: './dot-edit-layout-designer.component.html',
    styleUrls: ['./dot-edit-layout-designer.component.scss']
})
export class DotEditLayoutDesignerComponent implements OnInit, OnDestroy {
    @ViewChild('templateName')
    templateName: ElementRef;
    @Input()
    editTemplate = false;
    @Input()
    pageState: DotPageRenderState;
    newPageState: DotPageRenderState;

    form: FormGroup;
    initialFormValue: any;
    isModelUpdated = false;
    themeDialogVisibility = false;
    currentTheme: DotTheme;

    saveAsTemplate: boolean;
    showTemplateLayoutSelectionDialog = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotDialogService: DotAlertConfirmService,
        private dotEditLayoutService: DotEditLayoutService,
        private dotEventsService: DotEventsService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private dotThemesService: DotThemesService,
        private fb: FormBuilder,
        private loginService: LoginService,
        private dotPageLayoutService: DotPageLayoutService,
        private templateContainersCacheService: TemplateContainersCacheService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit(): void {
        this.setupLayout();

        if (this.shouldShowDialog()) {
            this.showTemplateLayoutDialog();
        } else {
            this.setEditLayoutMode();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Calling the service to add a new box
     *
     * @memberof DotEditLayoutDesignerComponent
     */
    addGridBox() {
        this.dotEditLayoutService.addBox();
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
        this.dotRouterService.goToEditPage({ url: this.pageState.page.pageURI });
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

        this.dotPageLayoutService
            .save(this.pageState.page.identifier, this.form.value)
            .pipe(take(1))
            .subscribe(
                (updatedPage: DotPageRender) => {
                    this.dotGlobalMessageService.success(
                        this.dotMessageService.get('dot.common.message.saved')
                    );
                    this.setupLayout(
                        new DotPageRenderState(this.loginService.auth.user, updatedPage)
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

    private setupLayout(pageState?: DotPageRenderState): void {
        if (pageState) {
            this.pageState = pageState;

            // This fixes https://github.com/dotCMS/core/issues/18830
            // but the reason why it is happening is unknown at this moment
            this.newPageState = _.cloneDeep(this.pageState);
        }
        this.templateContainersCacheService.set(this.pageState.containers);
        this.initForm();
        this.saveAsTemplateHandleChange(false);
        this.dotThemesService
            .get(this.form.get('themeId').value)
            .pipe(take(1))
            .subscribe(
                (theme: DotTheme) => {
                    this.currentTheme = theme;
                },
                (error) => this.errorHandler(error)
            );
        // Emit event to redraw the grid when the sidebar change
        this.form
            .get('layout.sidebar')
            .valueChanges.pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.dotEventsService.notify('layout-sidebar-change');
            });
    }

    // The POST request returns a 400 if we send the same properties we get
    // ISSUE: https://github.com/dotCMS/core/issues/16344
    private cleanUpBody(body: DotLayoutBody): DotLayoutBody {
        return body
            ? {
                  rows: body.rows.map((row: DotLayoutRow) => {
                      return {
                          ...row,
                          columns: row.columns.map((column: DotLayoutColumn) => {
                              return {
                                  containers: column.containers,
                                  leftOffset: column.leftOffset,
                                  width: column.width,
                                  styleClass: column.styleClass
                              };
                          })
                      };
                  })
              }
            : null;
    }

    private initForm(): void {
        this.form = this.fb.group({
            title: this.isLayout() ? null : this.pageState.template.title,
            themeId: this.pageState.template.theme,
            layout: this.fb.group({
                body: this.cleanUpBody(this.pageState.layout.body) || {},
                header: this.pageState.layout.header,
                footer: this.pageState.layout.footer,
                sidebar: this.createSidebarForm()
            })
        });

        this.initialFormValue = _.cloneDeep(this.form.value);
        this.isModelUpdated = false;
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.isModelUpdated = !_.isEqual(this.form.value, this.initialFormValue);
            // TODO: Set sidebar to null if sidebar location is empty, we're expecting a change in the backend to accept null value
        });
    }

    private showTemplateLayoutDialog(): void {
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

    private errorHandler(err: HttpErrorResponse): Observable<any> {
        return this.dotHttpErrorManagerService.handle(err).pipe(
            tap((res: DotHttpErrorHandled) => {
                if (!res.redirected) {
                    this.dotRouterService.goToSiteBrowser();
                }
                this.currentTheme = err.status === 403 ? null : this.currentTheme;
            })
        );
    }
}
