import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core/src/debug/debug_node';
import { DotDialogService } from '../../../../api/services/dot-dialog';
import { DotEditLayoutDesignerComponent } from './dot-edit-layout-designer.component';
import { DotEditLayoutGridModule } from '../components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { Observable } from 'rxjs/Observable';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { PaginatorService } from '../../../../api/services/paginator';
import { RouterTestingModule } from '@angular/router/testing';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { FormsModule, FormGroup } from '@angular/forms';
import { Component, Input } from '@angular/core';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotSidebarPropertiesModule } from '../components/dot-sidebar-properties/dot-sidebar-properties.module';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { fakePageView } from '../../../../test/page-view.mock';
import * as _ from 'lodash';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';
import { mockUser } from '../../../../test/login-service.mock';

@Component({
    selector: 'dot-template-addtional-actions-menu',
    template: ''
})
class MockAdditionalOptionsComponent {
    @Input() templateId: '';
}

@Component({
    selector: 'dot-layout-properties',
    template: ''
})
class MockDotLayoutPropertiesComponent {
    @Input() group: FormGroup;
}

const messageServiceMock = new MockDotMessageService({
    'editpage.layout.toolbar.action.save': 'Save',
    'editpage.layout.toolbar.action.cancel': 'Cancel',
    'editpage.layout.toolbar.template.name': 'Name of the template',
    'editpage.layout.toolbar.save.template': 'Save as template',
    'editpage.layout.dialog.edit.page': 'Edit Page',
    'editpage.layout.dialog.edit.template': 'Edit Template',
    'editpage.layout.dialog.info': 'This is the message',
    'editpage.layout.dialog.header': 'Edit some'
});

let component: DotEditLayoutDesignerComponent;
let fixture: ComponentFixture<DotEditLayoutDesignerComponent>;

const testConfigObject = {
    declarations: [DotEditLayoutDesignerComponent, MockAdditionalOptionsComponent, MockDotLayoutPropertiesComponent],
    imports: [
        DotEditLayoutGridModule,
        DotSidebarPropertiesModule,
        RouterTestingModule,
        BrowserAnimationsModule,
        DotActionButtonModule,
        FormsModule,
        FieldValidationMessageModule
    ],
    providers: [
        DotDialogService,
        LoginService,
        PageViewService,
        PaginatorService,
        SocketFactory,
        DotEditLayoutService,
        TemplateContainersCacheService,
        { provide: DotMessageService, useValue: messageServiceMock }
    ]
};

