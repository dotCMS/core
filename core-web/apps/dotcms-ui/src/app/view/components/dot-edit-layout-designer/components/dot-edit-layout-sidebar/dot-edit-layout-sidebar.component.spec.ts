import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotMessageService } from '@dotcms/data-access';
import { DotLayoutSideBar } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    mockDotContainers,
    MockDotMessageService,
    processedContainers
} from '@dotcms/utils-testing';

import { DotEditLayoutSidebarComponent } from './dot-edit-layout-sidebar.component';

import { DotSidebarPropertiesModule } from '../dot-sidebar-properties/dot-sidebar-properties.module';

let fakeValue: DotLayoutSideBar;

@Component({
    selector: 'dot-test-host-component',
    template: ` <form [formGroup]="form">
        <dot-edit-layout-sidebar formControlName="sidebar"></dot-edit-layout-sidebar>
    </form>`
})
class TestHostComponent {
    form: UntypedFormGroup;

    constructor() {
        this.form = new UntypedFormGroup({
            sidebar: new UntypedFormControl(fakeValue)
        });
    }
}

describe('DotEditLayoutSidebarComponent', () => {
    let component: DotEditLayoutSidebarComponent;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;
    let dotEditLayoutService: DotEditLayoutService;

    beforeEach(() => {
        fakeValue = {
            containers: [],
            location: 'left',
            width: 'small'
        };

        const messageServiceMock = new MockDotMessageService({
            'editpage.layout.designer.sidebar': 'Sidebar'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditLayoutSidebarComponent, TestHostComponent],
            imports: [
                DotContainerSelectorLayoutModule,
                BrowserAnimationsModule,
                DotSidebarPropertiesModule,
                DotMessagePipe
            ],
            providers: [
                DotEditLayoutService,
                DotTemplateContainersCacheService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-sidebar'));
        component = hostComponentfixture.debugElement.query(
            By.css('dot-edit-layout-sidebar')
        ).componentInstance;
        hostComponentfixture.detectChanges();
    });

    it('should have the right header for the Sidebar Header', () => {
        const headerSelector = de.query(By.css('h6'));
        expect(headerSelector.nativeElement.outerText).toBe('Sidebar');
    });

    it('should call the write value and transform the containers data', () => {
        hostComponentfixture.componentInstance.form = new UntypedFormGroup({
            sidebar: new UntypedFormControl({
                containers: [
                    { identifier: '5363c6c6-5ba0-4946-b7af-cf875188ac2e', uuid: '' },
                    { identifier: '56bd55ea-b04b-480d-9e37-5d6f9217dcc3', uuid: '' }
                ],
                location: 'left',
                width: 'small'
            })
        });

        dotEditLayoutService = hostComponentfixture.debugElement
            .query(By.css('dot-edit-layout-sidebar'))
            .injector.get(DotEditLayoutService);

        spyOn(dotEditLayoutService, 'getDotLayoutSidebar').and.returnValue(processedContainers);

        hostComponentfixture.detectChanges();
        expect(dotEditLayoutService.getDotLayoutSidebar).toHaveBeenCalled();
        expect(component.containers).toBe(processedContainers);
    });

    it('should transform containers raw data from component "dot-container-selector" into proper data to be saved in the BE', () => {
        const containerSelector: DebugElement = hostComponentfixture.debugElement.query(
            By.css('dot-container-selector')
        );
        const mockContainers = mockDotContainers();
        const transformedValue = {
            containers: [
                {
                    identifier: mockContainers[Object.keys(mockContainers)[0]].container.identifier,
                    uuid: undefined
                },
                {
                    identifier: mockContainers[Object.keys(mockContainers)[1]].container.path,
                    uuid: undefined
                }
            ],
            location: 'left',
            width: 'small'
        };
        spyOn(component, 'updateAndPropagate').and.callThrough();
        spyOn(component, 'propagateChange');
        containerSelector.triggerEventHandler('change', processedContainers);
        component.updateAndPropagate(processedContainers);
        expect(component.updateAndPropagate).toHaveBeenCalled();
        expect(component.propagateChange).toHaveBeenCalledWith(transformedValue);
    });

    it('should propagate call from component "dot-sidebar-properties" into parent container', () => {
        const sidebarProperties: DebugElement = hostComponentfixture.debugElement.query(
            By.css('dot-sidebar-properties')
        );
        spyOn(component, 'updateAndPropagate').and.callThrough();
        spyOn(component, 'propagateChange');
        sidebarProperties.triggerEventHandler('change', '');
        component.updateAndPropagate();
        expect(component.updateAndPropagate).toHaveBeenCalled();
        expect(component.propagateChange).toHaveBeenCalled();
    });
});
