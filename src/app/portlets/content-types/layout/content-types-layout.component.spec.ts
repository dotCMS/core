import { DOTTestBed } from '../../../test/dot-test-bed';
import { ContentTypesLayoutComponent } from './content-types-layout.component';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Input, Injectable } from '@angular/core';
import { TabViewModule } from 'primeng/primeng';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { By } from '@angular/platform-browser';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { Observable } from 'rxjs/Observable';
import { FieldDragDropService } from '../fields/service';
import {DotIconModule} from '../../../view/components/_common/dot-icon/dot-icon.module';

@Component({
    selector: 'dot-content-types-fields-list',
    template: ''
})
class TestContentTypeFieldsListComponent {}

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
    template: '<dot-content-type-layout [contentTypeId]="contentTypeId"></dot-content-type-layout>'
})
class TestHostComponent {
    @Input() contentTypeId: string;
}

@Component({
    selector: 'dot-content-types-relationship-listing',
    template: ''
})
class TestContentTypesRelationshipListingComponent {}

@Injectable()
export class MockDotMenuService {
    getDotMenuId(): Observable<string> {
        return Observable.of('1234');
    }
}

class FieldDragDropServiceMock {
    setBagOptions() {}
}

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
            'contenttypes.tab.relationship.header': 'Relationship'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesLayoutComponent,
                TestContentTypeFieldsListComponent,
                TestContentTypeFieldsRowListComponent,
                TestDotIframeComponent,
                TestContentTypesRelationshipListingComponent,
                TestHostComponent
            ],
            imports: [TabViewModule, DotIconModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotMenuService, useClass: MockDotMenuService },
                { provide: FieldDragDropService, useClass: FieldDragDropServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(TestHostComponent);
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

    it('should set the field and row bag options', () => {
        const fieldDragDropService: FieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setBagOptions');
        fixture.detectChanges();

        expect(fieldDragDropService.setBagOptions).toHaveBeenCalledTimes(1);
    });

    describe('Tabs', () => {
        let iframe: DebugElement;

        beforeEach(() => {
            fixture.componentInstance.contentTypeId = '2';
            fixture.detectChanges();
        });

        describe('Fields', () => {
            beforeEach(() => {
                this.pTabPanel = de.query(By.css('.content-type__properties'));
                this.pTabPanel.componentInstance.selected = true;
            });

            it('should have a field panel', () => {
                expect(this.pTabPanel).not.toBeNull();
                expect(this.pTabPanel.componentInstance.header).toBe('Fields Header Tab');
            });

            it('should have a content-type__fields-main', () => {
                const contentTypeFieldsMain = this.pTabPanel.query(By.css('.content-type__fields-main'));
                expect(contentTypeFieldsMain).not.toBeNull();
            });

            it('should have a content-type__fields-sidebar', () => {
                const contentTypeFieldsSideBar = this.pTabPanel.query(By.css('.content-type__fields-sidebar'));
                expect(contentTypeFieldsSideBar).not.toBeNull();
            });

            it('should have a field types list', () => {
                const fieldTitle = this.pTabPanel.query(By.css('.content-type__fields-sidebar-title span'));
                const contentTypesFieldsList = this.pTabPanel.query(By.css('dot-content-types-fields-list'));
                expect(fieldTitle.nativeElement.textContent).toBe('Field Title');
                expect(contentTypesFieldsList).not.toBeNull();
            });

            // Hiding the rows list for 5.0
            xit('should have a field row list', () => {
                const layoutTitle = this.pTabPanel.queryAll(By.css('.content-type__fields-sidebar-title'))[1];
                const fieldRowList = this.pTabPanel.query(By.css('dot-content-type-fields-row-list'));

                expect(layoutTitle.nativeElement.textContent).toBe('Layout Title');
                expect(fieldRowList).not.toBeNull();
            });
        });

        describe('Permission', () => {
            beforeEach(() => {
                this.pTabPanel = de.query(By.css('.content-type__permissions'));
                this.pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = this.pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(this.pTabPanel).not.toBeNull();
                expect(this.pTabPanel.componentInstance.header).toBe('Permissions Tab');
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe('/html/content_types/permissions.jsp?contentTypeId=2&popup=true');
            });
        });

        describe('Push History', () => {
            beforeEach(() => {
                this.pTabPanel = de.query(By.css('.content-type__push_history'));
                this.pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = this.pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a permission panel', () => {
                expect(this.pTabPanel).not.toBeNull();
                expect(this.pTabPanel.componentInstance.header).toBe('Push History');
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe('/html/content_types/push_history.jsp?contentTypeId=2&popup=true');
            });
        });

        describe('Relationship', () => {
            beforeEach(() => {
                this.pTabPanel = de.query(By.css('.content-type__relationships'));
                this.pTabPanel.componentInstance.selected = true;

                fixture.detectChanges();
                iframe = this.pTabPanel.query(By.css('dot-iframe'));
            });

            it('should have a Relationship tab', () => {
                expect(this.pTabPanel).toBeDefined();
            });

            it('should have a right header', () => {
                expect(this.pTabPanel.componentInstance.header).toBe('Relationship');
            });

            it('should have a iframe', () => {
                expect(iframe).not.toBeNull();
            });

            it('should set the src attribute', () => {
                expect(iframe.componentInstance.src).toBe(
                    // tslint:disable-next-line:max-line-length
                    'c/portal/layout?p_l_id=1234&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=2'
                );
            });
        });
    });
});
