import * as _ from 'lodash';
import { of as observableOf, of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlContainer,
    FormsModule,
    ReactiveFormsModule,
    UntypedFormGroup
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotEventsService, DotMessageService, DotThemesService } from '@dotcms/data-access';
import { DotTemplateDesigner, DotTheme } from '@dotcms/dotcms-models';
import { DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import {
    cleanUpDialog,
    DotThemesServiceMock,
    mockDotLayout,
    MockDotMessageService,
    mockDotRenderedPage,
    mockDotThemes
} from '@dotcms/utils-testing';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';

import { DotEditLayoutDesignerComponent } from './dot-edit-layout-designer.component';

@Component({
    selector: 'dot-template-addtional-actions-menu',
    template: ''
})
class AdditionalOptionsMockComponent {
    @Input() inode: '';
}

@Component({
    selector: 'dot-layout-properties',
    template: ''
})
class DotLayoutPropertiesMockComponent {
    @Input() group: UntypedFormGroup;
}

@Component({
    selector: 'dot-layout-designer',
    template: ''
})
class DotLayoutDesignerMockComponent {
    constructor(public group: ControlContainer) {}
}

@Component({
    selector: 'dot-theme-selector',
    template: ''
})
class DotThemeSelectorMockComponent {
    @Input() value: DotTheme;
    @Output() selected = new EventEmitter<DotTheme>();
}

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saving': 'saving...',
    'dot.common.cancel': 'Cancel',
    'editpage.layout.dialog.edit.page': 'Edit Page',
    'editpage.layout.dialog.edit.template': 'Edit Template',
    'editpage.layout.dialog.header': 'Edit some',
    'editpage.layout.dialog.info': 'This is the message',
    'editpage.layout.toolbar.action.save': 'Save',
    'editpage.layout.toolbar.save.template': 'Save as template',
    'editpage.layout.toolbar.template.name': 'Name of the template',
    'org.dotcms.frontend.content.submission.not.proper.permissions': 'No Read Permission'
});

let component: DotEditLayoutDesignerComponent;
let fixture: ComponentFixture<DotEditLayoutDesignerComponent>;
let dotThemesService: DotThemesService;

