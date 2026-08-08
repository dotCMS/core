/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

import { EMPTY, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { MenuItem } from 'primeng/api';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabsModule } from 'primeng/tabs';

import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageService,
    DotRouterService,
    DotSystemConfigService,
    DotUiColorsService
} from '@dotcms/data-access';
import { LoggerService, LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentType, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotIconComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    createFakeEvent,
    dotcmsContentTypeBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { ContentTypesLayoutComponent } from './content-types-layout.component';

import { DotAddToMenuService } from '../../../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotInlineEditComponent } from '../../../../../view/components/_common/dot-inline-edit/dot-inline-edit.component';
import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { IframeOverlayService } from '../../../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotCopyLinkComponent } from '../../../../../view/components/dot-copy-link/dot-copy-link.component';
import { DotPortletBoxComponent } from '../../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotAddToMenuComponent } from '../../../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.component';
import { FieldDragDropService } from '../fields/service';
import { DotStyleEditorBuilderComponent } from '../style-editor/dot-style-editor-builder.component';

@Component({
    selector: 'dot-content-types-fields-list',
    template: '',
    standalone: false
})
class TestContentTypeFieldsListComponent {
    @Input() baseType: string;
}

@Component({
    selector: 'dot-content-type-fields-row-list',
    template: '',
    standalone: false
})
class TestContentTypeFieldsRowListComponent {}

@Component({
    selector: 'dot-iframe',
    template: ''
})
class TestDotIframeComponent {
    @Input() src: string;
}

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-content-type-layout [contentType]="contentType"></dot-content-type-layout>',
    standalone: false
})
class TestHostComponent {
    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-style-editor-builder',
    template: '',
    standalone: true
})
class MockDotStyleEditorBuilderComponent {
    @Input() contentType: DotCMSContentType;
}

class FieldDragDropServiceMock {
    setBagOptions() {}
}

const fakeContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    icon: 'testIcon',
    id: '1234567890',
    name: 'name',
    variable: 'helloVariable',
    baseType: 'testBaseType'
};

