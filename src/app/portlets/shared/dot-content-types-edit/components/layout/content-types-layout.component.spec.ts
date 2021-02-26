import { of, Observable } from 'rxjs';
import { ContentTypesLayoutComponent } from './content-types-layout.component';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input, Injectable, Output, EventEmitter } from '@angular/core';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { DotMenuService } from '@services/dot-menu.service';
import { FieldDragDropService } from '../fields/service';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { RouterTestingModule } from '@angular/router/testing';
import { DotCMSContentType } from 'dotcms-models';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TabViewModule } from 'primeng/tabview';
import { SplitButtonModule } from 'primeng/splitbutton';
import { MenuItem } from 'primeng/api';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@Component({
    selector: 'dot-content-types-fields-list',
    template: ''
})
class TestContentTypeFieldsListComponent {
    @Input() baseType: string;
}

@Component({
    selector: 'dot-content-type-fields-row-list',
    template: ''
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
    template: '<dot-content-type-layout [contentType]="contentType"></dot-content-type-layout>'
})
class TestHostComponent {
    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-content-types-relationship-listing',
    template: ''
})
class TestContentTypesRelationshipListingComponent {}

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
            'contenttypes.content.row': 'Row'
        });

        TestBed.configureTestingModule({
            declarations: [
                ContentTypesLayoutComponent,
                TestContentTypeFieldsListComponent,
                TestContentTypeFieldsRowListComponent,
                TestDotIframeComponent,
                TestContentTypesRelationshipListingComponent,
                TestHostComponent
            ],
            imports: [
                TabViewModule,
                DotIconModule,
                DotSecondaryToolbarModule,
                RouterTestingModule,
                DotApiLinkModule,
                DotCopyButtonModule,
                DotPipesModule,
                SplitButtonModule,
                HttpClientTestingModule,
                DotPortletBoxModule
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
        const pTabView = de.query(By.css('p-tabView'));

        expect(pTabView).not.toBeNull();
    });

    it('should have just one tab', () => {
        const pTabPanels = fixture.debugElement.queryAll(By.css('p-tabPanel'));
        expect(pTabPanels.length).toBe(1);
    });

    it('should not have a Permissions tab', () => {
        let pTabPanel = de.query(By.css('.content-type__permissions'));
        expect(pTabPanel).toBeFalsy();
    });

    it('should set the field and row bag options', () => {
        const fieldDragDropService: FieldDragDropService = fixture.debugElement.injector.get(
            FieldDragDropService
        );
        fixture.componentInstance.contentType = fakeContentType;
        spyOn(fieldDragDropService, 'setBagOptions');
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
            expect(de.query(By.css('.main-toolbar-left .content-type__title'))).toBeDefined();
            expect(de.query(By.css('.main-toolbar-left .content-type__info'))).toBeDefined();
            expect(de.query(By.css('.main-toolbar-right #form-edit-button'))).toBeDefined();
        });

        it('should have api link component', () => {
            expect(de.query(By.css('dot-api-link')).componentInstance.link).toBe(
                '/api/v1/contenttype/id/1234567890'
            );
        });

        it('should have copy variable button', () => {
            expect(de.query(By.css('dot-copy-button')).componentInstance.copy).toBe(
                'helloVariable'
            );
        });

        it('should have edit button', () => {
            const editButton: DebugElement = fixture.debugElement.query(
                By.css('#form-edit-button')
            );
            expect(editButton.nativeElement.textContent).toBe('Edit');
            expect(editButton.nativeElement.disabled).toBe(false);
            expect(editButton).toBeTruthy();
        });
    });

    describe('Tabs', () => {
        let iframe: DebugElement;
        let dotCurrentUserService: DotCurrentUserService;

        beforeEach(() => {
            fixture.componentInstance.contentType = fakeContentType;
            dotCurrentUserService = fixture.debugElement.injector.get(DotCurrentUserService);
            spyOn(dotCurrentUserService, 'hasAccessToPortlet').and.returnValue(of(true));

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
                        By.css('.content-type__fields-sidebar p-splitButton')
                    );
                    dotEventsService = fixture.debugElement.injector.get(DotEventsService);
                    spyOn(dotEventsService, 'notify');
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
                });

                it('should set actions correctly', () => {
                    const addRow: MenuItem = splitButton.componentInstance.model[0];
                    const addTabDivider: MenuItem = splitButton.componentInstance.model[1];
                    addRow.command();
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-row');
                    addTabDivider.command();
                    expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
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
