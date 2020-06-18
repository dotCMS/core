import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotEditLayoutSidebarComponent } from './dot-edit-layout-sidebar.component';
import { FormControl, FormGroup } from '@angular/forms';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { TemplateContainersCacheService } from '../../../template-containers-cache.service';
import { DotSidebarPropertiesModule } from '../dot-sidebar-properties/dot-sidebar-properties.module';
import { mockDotContainers } from '../../../../../test/dot-page-render.mock';
import { DotLayoutSideBar } from '@portlets/dot-edit-page/shared/models/dot-layout-sidebar.model';
import { DotEditLayoutService } from '@portlets/dot-edit-page/shared/services/dot-edit-layout.service';

let fakeValue: DotLayoutSideBar;

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="form">
                    <dot-edit-layout-sidebar formControlName="sidebar"></dot-edit-layout-sidebar>
                </form>`
})
class TestHostComponent {
    form: FormGroup;
    constructor() {
        this.form = new FormGroup({
            sidebar: new FormControl(fakeValue)
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
                DotContainerSelectorModule,
                BrowserAnimationsModule,
                DotSidebarPropertiesModule
            ],
            providers: [
                DotEditLayoutService,
                TemplateContainersCacheService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        hostComponentfixture = DOTTestBed.createComponent(TestHostComponent);
        de = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-sidebar'));
        component = hostComponentfixture.debugElement.query(By.css('dot-edit-layout-sidebar'))
            .componentInstance;
        hostComponentfixture.detectChanges();
    });

    it('should have the right header for the Sidebar Header', () => {
        const headerSelector = de.query(By.css('h6'));
        expect(headerSelector.nativeElement.outerText).toBe('Sidebar');
    });

    it('should call the write value and transform the containers data', () => {
        const mockResponse = mockDotContainers;

        hostComponentfixture.componentInstance.form = new FormGroup({
            sidebar: new FormControl({
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

        spyOn(dotEditLayoutService, 'getDotLayoutSidebar').and.returnValue(mockResponse);

        hostComponentfixture.detectChanges();
        expect(dotEditLayoutService.getDotLayoutSidebar).toHaveBeenCalled();
        expect(component.containers).toBe(mockResponse);
    });

    it('should transform containers raw data from component "dot-container-selector" into proper data to be saved in the BE', () => {
        const containerSelector: DebugElement = hostComponentfixture.debugElement.query(
            By.css('dot-container-selector')
        );
        const transformedValue = {
            containers: [
                {
                    identifier: mockDotContainers[0].container.identifier,
                    uuid: undefined
                },
                {
                    identifier: mockDotContainers[1].container.identifier,
                    uuid: undefined
                }
            ],
            location: 'left',
            width: 'small'
        };
        spyOn(component, 'updateAndPropagate').and.callThrough();
        spyOn(component, 'propagateChange');
        containerSelector.triggerEventHandler('change', mockDotContainers);
        component.updateAndPropagate(mockDotContainers);
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