describe('DotEditLayoutDesignerComponent - Layout (anonymous = true)', () => {
    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            ...testConfigObject,
            providers: [...testConfigObject.providers]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutDesignerComponent);
        component = fixture.componentInstance;
        component.pageState = new DotRenderedPageState(mockDotRenderedPage, null, mockUser);
    });

    it('should have dot-edit-layout-grid', () => {
        const gridLayout: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-grid'));
        expect(gridLayout).toBeDefined();
    });

    it('should have dot-template-addtional-actions-menu', () => {
        fixture.detectChanges();
        const aditionalOptions: DebugElement = fixture.debugElement.query(
            By.css('dot-template-addtional-actions-menu')
        );

        expect(aditionalOptions).toBeDefined();
        expect(aditionalOptions.componentInstance.templateId).toEqual(fakePageView.template.inode);
    });

    it('should have a null sidebar containers', () => {
        component.pageState = _.cloneDeep(fakePageView);
        component.pageState.layout.sidebar.containers = null;

        fixture.detectChanges();
        expect(component.form.value.layout.sidebar.containers).toEqual([]);
    });

    it('should have a sidebar containers', () => {
        fixture.detectChanges();

        expect(component.form.value.layout.sidebar.containers).toEqual([{
            identifier: 'fc193c82-8c32-4abe-ba8a-49522328c93e',
            uuid: 'LEGACY_RELATION_TYPE'
        }]);
    });

    it('should have dot-layout-properties', () => {
        fixture.detectChanges();
        const layoutProperties: DebugElement = fixture.debugElement.query(By.css('dot-layout-properties'));

        expect(layoutProperties).toBeDefined();
        expect(component.form.get('layout')).toEqual(layoutProperties.componentInstance.group);
    });

    it('should have add box button', () => {
        const addBoxButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-add'));
        expect(addBoxButton).toBeDefined();
    });

    it('should have page title', () => {
        fixture.detectChanges();
        const pageTitle: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__page-title'));
        expect(pageTitle.nativeElement.textContent).toEqual('Hello World');
    });

    it('should have cancel button', () => {
        fixture.detectChanges();
        const cancelButton: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-action-cancel')
        );

        expect(cancelButton).toBeDefined();
        expect(cancelButton.nativeElement.textContent).toEqual('Cancel');
    });

    it('should have save button', () => {
        fixture.detectChanges();
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

        expect(saveButton).toBeDefined();
        expect(saveButton.nativeElement.textContent).toEqual('Save');
    });

    it('should have save button disabled by default', () => {
        fixture.detectChanges();
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

        expect(saveButton.nativeElement.disabled).toBe(true);
    });

    it('should have save button enabled if the model is updated', () => {
        fixture.detectChanges();
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

        component.form.get('layout.header').setValue(true);
        fixture.detectChanges();

        expect(saveButton.nativeElement.disabled).toBe(false);
    });

    it('should have save button disabled if the form is not valid', () => {
        fixture.detectChanges();
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

        // This will make the template title required, it's like clicking the "Save as template" checkbox
        component.saveAsTemplateHandleChange(true);
        fixture.detectChanges();

        expect(saveButton.nativeElement.disabled).toBe(true);
    });

    it('should have save button enabled when model is updated and form is valid', () => {
        fixture.detectChanges();
        const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

        component.saveAsTemplateHandleChange(true);
        component.form.get('title').setValue('Hello World');

        fixture.detectChanges();

        expect(saveButton.nativeElement.disabled).toBe(false);
    });

    it('should have checkbox to save as template', () => {
        fixture.detectChanges();
        const checkboxSave: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-save-template')
        );

        expect(checkboxSave).toBeDefined();
        expect(checkboxSave.nativeElement.textContent).toContain('Save as template');
    });

    it('should show template name input and hide page title if save as template is checked', () => {
        fixture.detectChanges();
        component.saveAsTemplate = true;
        fixture.detectChanges();

        const pageTitle: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__page-title'));
        expect(pageTitle === null).toBe(true);

        const templateNameInput: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-template-name')
        );
        expect(templateNameInput).toBeDefined();
    });

    it('should have header in the template', () => {
        fixture.detectChanges();
        component.form.get('layout.header').setValue(true);

        fixture.detectChanges();
        const headerElem: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__template-header'));

        expect(headerElem).toBeDefined();
        expect(headerElem.nativeElement.innerHTML).toEqual('HEADER');
    });

    it('should have footer in the template', () => {
        fixture.detectChanges();
        component.form.get('layout.footer').setValue(true);

        fixture.detectChanges();
        const footerElem: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__template-footer'));

        expect(footerElem).toBeDefined();
        expect(footerElem.nativeElement.innerHTML).toEqual('FOOTER');
    });

    it('should NOT have header in the template', () => {
        fixture.detectChanges();
        const headerElem: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__template-header'));

        expect(headerElem === null).toBe(true);
    });

    it('should NOT have footer in the template', () => {
        fixture.detectChanges();
        const footerElem: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__template-footer'));

        expect(footerElem === null).toBe(true);
    });

    it('should NOT have sidebar in the template', () => {
        component.pageState.layout.sidebar = null;
        fixture.detectChanges();

        const sidebarLeft: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__template-sidebar--left')
        );
        const sidebarRight: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__template-sidebar--right')
        );

        expect(sidebarLeft === null).toBe(true);
        expect(sidebarRight === null).toBe(true);
    });

    it('should set sidebar left with "dot-edit-layout__template-sidebar--small" class', () => {
        fixture.detectChanges();
        component.form.get('layout.sidebar.location').setValue('left');
        component.form.get('layout.sidebar.width').setValue('small');

        fixture.detectChanges();
        const sidebarLeft: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__template-sidebar--left')
        );
        expect(sidebarLeft).toBeDefined();
        expect(sidebarLeft.nativeElement.classList.contains('dot-edit-layout__template-sidebar--small')).toEqual(true);
    });

    it('should set sidebar left with "dot-edit-layout__template-sidebar--medium" class', () => {
        fixture.detectChanges();
        component.form.get('layout.sidebar.location').setValue('left');
        component.form.get('layout.sidebar.width').setValue('medium');

        fixture.detectChanges();
        const sidebarLeft: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__template-sidebar--left')
        );
        expect(sidebarLeft.nativeElement.classList.contains('dot-edit-layout__template-sidebar--medium')).toEqual(true);
        expect(sidebarLeft).toBeDefined();
    });

    it('should set sidebar left with "dot-edit-layout__template-sidebar--large" class', () => {
        fixture.detectChanges();
        component.form.get('layout.sidebar.location').setValue('left');
        component.form.get('layout.sidebar.width').setValue('large');

        fixture.detectChanges();
        const sidebarLeft: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__template-sidebar--left')
        );
        expect(sidebarLeft.nativeElement.classList.contains('dot-edit-layout__template-sidebar--large')).toEqual(true);
        expect(sidebarLeft).toBeDefined();
    });

    it('should have a form', () => {
        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toEqual(form.componentInstance.form);
    });

    it('should not have name set', () => {
        fixture.detectChanges();
        expect(component.form.value.title).toBeNull();
    });

    it('should have name set', () => {
        fakePageView.template.title = 'template_name';
        fakePageView.template.anonymous = false;
        fixture.detectChanges();

        expect(component.form.value.title).toEqual(fakePageView.template.title);
    });

    it('template-name should has the right formControlName', () => {
        fixture.detectChanges();
        component.saveAsTemplate = true;
        fixture.detectChanges();

        const templateNameInput: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-template-name')
        );
        expect(templateNameInput.attributes.formControlName).toEqual('title');
    });

    it('layout body should has the right formControlName', () => {
        fixture.detectChanges();

        const dotEditLayoutGrid: DebugElement = fixture.debugElement.query(By.css('dot-edit-layout-grid'));
        const gridGuides: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__grid-guides'));

        expect(gridGuides.attributes.formGroupName).toEqual('layout');
        expect(dotEditLayoutGrid.attributes.formControlName).toEqual('body');
    });

    it('should set containers in TemplateContainersCacheService', () => {
        const templateContainersCacheService: TemplateContainersCacheService = fixture.debugElement.injector.get(
            TemplateContainersCacheService
        );

        spyOn(templateContainersCacheService, 'set');
        fixture.detectChanges();
        expect(templateContainersCacheService.set).toHaveBeenCalledWith(fakePageView.containers);
    });
});