describe('ContentTypesLayoutComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.sidebar.components.title': 'Field Title',
            'contenttypes.tab.fields.header': 'Fields Header Tab',
            'contenttypes.sidebar.layouts.title': 'Layout Title',
            'contenttypes.tab.permissions.header': 'Permissions Tab',
            'contenttypes.tab.publisher.push.history.header': 'Push History',
            'contenttypes.action.edit': 'Edit',
            'contenttypes.content.variable': 'Variable',
            'contenttypes.form.identifier': 'Identifier',
            'contenttypes.dropzone.rows.add': 'Add Row',
            'contenttypes.content.row': 'Row',
            'contenttypes.content.add_to_menu': 'Add To Menu',
            'contenttypes.content.add_to_menu.header': 'Add to Menu',
            'contenttypes.content.add_to_menu.name': 'Name',
            'contenttypes.content.add_to_menu.show_under': 'Show under',
            'contenttypes.content.add_to_menu.default_view': 'Default view',
            'custom.content.portlet.dataViewMode.card': 'card',
            'custom.content.portlet.dataViewMode.list': 'list',
            add: 'Add',
            cancel: 'Cancel'
        });

        TestBed.configureTestingModule({
            declarations: [
                TestContentTypeFieldsListComponent,
                TestContentTypeFieldsRowListComponent,
                TestHostComponent
            ],
            imports: [
                ContentTypesLayoutComponent,
                TabsModule,
                DotIconComponent,
                RouterTestingModule,
                BrowserAnimationsModule,
                DotApiLinkComponent,
                DotCopyLinkComponent,
                DotSafeHtmlPipe,
                DotMessagePipe,
                SplitButtonModule,
                DotInlineEditComponent,
                DotPortletBoxComponent,
                DotCopyButtonComponent
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: FieldDragDropService, useClass: FieldDragDropServiceMock },
                DotCurrentUserService,
                DotEventsService,
                DotAddToMenuService,
                {
                    provide: DotMenuService,
                    useValue: {
                        loadMenu: jest.fn().mockReturnValue(
                            of([
                                {
                                    id: '123',
                                    name: 'Menu 1',
                                    label: 'Menu 1',
                                    tabName: 'Name',
                                    tabDescription: 'Description',
                                    tabIcon: 'icon',
                                    url: '/url/index',
                                    active: false,
                                    isOpen: false,
                                    menuItems: []
                                }
                            ])
                        )
                    }
                },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                {
                    provide: DotIframeService,
                    useValue: {
                        reloadData: jest.fn(),
                        reloaded: jest.fn().mockReturnValue(of({})),
                        ran: jest.fn().mockReturnValue(of({})),
                        reloadedColors: jest.fn().mockReturnValue(of({}))
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: { currentPortlet: { id: 'test-portlet-id' } }
                },
                { provide: DotUiColorsService, useValue: { setColors: jest.fn() } },
                {
                    provide: DotLoadingIndicatorService,
                    useValue: {
                        display: false,
                        show: jest.fn(),
                        hide: jest.fn()
                    }
                },
                {
                    provide: IframeOverlayService,
                    useValue: {
                        overlay: of(false),
                        show: jest.fn(),
                        hide: jest.fn(),
                        toggle: jest.fn()
                    }
                },
                { provide: LoggerService, useValue: { debug: jest.fn(), error: jest.fn() } },
                { provide: LoginService, useValue: { isLogin$: of(true) } },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: { handle: jest.fn().mockReturnValue(of({})) }
                },
                {
                    provide: DotAlertConfirmService,
                    useValue: { confirm: jest.fn(), alert: jest.fn() }
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            data: {
                                featuredFlags: {
                                    [FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR]: true
                                },
                                tabPermissions: { showPermissionsTab: true }
                            }
                        },
                        firstChild: null,
                        events: EMPTY
                    }
                }
            ]
        }).overrideComponent(ContentTypesLayoutComponent, {
            remove: {
                imports: [IframeComponent, DotStyleEditorBuilderComponent]
            },
            add: {
                imports: [TestDotIframeComponent, MockDotStyleEditorBuilderComponent]
            }
        });

        await TestBed.compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        const originalDetectChanges = fixture.detectChanges.bind(fixture);
        fixture.detectChanges = (_checkNoChanges?: boolean) => originalDetectChanges(false);
        de = fixture.debugElement.query(By.css('dot-content-type-layout'));
    });

    it('should have a tab-view', () => {
        const pTabs = de.query(By.css('p-tabs'));

        expect(pTabs).not.toBeNull();
    });

    it('should have just one tab', () => {
        const pTabPanels = fixture.debugElement.queryAll(By.css('p-tabpanel'));
        expect(pTabPanels.length).toBe(1);
    });

    it('should not have a Permissions tab', () => {
        const pTabPanel = de.query(By.css('p-tabpanel[value="permissions"]'));
        expect(pTabPanel).toBeFalsy();
    });

    it('should set the field and row bag options', () => {
        const fieldDragDropService: FieldDragDropService =
            fixture.debugElement.injector.get(FieldDragDropService);
        fixture.componentRef.setInput('contentType', fakeContentType);
        jest.spyOn(fieldDragDropService, 'setBagOptions');
        fixture.detectChanges();
        expect(fieldDragDropService.setBagOptions).toHaveBeenCalledTimes(1);
    });

    it('should navigate to the route and immediately update $activeTab when clicking a tab', () => {
        fixture.componentRef.setInput('contentType', fakeContentType);
        fixture.detectChanges();

        const router = fixture.debugElement.injector.get(Router);
        jest.spyOn(router, 'navigate');

        de.componentInstance.onTabChange('permissions');

        expect(de.componentInstance.$activeTab()).toBe('permissions');
        expect(router.navigate).toHaveBeenCalledWith(
            ['permissions'],
            expect.objectContaining({ relativeTo: expect.anything() })
        );
    });

    describe('Edit toolBar', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('contentType', fakeContentType);
            fixture.detectChanges();
        });

        it('should have the Settings button', () => {
            const settingsBtn = de.query(By.css('[data-testid="settings-btn"]'));
            expect(settingsBtn).not.toBeNull();
        });

        it('should have open Add to Menu Dialog and close', () => {
            de.componentInstance.addContentInMenu();
            fixture.detectChanges();

            expect(de.componentInstance.$addToMenuContentType()).toBe(true);

            const addToMenuEl = de.query(By.css('dot-add-to-menu'));
            expect(addToMenuEl).toBeTruthy();

            const addToMenuDialog = addToMenuEl!.componentInstance as DotAddToMenuComponent;
            addToMenuDialog.cancel.emit(true);
            fixture.detectChanges();

            expect(de.query(By.css('dot-add-to-menu'))).toBeFalsy();
            expect(de.componentInstance.$addToMenuContentType()).toBe(false);
        });
    });

    describe('Tabs', () => {
        let iframe: DebugElement;

        beforeEach(() => {
            fixture.componentRef.setInput('contentType', fakeContentType);
            fixture.detectChanges();
        });

        describe('Fields', () => {
            let pTabPanel;
            beforeEach(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                pTabPanel = panels[0];
            });

            it('should have a field panel', () => {
                expect(pTabPanel).not.toBeNull();
                const tabs = de.queryAll(By.css('p-tab'));
                expect(tabs.length).toBeGreaterThan(0);
            });

            it('should have a content-type__fields-main', () => {
                const contentTypeFieldsMain = pTabPanel.query(By.css('#content-type-form-main'));
                expect(contentTypeFieldsMain).not.toBeNull();
            });

            it('should have a content-type__fields-sidebar', () => {
                // Updated: sidebar now contains the splitbutton and fields list
                const contentTypeFieldsSideBar = pTabPanel.query(
                    By.css('dot-content-types-fields-list')
                );
                expect(contentTypeFieldsSideBar).not.toBeNull();
            });

            it('should have a field types list with the correct params', () => {
                const contentTypesFieldsList = pTabPanel.query(
                    By.css('dot-content-types-fields-list')
                );
                expect(contentTypesFieldsList).not.toBeNull();
                expect(contentTypesFieldsList.componentInstance.baseType).toEqual('testBaseType');
            });

            // Hiding the rows list for 5.0
            xit('should have a field row list', () => {
                const fieldRowList = pTabPanel.query(By.css('dot-content-type-fields-row-list'));
                expect(fieldRowList).not.toBeNull();
            });

            describe('Add Row Button', () => {
                let splitButton: DebugElement;
                let dotEventsService: DotEventsService;

                beforeEach(() => {
                    splitButton = pTabPanel.query(By.css('p-splitbutton'));
                    dotEventsService = fixture.debugElement.injector.get(DotEventsService);
                    jest.spyOn(dotEventsService, 'notify');
                });

                it('should have the correct label', () => {
                    expect(splitButton.componentInstance.label).toEqual('Row');
                });

                it('should have the correct icon', () => {
                    expect(splitButton.componentInstance.icon).toEqual('pi pi-plus');
                });

                it('should fire event service with add row ', () => {
                    const button = splitButton.query(By.css('button'));
                    button.nativeElement.click();
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-row');
                    expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
                });

                it('should set actions correctly', () => {
                    const addRow: MenuItem = splitButton.componentInstance.model[0];
                    const addTabDivider: MenuItem = splitButton.componentInstance.model[1];
                    addRow.command({ originalEvent: createFakeEvent('click') });
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-row');
                    expect(dotEventsService.notify).toHaveBeenCalledTimes(1);

                    // Clear the mock before the second call
                    (dotEventsService.notify as jest.Mock).mockClear();

                    addTabDivider.command({ originalEvent: createFakeEvent('click') });
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
                    expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
                });
            });
        });

        describe('Permission', () => {
            let pTabPanel;
            beforeEach(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                // panels[0]=Fields, [1]=StyleEditor, [2]=Permissions
                pTabPanel = panels[2];
                fixture.detectChanges();
                iframe = pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(pTabPanel).not.toBeNull();
                const tabs = de.queryAll(By.css('p-tab'));
                expect(tabs.length).toBeGreaterThanOrEqual(3);
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe(
                    '/html/content_types/permissions.jsp?contentTypeId=1234567890&popup=true'
                );
            });
        });

        describe('Push History', () => {
            let pTabPanel;
            beforeEach(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                // panels[0]=Fields, [1]=StyleEditor, [2]=Permissions, [3]=PushHistory
                pTabPanel = panels[3];
                fixture.detectChanges();
                iframe = pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(pTabPanel).not.toBeNull();
                const tabs = de.queryAll(By.css('p-tab'));
                expect(tabs.length).toBeGreaterThanOrEqual(3);
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe(
                    '/html/content_types/push_history.jsp?contentTypeId=1234567890&popup=true'
                );
            });
        });

        describe('Style Editor tab', () => {
            it('should show the style editor tab when feature flag is enabled', () => {
                const styleEditorPanel = de.query(By.css('[data-testid="style-editor-panel"]'));
                const styleEditorTab = de.query(By.css('[data-testid="style-editor-tab"]'));

                expect(styleEditorPanel).not.toBeNull();
                expect(styleEditorTab).not.toBeNull();
            });

            it('should render dot-style-editor-builder inside the style editor panel', () => {
                const styleEditorBuilder = de.query(By.css('dot-style-editor-builder'));

                expect(styleEditorBuilder).not.toBeNull();
                expect(styleEditorBuilder.componentInstance.contentType).toEqual(fakeContentType);
            });

            it('should hide the style editor tab when feature flag is disabled', () => {
                de.componentInstance.$showStyleEditorTab.set(false);
                fixture.detectChanges();

                const styleEditorPanel = de.query(By.css('[data-testid="style-editor-panel"]'));
                const styleEditorTab = de.query(By.css('[data-testid="style-editor-tab"]'));

                expect(styleEditorPanel).toBeNull();
                expect(styleEditorTab).toBeNull();
            });
        });
    });
});
