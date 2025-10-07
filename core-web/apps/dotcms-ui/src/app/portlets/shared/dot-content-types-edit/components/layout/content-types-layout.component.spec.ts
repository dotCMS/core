/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { MenuItem } from 'primeng/api';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabViewModule } from 'primeng/tabview';

import { DotCurrentUserService, DotEventsService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotIconModule,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    createFakeEvent,
    dotcmsContentTypeBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { ContentTypesLayoutComponent } from './content-types-layout.component';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotInlineEditModule } from '../../../../../view/components/_common/dot-inline-edit/dot-inline-edit.module';
import { DotCopyLinkModule } from '../../../../../view/components/dot-copy-link/dot-copy-link.module';
import { DotPortletBoxModule } from '../../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';
import { DotSecondaryToolbarModule } from '../../../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.module';
import { FieldDragDropService } from '../fields/service';

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
    template: '',
    standalone: false
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
    standalone: false
})
class MockDotAddToMenuComponent {
    @Input() contentType: DotCMSContentType;
    @Output() cancel = new EventEmitter<boolean>();
}

@Injectable()
export class MockDotMenuService {
    getDotMenuId(): Observable<string> {
        return of('1234');
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
                ContentTypesLayoutComponent,
                TestContentTypeFieldsListComponent,
                TestContentTypeFieldsRowListComponent,
                TestDotIframeComponent,
                TestContentTypesRelationshipListingComponent,
                TestHostComponent,
                MockDotAddToMenuComponent
            ],
            imports: [
                TabViewModule,
                DotIconModule,
                DotSecondaryToolbarModule,
                RouterTestingModule,
                DotApiLinkComponent,
                DotCopyLinkModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                SplitButtonModule,
                DotInlineEditModule,
                HttpClientTestingModule,
                DotPortletBoxModule,
                DotCopyButtonComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotMenuService, useClass: MockDotMenuService },
                { provide: FieldDragDropService, useClass: FieldDragDropServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotCurrentUserService,
                DotEventsService
            ]
        });

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-content-type-layout'));
    });

    it('should have a tab-view', () => {
        const pTabView = de.query(By.css('p-tabview'));

        expect(pTabView).not.toBeNull();
    });

    it('should have just one tab', () => {
        const pTabPanels = fixture.debugElement.queryAll(By.css('p-tabpanel'));
        expect(pTabPanels.length).toBe(1);
    });

    it('should not have a Permissions tab', () => {
        const pTabPanel = de.query(By.css('.content-type__permissions'));
        expect(pTabPanel).toBeFalsy();
    });

    it('should set the field and row bag options', () => {
        const fieldDragDropService: FieldDragDropService =
            fixture.debugElement.injector.get(FieldDragDropService);
        fixture.componentInstance.contentType = fakeContentType;
        jest.spyOn(fieldDragDropService, 'setBagOptions');
        fixture.detectChanges();
        expect(fieldDragDropService.setBagOptions).toHaveBeenCalledTimes(1);
    });

    it('should have dot-portlet-box in the second tab after it has been clicked', fakeAsync(() => {
        fixture.componentInstance.contentType = fakeContentType;

        fixture.detectChanges();

        const contentTypeRelationshipsTabLink = de.query(
            By.css('ul.p-tabview-nav li:nth-child(2) > a')
        );
        contentTypeRelationshipsTabLink.nativeElement.click();
        fixture.detectChanges();

        fixture.whenStable().then(() => {
            const contentTypeRelationships = de.query(By.css('.content-type__relationships'));
            const contentTypeRelationshipsPortletBox = contentTypeRelationships.query(
                By.css('dot-portlet-box')
            );
            expect(contentTypeRelationshipsPortletBox).not.toBeNull();
        });
    }));

    it('should have dot-portlet-box in the fourth tab after it has been clicked', fakeAsync(() => {
        fixture.componentInstance.contentType = fakeContentType;
        fixture.detectChanges();

        const contentTypePushHistoryTabLink = de.query(
            By.css('ul.p-tabview-nav li:nth-child(3) > a')
        );
        contentTypePushHistoryTabLink.nativeElement.click();
        fixture.detectChanges();

        fixture.whenStable().then(() => {
            const contentTypePushHistory = de.query(By.css('.content-type__push_history'));
            const contentTypePushHistoryPortletBox = contentTypePushHistory.query(
                By.css('dot-portlet-box')
            );
            expect(contentTypePushHistoryPortletBox).not.toBeNull();
        });
    }));

    describe('Edit toolBar', () => {
        beforeEach(() => {
            fixture.componentInstance.contentType = fakeContentType;
            fixture.detectChanges();
        });

        it('should have dot-secondary-toolbar', () => {
            expect(de.query(By.css('dot-secondary-toolbar'))).toBeDefined();
        });

        it('should have elements in the correct place', () => {
            expect(
                de.query(By.css('.main-toolbar-left header dot-icon')).componentInstance.name
            ).toBe(fakeContentType.icon);
            expect(de.query(By.css('.main-toolbar-left header dot-inline-edit'))).toBeDefined();
            expect(
                de.query(By.css('.main-toolbar-left header p-inplace h4')).nativeElement.innerHTML
            ).toBe(fakeContentType.name);
            expect(de.query(By.css('.main-toolbar-left .content-type__title'))).toBeDefined();
            expect(de.query(By.css('.main-toolbar-left .content-type__info'))).toBeDefined();
            expect(de.query(By.css('.main-toolbar-right #form-edit-button'))).toBeDefined();
            expect(de.query(By.css('.main-toolbar-right #add-to-menu-button'))).toBeDefined();
        });

        it('should set and emit change name of Content Type', () => {
            de.query(By.css('.main-toolbar-left header p-inplace h4')).nativeElement.click();
            fixture.detectChanges();

            const dotInlineEditComp = de.query(
                By.css('.main-toolbar-left header dot-inline-edit')
            ).componentInstance;

            jest.spyOn(de.componentInstance.changeContentTypeName, 'emit');
            jest.spyOn(dotInlineEditComp, 'hideContent');

            expect(de.query(By.css('.main-toolbar-left header p-inplace input'))).toBeDefined();
            de.query(By.css('.main-toolbar-left header p-inplace input')).nativeElement.value =
                'changedName';
            de.query(By.css('.main-toolbar-left header p-inplace input')).triggerEventHandler(
                'keyup',
                {
                    stopPropagation: jest.fn(),
                    key: 'Enter'
                }
            );
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
            expect(editButton.nativeElement.textContent).toBe('Edit');
            expect(editButton.nativeElement.disabled).toBe(false);
            expect(editButton).toBeTruthy();
        });

        it('should have Add To Menu button', () => {
            const addToMenuButton: DebugElement = fixture.debugElement.query(
                By.css('#add-to-menu-button')
            );
            expect(addToMenuButton.nativeElement.textContent).toBe('Add To Menu');
            expect(addToMenuButton.nativeElement.disabled).toBe(false);
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
            AddToMenuDialog.cancel.emit();
            fixture.detectChanges();
            expect(de.query(By.css('dot-add-to-menu'))).toBeFalsy();
            expect(de.componentInstance.addToMenuContentType).toBe(false);
        });
    });

    describe('Tabs', () => {
        let iframe: DebugElement;
        let dotCurrentUserService: DotCurrentUserService;

        beforeEach(() => {
            fixture.componentInstance.contentType = fakeContentType;
            dotCurrentUserService = fixture.debugElement.injector.get(DotCurrentUserService);
            jest.spyOn(dotCurrentUserService, 'hasAccessToPortlet').mockReturnValue(of(true));

            fixture.detectChanges();
        });

        describe('Fields', () => {
            let pTabPanel;
            beforeEach(() => {
                pTabPanel = de.query(By.css('.content-type__properties'));
                pTabPanel.componentInstance.selected = true;
            });

            it('should have a field panel', () => {
                expect(pTabPanel).not.toBeNull();
                expect(pTabPanel.componentInstance.header).toBe('Fields Header Tab');
            });

            it('should have a content-type__fields-main', () => {
                const contentTypeFieldsMain = pTabPanel.query(By.css('.content-type__fields-main'));
                expect(contentTypeFieldsMain).not.toBeNull();
            });

            it('should have a content-type__fields-sidebar', () => {
                const contentTypeFieldsSideBar = pTabPanel.query(
                    By.css('.content-type__fields-sidebar')
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
                const layoutTitle = pTabPanel.queryAll(
                    By.css('.content-type__fields-sidebar-title')
                )[1];
                const fieldRowList = pTabPanel.query(By.css('dot-content-type-fields-row-list'));

                expect(layoutTitle.nativeElement.textContent).toBe('Layout Title');
                expect(fieldRowList).not.toBeNull();
            });

            describe('Add Row Button', () => {
                let splitButton: DebugElement;
                let dotEventsService: DotEventsService;

                beforeEach(() => {
                    splitButton = pTabPanel.query(
                        By.css('.content-type__fields-sidebar p-splitbutton')
                    );
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
                    dotEventsService.notify.mockClear();

                    addTabDivider.command({ originalEvent: createFakeEvent('click') });
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
                    expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
                });
            });
        });

        describe('Permission', () => {
            let pTabPanel;
            beforeEach(() => {
                pTabPanel = de.query(By.css('.content-type__permissions'));
                pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(pTabPanel).not.toBeNull();
                expect(pTabPanel.componentInstance.header).toBe('Permissions Tab');
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
                pTabPanel = de.query(By.css('.content-type__push_history'));
                pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(pTabPanel).not.toBeNull();
                expect(pTabPanel.componentInstance.header).toBe('Push History');
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
                pTabPanel = de.query(By.css('.content-type__relationships'));
                pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a Relationship tab', () => {
                expect(pTabPanel).toBeDefined();
            });

            it('should have a right header', () => {
                expect(pTabPanel.componentInstance.header).toBe('Relationship');
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe(
                    // tslint:disable-next-line:max-line-length
                    'c/portal/layout?p_l_id=1234&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=1234567890'
                );
            });
        });
    });
});