describe('DotEditLayoutDesignerComponent', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotEditLayoutDesignerComponent,
                AdditionalOptionsMockComponent,
                DotLayoutPropertiesMockComponent,
                DotThemeSelectorMockComponent,
                DotLayoutDesignerMockComponent
            ],
            imports: [
                DotMessagePipe,
                DotActionButtonModule,
                DotEditPageInfoModule,
                DotSecondaryToolbarModule,
                DotGlobalMessageModule,
                DotFieldValidationMessageModule,
                FormsModule,
                ReactiveFormsModule,
                RouterTestingModule,
                TooltipModule,
                UiDotIconButtonModule,
                ButtonModule
            ],
            providers: [
                DotEditLayoutService,
                DotThemesService,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotThemesService, useClass: DotThemesServiceMock },
                {
                    provide: DotRouterService,
                    useValue: {
                        goToSiteBrowser: jasmine.createSpy()
                    }
                },
                {
                    provide: DotEventsService,
                    useValue: {
                        notify: jasmine.createSpy(),
                        listen: jasmine.createSpy().and.returnValue(of({}))
                    }
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jasmine.createSpy().and.returnValue(of({}))
                    }
                },
                {
                    provide: DotTemplateContainersCacheService,
                    useValue: {
                        set: jasmine.createSpy
                    }
                },
                {
                    provide: DotGlobalMessageService,
                    useValue: {
                        display: jasmine.createSpy(),
                        loading: jasmine.createSpy(),
                        customDisplay: jasmine.createSpy()
                    }
                }
            ]
        });

        fixture = TestBed.createComponent(DotEditLayoutDesignerComponent);
        component = fixture.componentInstance;
        dotThemesService = TestBed.inject(DotThemesService);
    });

    describe('edit layout', () => {
        beforeEach(() => {
            component.layout = mockDotLayout();
            component.theme = '123';
            component.disablePublish = false;
            fixture.detectChanges();
        });
        // these need to be fixed when this is fixed correctly https://github.com/dotCMS/core/issues/18830
        it('should have dot-secondary-toolbar with right content', () => {
            const dotSecondaryToolbar = fixture.debugElement.query(By.css('dot-secondary-toolbar'));
            const dotEditPageInfo = fixture.debugElement.query(
                By.css('dot-secondary-toolbar .main-toolbar-left dot-edit-page-info')
            );
            const dotLayoutActions = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-action-themes')
            );

            expect(dotSecondaryToolbar).not.toBeNull();
            expect(dotEditPageInfo).not.toBeNull();
            expect(dotLayoutActions).not.toBeNull();
        });
        // these need to be fixed when this is fixed correctly https://github.com/dotCMS/core/issues/18830
        it('should show dot-edit-page-info', () => {
            const dotEditPageInfo: DebugElement = fixture.debugElement.query(
                By.css('dot-edit-page-info')
            );
            expect(dotEditPageInfo).toBeTruthy();
            // TODO: NEED EXTRA EXPECTS
        });

        it('should enable publish button when editing the form.', () => {
            component.form.get('title').setValue('Hello');
            fixture.detectChanges();
            expect(component.disablePublish).toBe(false);
        });

        it('should not show template name input', () => {
            const templateNameInput: DebugElement = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-template-name')
            );
            expect(templateNameInput).toBe(null);
        });

        it('should not show checkbox to save as template', () => {
            const checkboxSave: DebugElement = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-save-template')
            );
            expect(checkboxSave).toBe(null);
        });

        it('should emit pushAndPublish event when button clicked', () => {
            spyOn(component.saveAndPublish, 'emit').and.callThrough();
            fixture.detectChanges();
            const publishButton = fixture.debugElement.query(By.css('[data-testId="publishBtn"]'));
            publishButton.triggerEventHandler('click', null);
            fixture.detectChanges();
            expect(component.saveAndPublish.emit).toHaveBeenCalledWith(
                component.form.value as DotTemplateDesigner
            );
        });

        it('should save changes when editing the form.', () => {
            spyOn(component.updateTemplate, 'emit');
            component.form.get('title').setValue('Hello');
            fixture.detectChanges();
            expect(component.updateTemplate.emit).toHaveBeenCalledTimes(1);
        });

        it('should show dot-layout-properties and bind attr correctly', () => {
            fixture.detectChanges();
            const layoutProperties: DebugElement = fixture.debugElement.query(
                By.css('dot-layout-properties')
            );

            expect(layoutProperties).toBeTruthy();
            expect(layoutProperties.componentInstance.group).toEqual(component.form.get('layout'));
        });

        it('should have dot-layout-designer', () => {
            fixture.detectChanges();
            const layoutDesigner: DebugElement = fixture.debugElement.query(
                By.css('dot-layout-designer')
            );

            expect(layoutDesigner.componentInstance.group.name).toEqual('layout');
        });

        it('should not show dot-template-addtional-actions-menu', () => {
            const aditionalOptions: DebugElement = fixture.debugElement.query(
                By.css('dot-template-addtional-actions-menu')
            );

            expect(aditionalOptions).toBe(null);
        });

        it('should not show save as template checkbox', () => {
            const saveAsTemplate: DebugElement = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-save-template')
            );

            expect(saveAsTemplate).toBe(null);
        });

        it('should set form model correctly', () => {
            expect(component.form.value).toEqual({
                title: '',
                themeId: '123',
                layout: {
                    body: mockDotRenderedPage().layout.body,
                    header: mockDotRenderedPage().layout.header,
                    footer: mockDotRenderedPage().layout.footer,
                    sidebar: {
                        location: mockDotRenderedPage().layout.sidebar.location,
                        containers: mockDotRenderedPage().layout.sidebar.containers,
                        width: mockDotRenderedPage().layout.sidebar.width
                    },
                    title: mockDotRenderedPage().layout.title,
                    width: mockDotRenderedPage().layout.width
                }
            });
        });
    });

    describe('themes', () => {
        let themeSelector: DotThemeSelectorMockComponent;
        let themeButton;

        beforeEach(() => {
            component.layout = mockDotLayout();
            component.themeDialogVisibility = true;
        });

        it('should expose theme selector component & Theme button be enabled', () => {
            fixture.detectChanges();
            themeButton = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-action-themes')
            ).nativeElement;
            themeButton.click();
            themeSelector = fixture.debugElement.query(
                By.css('dot-theme-selector')
            ).componentInstance;

            expect(themeSelector).not.toBe(null);
        });

        it('should Theme button be disabled', () => {
            spyOn(dotThemesService, 'get').and.returnValue(observableOf(null));
            fixture.detectChanges();
            const themeSelectorBtn = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-action-themes')
            );
            expect(themeSelectorBtn.nativeElement.disabled).toBe(true);
            expect(themeSelectorBtn.attributes['ng-reflect-text']).toBe('No Read Permission');
        });

        it('should get the emitted value from themes and trigger a save', () => {
            spyOn(component, 'changeThemeHandler').and.callThrough();
            fixture.detectChanges();
            themeButton = fixture.debugElement.query(
                By.css('.dot-edit-layout__toolbar-action-themes')
            ).nativeElement;
            themeButton.click();
            themeSelector = fixture.debugElement.query(
                By.css('dot-theme-selector')
            ).componentInstance;
            const mockTheme = _.cloneDeep(mockDotThemes[0]);
            themeSelector.selected.emit(mockTheme);
            expect(component.changeThemeHandler).toHaveBeenCalledWith(mockTheme);
        });
    });

    describe('containers model', () => {
        beforeEach(() => {
            component.layout = mockDotLayout();
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
            const layout = mockDotLayout();

            component.layout = {
                ...layout,
                sidebar: {
                    ...layout.sidebar,
                    containers: []
                }
            };
            fixture.detectChanges();
            expect(component.form.value.layout.sidebar.containers).toEqual([]);
        });
    });

    describe('edit layout No sidebars', () => {
        beforeEach(() => {
            const layout = mockDotLayout();

            component.layout = {
                ...layout,
                sidebar: null
            };
            component.theme = '123';
            fixture.detectChanges();
        });

        it('should not break when sidebar property in layout is null', () => {
            expect(component.form.value).toEqual({
                title: '',
                themeId: '123',
                layout: {
                    body: mockDotRenderedPage().layout.body,
                    header: mockDotRenderedPage().layout.header,
                    footer: mockDotRenderedPage().layout.footer,
                    sidebar: {
                        location: '',
                        containers: [],
                        width: 'small'
                    },
                    title: '',
                    width: ''
                }
            });
        });
    });

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
