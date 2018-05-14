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
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { PaginatorService } from '../../../../api/services/paginator';
import { RouterTestingModule } from '@angular/router/testing';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { FormsModule, FormGroup } from '@angular/forms';
import { Component, Input } from '@angular/core';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPage } from '../../../../test/dot-rendered-page.mock';
import { mockUser } from '../../../../test/login-service.mock';
import { async } from '@angular/core/testing';

@Component({
    selector: 'dot-template-addtional-actions-menu',
    template: ''
})
class MockAdditionalOptionsComponent {
    @Input() inode: '';
}

@Component({
    selector: 'dot-layout-properties',
    template: ''
})
class MockDotLayoutPropertiesComponent {
    @Input() group: FormGroup;
}

@Component({
    selector: 'dot-layout-designer',
    template: ''
})
class MockDotLayoutDesignerComponent {
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
    declarations: [
        DotEditLayoutDesignerComponent,
        MockAdditionalOptionsComponent,
        MockDotLayoutDesignerComponent,
        MockDotLayoutPropertiesComponent
    ],
    imports: [DotEditLayoutGridModule, RouterTestingModule, DotActionButtonModule, FormsModule, FieldValidationMessageModule],
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

describe('DotEditLayoutDesignerComponent', () => {
    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            ...testConfigObject,
            providers: [...testConfigObject.providers]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutDesignerComponent);
        component = fixture.componentInstance;
    }));

    describe('edit layout', () => {
        beforeEach(() => {
            component.pageState = new DotRenderedPageState(
                mockUser,
                {
                    ...mockDotRenderedPage,
                    template: null,
                    canCreateTemplate: false
                },
                null
            );
            fixture.detectChanges();
        });

        it('should show page title', () => {
            const pageTitle: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__page-title'));
            expect(pageTitle.nativeElement.textContent).toEqual('A title');
        });

        it('should not show template name input', () => {
            const templateNameInput: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-template-name'));
            expect(templateNameInput).toBe(null);
        });

        it('should not show checkbox to save as template', () => {
            const checkboxSave: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-save-template'));
            expect(checkboxSave).toBe(null);
        });

        it('should show cancel button', () => {
            fixture.detectChanges();
            const cancelButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-cancel'));

            expect(cancelButton).toBeTruthy();
            expect(cancelButton.nativeElement.textContent).toEqual('Cancel');
        });

        it('should show save button', () => {
            fixture.detectChanges();
            const saveButton: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));

            expect(saveButton).toBeTruthy();
            expect(saveButton.nativeElement.textContent).toEqual('Save');
        });

        it('should show dot-layout-properties and bind attr correctly', () => {
            fixture.detectChanges();
            const layoutProperties: DebugElement = fixture.debugElement.query(By.css('dot-layout-properties'));

            expect(layoutProperties).toBeTruthy();
            expect(layoutProperties.componentInstance.group).toEqual(component.form.get('layout'));
        });

        it('should have dot-layout-designer', () => {
            fixture.detectChanges();
            const layoutDesigner: DebugElement = fixture.debugElement.query(By.css('dot-layout-designer'));

            expect(layoutDesigner).toBeTruthy();
            expect(layoutDesigner.componentInstance.group).toEqual(component.form.get('layout'));
        });

        it('should not show dot-template-addtional-actions-menu', () => {
            const aditionalOptions: DebugElement = fixture.debugElement.query(By.css('dot-template-addtional-actions-menu'));

            expect(aditionalOptions).toBe(null);
        });

        it('should not show save as template checkbox', () => {
            const saveAsTemplate: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-save-template'));

            expect(saveAsTemplate).toBe(null);
        });

        it('should set form model correctly', () => {
            expect(component.form.value).toEqual({
                title: null,
                layout: {
                    body: mockDotRenderedPage.layout.body,
                    header: mockDotRenderedPage.layout.header,
                    footer: mockDotRenderedPage.layout.footer,
                    sidebar: {
                        location: mockDotRenderedPage.layout.sidebar.location,
                        containers: mockDotRenderedPage.layout.sidebar.containers,
                        width: mockDotRenderedPage.layout.sidebar.width
                    }
                }
            });
        });

        describe('can save as template', () => {
            beforeEach(() => {
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    {
                        ...mockDotRenderedPage,
                        template: null,
                        canCreateTemplate: true
                    },
                    null
                );
                component.editTemplate = true;
                fixture.detectChanges();
            });

            it('should show save as template checkbox', () => {
                const saveAsTemplate: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-save-template'));
                expect(saveAsTemplate).not.toBe(null);
            });

            describe('save as template mode', () => {
                let templateNameField: DebugElement;
                beforeEach(() => {
                    const saveAsTemplate: DebugElement = fixture.debugElement.query(
                        By.css('.dot-edit-layout__toolbar-save-template input[type="checkbox"]')
                    );
                    saveAsTemplate.nativeElement.click();
                    fixture.detectChanges();
                    templateNameField = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-template-name'));
                });

                it('should set save as template mode', () => {
                    expect(templateNameField).not.toBe(null);
                });

                it('should update template title in form model', () => {
                    templateNameField.nativeElement.value = 'Hello World';
                    const evt = document.createEvent('Event');
                    evt.initEvent('input', true, false);
                    templateNameField.nativeElement.dispatchEvent(evt);
                    fixture.detectChanges();

                    expect(component.form.get('title').value).toEqual('Hello World');
                });
            });
        });
    });

    describe('edit template', () => {
        beforeEach(() => {
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
            component.editTemplate = true;
            fixture.detectChanges();
        });

        it('should show dot-template-addtional-actions-menu and bind attr correctly', () => {
            const aditionalOptions: DebugElement = fixture.debugElement.query(By.css('dot-template-addtional-actions-menu'));

            expect(aditionalOptions).toBeTruthy();
            expect(aditionalOptions.componentInstance.inode).toEqual(mockDotRenderedPage.template.inode);
        });

        it('should set form model correctly', () => {
            expect(component.form.value).toEqual({
                title: mockDotRenderedPage.template.title,
                layout: {
                    body: mockDotRenderedPage.layout.body,
                    header: mockDotRenderedPage.layout.header,
                    footer: mockDotRenderedPage.layout.footer,
                    sidebar: {
                        location: mockDotRenderedPage.layout.sidebar.location,
                        containers: mockDotRenderedPage.layout.sidebar.containers,
                        width: mockDotRenderedPage.layout.sidebar.width
                    }
                }
            });
        });
    });

    describe('containers model', () => {
        beforeEach(() => {
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
        });

        it('should have a sidebar containers', () => {
            fixture.detectChanges();
            expect(component.form.value.layout.sidebar.containers).toEqual([
                {
                    identifier: 'fc193c82-8c32-4abe-ba8a-49522328c93e',
                    uuid: 'LEGACY_RELATION_TYPE'
                }
            ]);
        });

        it('should have a null sidebar containers', () => {
            component.pageState.layout.sidebar.containers = [];
            fixture.detectChanges();
            expect(component.form.value.layout.sidebar.containers).toEqual([]);
        });
    });

    describe('save button', () => {
        let saveButton: DebugElement;

        beforeEach(() => {
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
            fixture.detectChanges();
            saveButton = fixture.debugElement.query(By.css('.dot-edit-layout__toolbar-action-save'));
        });

        it('should have disabled by default', () => {
            expect(saveButton.nativeElement.disabled).toBe(true);
        });

        it('should have enabled if the model is updated', () => {
            component.form.get('layout.header').setValue(true);
            fixture.detectChanges();

            expect(saveButton.nativeElement.disabled).toBe(false);
        });

        it('should have disabled if the form is not valid', () => {
            // This will make the template title required, it's like clicking the "Save as template" checkbox
            // component.saveAsTemplateHandleChange(true);
            fixture.detectChanges();

            expect(saveButton.nativeElement.disabled).toBe(true);
        });

        it('should have enabled when model is updated and form is valid', () => {
            component.saveAsTemplateHandleChange(true);
            component.form.get('title').setValue('Hello World');

            fixture.detectChanges();

            expect(saveButton.nativeElement.disabled).toBe(false);
        });
    });

    describe('edit layout/template dialog', () => {
        let dotDialogService: DotDialogService;

        beforeEach(() => {
            dotDialogService = fixture.debugElement.injector.get(DotDialogService);
            spyOn(dotDialogService, 'alert').and.callThrough();
            spyOn(component, 'setEditLayoutMode');
        });

        describe('should show', () => {
            beforeEach(() => {
                component.pageState = new DotRenderedPageState(mockUser, {
                    ...mockDotRenderedPage,
                    template: {
                        ...mockDotRenderedPage.template,
                        anonymous: false
                    }
                });
                component.editTemplate = true;
                fixture.detectChanges();
            });

            it('user have can edit page or template', () => {
                expect(dotDialogService.alert).toHaveBeenCalled();
            });

            describe('set edit mode', () => {
                it('should set edit layout mode on dialog accept click', () => {
                    dotDialogService.alertModel.accept();
                    expect(component.setEditLayoutMode).toHaveBeenCalledTimes(1);
                });

                it('should keep edit template mode on dialog reject click', () => {
                    dotDialogService.alertReject(null);
                    expect(component.setEditLayoutMode).not.toHaveBeenCalled();
                });
            });
        });

        describe('not show', () => {
            it('when user can\'t edit the template and set layout mode', () => {
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    {
                        ...mockDotRenderedPage,
                        template: {
                            ...mockDotRenderedPage.template,
                            canEdit: false
                        }
                    },
                    null
                );
                fixture.detectChanges();
                expect(dotDialogService.alert).not.toHaveBeenCalled();
                expect(component.setEditLayoutMode).toHaveBeenCalled();
            });

            it('when page have a layout and set layout mode', () => {
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    {
                        ...mockDotRenderedPage,
                        template: {
                            ...mockDotRenderedPage.template,
                            anonymous: true
                        }
                    },
                    null
                );
                fixture.detectChanges();
                expect(dotDialogService.alert).not.toHaveBeenCalled();
                expect(component.setEditLayoutMode).toHaveBeenCalled();
            });

            it('when editTemplate is false by default', () => {
                component.pageState = new DotRenderedPageState(
                    mockUser,
                    {
                        ...mockDotRenderedPage,
                        template: {
                            ...mockDotRenderedPage.template,
                            canEdit: true
                        }
                    },
                    null
                );
                fixture.detectChanges();
                expect(dotDialogService.alert).not.toHaveBeenCalled();
                expect(component.setEditLayoutMode).toHaveBeenCalled();
                expect(component.editTemplate).toEqual(false);
            });
        });
    });

    describe('edit layout No sidebars', () => {
        beforeEach(() => {
            component.pageState = new DotRenderedPageState(
                mockUser,
                {
                    ...mockDotRenderedPage,
                    template: null,
                    canCreateTemplate: false
                },
                null
            );
            component.pageState.layout.sidebar = null;
            fixture.detectChanges();
        });

        it('should not break when sidebar property in layout is null', () => {
            expect(component.form.value).toEqual({
                title: null,
                layout: {
                    body: mockDotRenderedPage.layout.body,
                    header: mockDotRenderedPage.layout.header,
                    footer: mockDotRenderedPage.layout.footer,
                    sidebar: {
                        location: '',
                        containers: [],
                        width: 'small'
                    }
                }
            });
        });
    });
});