const templateRouteData = [
    {
        provide: ActivatedRoute,
        useValue: {
            // New object of fakePageView with anonymous: false
            data: Observable.of({
                ...fakePageView,
                pageView: {
                    ...fakePageView,
                    template: {
                        ...fakePageView.template,
                        anonymous: false,
                        title: 'Hello Template Name'
                    }
                }
            })
        }
    }
];

xdescribe('DotEditLayoutDesignerComponent - Template (anonymous = false)', () => {
    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            ...testConfigObject,
            providers: [...testConfigObject.providers, ...templateRouteData]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutDesignerComponent);
        component = fixture.componentInstance;
    });

    it('should show select edit layout/template dialog', () => {
        fixture.detectChanges();
        const dialog: DebugElement = fixture.debugElement.query(By.css('p-dialog .ui-dialog'));
        expect(dialog.styles.display).toEqual('block');
    });

    it('should set edit template mode', () => {
        fixture.detectChanges();
        spyOn(component, 'setEditLayoutMode');

        const editLayoutButton: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__dialog-edit-template')
        );
        editLayoutButton.nativeElement.click();
        fixture.detectChanges();

        const checkboxSave: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-save-template')
        );

        expect(component.setEditLayoutMode).not.toHaveBeenCalled();
        expect(component.showTemplateLayoutSelectionDialog).toEqual(false, 'hide the dialog');
        expect(component.form.get('title').value).toEqual('Hello Template Name');
        expect(checkboxSave === null).toBe(true, 'checkbox not showing');
    });

    it('should set edit layout mode', () => {
        spyOn(component, 'setEditLayoutMode').and.callThrough();

        fixture.detectChanges();
        const editLayoutButton: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__dialog-edit-layout')
        );
        editLayoutButton.nativeElement.click();
        fixture.detectChanges();
        expect(component.setEditLayoutMode).toHaveBeenCalledTimes(1);
        expect(component.showTemplateLayoutSelectionDialog).toEqual(false, 'hide the dialog');
        expect(component.form.get('title').value).toBeNull('form title null');
    });

    it('should set the title field required when save as a template is checked', () => {
        spyOn(component, 'saveAsTemplateHandleChange').and.callThrough();
        fixture.detectChanges();
        const editLayoutButton: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__dialog-edit-layout')
        );
        editLayoutButton.nativeElement.click();
        fixture.detectChanges();
        const templateNameInput: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-template-name')
        );
        component.saveAsTemplateHandleChange(true);
        fixture.detectChanges();
        const focusElement: DebugElement = fixture.debugElement.query(By.css(':focus'));

        expect(templateNameInput).toEqual(focusElement);
        expect(component.form.get('title').valid).toEqual(false);
    });
});
