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

import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
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
    DotUiColorsService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsEventsService,
    LoggerService,
    LoginService
} from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotIconComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
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
import { DotSecondaryToolbarComponent } from '../../../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotAddToMenuComponent } from '../../../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.component';
import { FieldDragDropService, FieldService } from '../fields/service';

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
    selector: 'dot-content-types-relationship-listing',
    template: '',
    standalone: false
})
class TestContentTypesRelationshipListingComponent {}

@Component({
    selector: 'dot-add-to-menu',
    template: ``,
    standalone: true
})
class MockDotAddToMenuComponent {
    @Input() contentType: DotCMSContentType;
    @Output('cancel') $cancel = new EventEmitter<boolean>();
}

@Injectable()
export class MockDotMenuService {
    getDotMenuId(): Observable<string> {
        return of('1234');
    }

    loadMenu(_reload?: boolean): Observable<any> {
        return of([{ id: 'menu-1' }]);
    }
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

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.sidebar.components.title': 'Field Title',
            'contenttypes.tab.fields.header': 'Fields Header Tab',
            'contenttypes.sidebar.layouts.title': 'Layout Title',
            'contenttypes.tab.permissions.header': 'Permissions Tab',
            'contenttypes.tab.publisher.push.history.header': 'Push History',
            'contenttypes.tab.relationship.header': 'Relationship',
            'contenttypes.action.edit': 'Edit',
            'contenttypes.content.variable': 'Variable',
            'contenttypes.form.identifier': 'Identifier',
            'contenttypes.dropzone.rows.add': 'Add Row',
            'contenttypes.content.row': 'Row',
            'contenttypes.content.add_to_menu': 'Add To Menu'
        });

        TestBed.configureTestingModule({
            declarations: [
                TestContentTypeFieldsListComponent,
                TestContentTypeFieldsRowListComponent,
                TestContentTypesRelationshipListingComponent,
                TestHostComponent
            ],
            imports: [
                ContentTypesLayoutComponent,
                TabsModule,
                DotIconComponent,
                DotSecondaryToolbarComponent,
                RouterTestingModule,
                DotApiLinkComponent,
                DotCopyLinkComponent,
                DotSafeHtmlPipe,
                DotMessagePipe,
                SplitButtonModule,
                DotInlineEditComponent,
                HttpClientTestingModule,
                DotPortletBoxComponent,
                DotCopyButtonComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotMenuService, useClass: MockDotMenuService },
                { provide: FieldDragDropService, useClass: FieldDragDropServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotCurrentUserService,
                DotEventsService,
                DotAddToMenuService,
                FieldService,
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
                    provide: DotcmsEventsService,
                    useValue: {
                        subscribeTo: jest.fn().mockReturnValue(of({})),
                        subscribeToEvents: jest.fn().mockReturnValue(of({}))
                    }
                },
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
                }
            ]
        });

        // Override ContentTypesLayoutComponent to use the mock IframeComponent
        TestBed.overrideComponent(ContentTypesLayoutComponent, {
            remove: { imports: [IframeComponent, DotAddToMenuComponent] },
            add: { imports: [TestDotIframeComponent, MockDotAddToMenuComponent] }
        });

        fixture = TestBed.createComponent(TestHostComponent);
        const originalDetectChanges = fixture.detectChanges.bind(fixture);
        fixture.detectChanges = (checkNoChanges?: boolean) => originalDetectChanges(false);
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
        const pTabPanel = de.query(By.css('p-tabpanel[value="2"]'));
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

    it('should have dot-portlet-box in the second tab after it has been clicked', fakeAsync(() => {
        fixture.componentRef.setInput('contentType', fakeContentType);

        fixture.detectChanges();

        const tabs = de.queryAll(By.css('p-tab'));
        if (tabs.length > 1) {
            tabs[1].nativeElement.click();
            fixture.detectChanges();

            fixture.whenStable().then(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                if (panels.length > 1) {
                    const contentTypeRelationshipsPortletBox = panels[1].query(
                        By.css('dot-portlet-box')
                    );
                    expect(contentTypeRelationshipsPortletBox).not.toBeNull();
                }
            });
        }
    }));

    it('should have dot-portlet-box in the fourth tab after it has been clicked', fakeAsync(() => {
        fixture.componentRef.setInput('contentType', fakeContentType);
        fixture.detectChanges();

        const tabs = de.queryAll(By.css('p-tab'));
        const tabIndex = tabs.length > 3 ? 3 : 2; // Use last tab
        if (tabs.length > tabIndex) {
            tabs[tabIndex].nativeElement.click();
            fixture.detectChanges();

            fixture.whenStable().then(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                if (panels.length > tabIndex) {
                    const contentTypePushHistoryPortletBox = panels[tabIndex].query(
                        By.css('dot-portlet-box')
                    );
                    expect(contentTypePushHistoryPortletBox).not.toBeNull();
                }
            });
        }
    }));

    describe('Edit toolBar', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('contentType', fakeContentType);
            fixture.detectChanges();
        });

        it('should have dot-secondary-toolbar', () => {
            expect(de.query(By.css('dot-secondary-toolbar'))).toBeDefined();
        });

        it('should have elements in the correct place', () => {
            // Updated selectors for new template structure
            expect(de.query(By.css('header dot-inline-edit'))).toBeDefined();
            expect(de.query(By.css('#form-edit-button'))).toBeDefined();
            expect(de.query(By.css('#add-to-menu-button'))).toBeDefined();
        });

        it('should set and emit change name of Content Type', () => {
            // Updated selectors for new template structure
            const header = de.query(By.css('header'));
            const inlineEditDisplay = header.query(By.css('h4'));
            inlineEditDisplay.nativeElement.click();
            fixture.detectChanges();

            const dotInlineEditComp = de.query(By.css('header dot-inline-edit')).componentInstance;

            jest.spyOn(de.componentInstance.changeContentTypeName, 'emit');
            jest.spyOn(dotInlineEditComp, 'hideContent');

            const inputElement = header.query(By.css('input'));
            expect(inputElement).toBeDefined();
            inputElement.nativeElement.value = 'changedName';
            inputElement.triggerEventHandler('keyup', {
                stopPropagation: jest.fn(),
                key: 'Enter'
            });
            expect(de.componentInstance.changeContentTypeName.emit).toHaveBeenCalledWith(
                'changedName'
            );
            expect(dotInlineEditComp.hideContent).toHaveBeenCalledTimes(1);
        });

        it('should have api link component', () => {
            expect(de.query(By.css('dot-api-link'))).toBeDefined();
        });

        it('should have copy variable link', () => {
            expect(de.query(By.css('[data-testId="copyVariableName"]'))).toBeDefined();
        });

        it('should have copy identifier link', () => {
            expect(de.query(By.css('[data-testId="copyIdentifier"]'))).toBeDefined();
        });

        it('should have edit button', () => {
            const editButton: DebugElement = fixture.debugElement.query(
                By.css('#form-edit-button')
            );
            expect(editButton.nativeElement.textContent).toContain('Edit');
            expect(editButton.componentInstance.disabled).toBeFalsy();
            expect(editButton).toBeTruthy();
        });

        it('should have Add To Menu button', () => {
            const addToMenuButton: DebugElement = fixture.debugElement.query(
                By.css('#add-to-menu-button')
            );
            expect(addToMenuButton.nativeElement.textContent).toContain('Add To Menu');
            expect(addToMenuButton.componentInstance.disabled).toBeFalsy();
            expect(addToMenuButton).toBeTruthy();
        });

        it('should have open Add to Menu Dialog and close', () => {
            jest.spyOn(de.componentInstance, 'addContentInMenu');
            fixture.debugElement.query(By.css('#add-to-menu-button')).triggerEventHandler('click');
            fixture.detectChanges();
            expect(de.componentInstance.addContentInMenu).toHaveBeenCalled();
            expect(de.componentInstance.addToMenuContentType).toBe(true);
            const AddToMenuDialog: MockDotAddToMenuComponent = de.query(
                By.css('dot-add-to-menu')
            ).componentInstance;
            expect(de.query(By.css('dot-add-to-menu'))).toBeTruthy();
            AddToMenuDialog.$cancel.emit(true);
            de.componentInstance.addToMenuContentType = false;
            fixture.detectChanges();
            expect(de.query(By.css('dot-add-to-menu'))).toBeFalsy();
            expect(de.componentInstance.addToMenuContentType).toBe(false);
        });
    });

    describe('Tabs', () => {
        let iframe: DebugElement;
        let dotCurrentUserService: DotCurrentUserService;

        beforeEach(() => {
            fixture.componentRef.setInput('contentType', fakeContentType);
            dotCurrentUserService = fixture.debugElement.injector.get(DotCurrentUserService);
            jest.spyOn(dotCurrentUserService, 'hasAccessToPortlet').mockReturnValue(of(true));

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
                pTabPanel = panels.length > 2 ? panels[2] : null;
                if (pTabPanel) {
                    fixture.detectChanges();
                    iframe = pTabPanel.query(By.css('dot-iframe'));
                }
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
                pTabPanel = panels.length > 3 ? panels[3] : panels.length > 2 ? panels[2] : null;
                if (pTabPanel) {
                    fixture.detectChanges();
                    iframe = pTabPanel.query(By.css('dot-iframe'));
                }
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

        describe('Relationship', () => {
            let pTabPanel;
            beforeEach(() => {
                const panels = de.queryAll(By.css('p-tabpanel'));
                pTabPanel = panels.length > 1 ? panels[1] : null;
                if (pTabPanel) {
                    fixture.detectChanges();
                    iframe = pTabPanel.query(By.css('dot-iframe'));
                }
            });

            it('should have a Relationship tab', () => {
                expect(pTabPanel).toBeDefined();
            });

            it('should have a right header', () => {
                const tabs = de.queryAll(By.css('p-tab'));
                expect(tabs.length).toBeGreaterThanOrEqual(2);
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe(
                    // tslint:disable-next-line:max-line-length
                    '/c/portal/layout?p_l_id=1234&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=1234567890'
                );
            });
        });
    });
});
