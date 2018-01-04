import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core/src/debug/debug_node';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { FormatDateService } from '../../../../api/services/format-date-service';
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

@Component({
    selector: 'dot-template-addtional-actions-menu',
    template: ''
})
class MockAdditionalOptionsComponent {
    @Input() templateId: string;
}

@Component({
    selector: 'dot-layout-properties',
    template: ''
})
class MockDotLayoutPropertiesComponent {
    @Input() group: FormGroup;
}

describe('DotEditLayoutComponent', () => {
    let component: DotEditLayoutComponent;
    let fixture: ComponentFixture<DotEditLayoutComponent>;

    const fakePageView = {
        pageView: {
            containers: {
                '5363c6c6-5ba0-4946-b7af-cf875188ac2e': {
                    container: {
                        type: 'containers',
                        identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e',
                        name: 'Medium Column (md-1)',
                        categoryId: '9ab97328-e72f-4d7e-8be6-232f53218a93'
                    }
                },
                '56bd55ea-b04b-480d-9e37-5d6f9217dcc3': {
                    container: {
                        type: 'containers',
                        identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3',
                        name: 'Large Column (lg-1)',
                        categoryId: 'dde0b865-6cea-4ff0-8582-85e5974cf94f'
                    }
                }
            },
            page: {
                identifier: '123',
                title: 'Hello World'
            },
            layout: {
                header: false,
                footer: false,
                sidebar: {
                    location: '',
                    containers: [],
                    width: '',
                    widthPercent: '',
                    preview: false
                },
                body: {
                    rows: []
                }
            },
            template: {
                title: 'anonymous_layout_1511798005268',
                inode: '123',
                anonymous: true
            }
        }
    };

    const messageServiceMock = new MockDotMessageService({
        'editpage.layout.toolbar.action.save': 'Save',
        'editpage.layout.toolbar.action.cancel': 'Cancel',
        'editpage.layout.toolbar.template.name': 'Name of the template',
        'editpage.layout.toolbar.save.template': 'Save as template'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutComponent, MockAdditionalOptionsComponent, MockDotLayoutPropertiesComponent],
            imports: [
                DotEditLayoutGridModule,
                RouterTestingModule,
                BrowserAnimationsModule,
                DotActionButtonModule,
                FormsModule
            ],
            providers: [
                DotConfirmationService,
                FormatDateService,
                LoginService,
                PageViewService,
                PaginatorService,
                SocketFactory,
                DotEditLayoutService,
                TemplateContainersCacheService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: Observable.of(fakePageView)
                    }
                },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditLayoutComponent);
        component = fixture.componentInstance;
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
        expect(aditionalOptions.componentInstance.templateId).toEqual(fakePageView.pageView.template.inode);
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

    it('should have checkbox to save as template', () => {
        fixture.detectChanges();
        const checkboxSave: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-save-template')
        );

        expect(checkboxSave).toBeDefined();
        expect(checkboxSave.nativeElement.textContent).toContain('Save as template');
    });

    it('should show template name input and hide page title if save as template is checked', () => {
        component.saveAsTemplate = true;
        fixture.detectChanges();

        const pageTitle: DebugElement = fixture.debugElement.query(By.css('.dot-edit-layout__page-title'));
        expect(pageTitle === null).toBe(true);

        const templateNameInput: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-template-name')
        );
        expect(templateNameInput).toBeDefined();
    });

    xit('should have header in the template', () => {});

    xit('should NOT have header in the template', () => {});

    xit('should have footer in the template', () => {});

    xit('should NOT have footer in the template', () => {});

    xit('should have sidebar in the template', () => {});

    xit('should NOT have sidebar in the template', () => {});

    it('should have a form', () => {
        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toEqual(form.componentInstance.form);
    });

    it('should not have name set', () => {
        fixture.detectChanges();
        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(component.form.value.title).toBeNull();
    });

    it('should have name set', () => {
        fakePageView.pageView.template.title = 'template_name';
        fakePageView.pageView.template.anonymous = false;
        fixture.detectChanges();

        const form: DebugElement = fixture.debugElement.query(By.css('form'));
        expect(component.form.value.title).toEqual(fakePageView.pageView.template.title);
    });

    it('template-name should has the right formControlName', () => {
        component.saveAsTemplate = true;
        fixture.detectChanges();

        const templateNameInput: DebugElement = fixture.debugElement.query(
            By.css('.dot-edit-layout__toolbar-template-name')
        );
        expect(templateNameInput.attributes.formControlName).toEqual('title');
    });

    it('layput body should has the right formControlName', () => {
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

        expect(templateContainersCacheService.set).toHaveBeenCalledWith(fakePageView.pageView.containers);
    });
});
