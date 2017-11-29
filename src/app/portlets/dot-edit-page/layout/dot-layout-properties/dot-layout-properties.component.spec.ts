import { DOTTestBed } from '../../../../test/dot-test-bed';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { ReactiveFormsModule, FormGroup, FormControl } from '@angular/forms';
import { OverlayPanelModule, ButtonModule } from 'primeng/primeng';
import { DotLayoutSidebarModule } from './dot-layout-property-sidebar/dot-layout-property-sidebar.module';
import { DotLayoutPropertiesItemModule } from './dot-layout-properties-item/dot-layout-properties-item.module';
import { DotLayoutPropertiesComponent } from './dot-layout-properties.component';
import { async, ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'dot-test-host-component',
    template:   `<form [formGroup]="group">
                    <dot-layout-properties></dot-layout-properties>
                </form>`
})
class TestHostComponent {
    group: FormGroup;
    constructor() {
        this.group = new FormGroup({
            layout: new FormControl({
                header: true,
                footer: true,
                sidebar: 'left'
            })
        });
    }
}

describe('DotLayoutPropertiesComponent', () => {
    let comp: DotLayoutPropertiesComponent;
    let fixture: ComponentFixture<DotLayoutPropertiesComponent>;
    let de: DebugElement;
    let hostComponentfixture: ComponentFixture<TestHostComponent>;

    const messageServiceMock = new MockMessageService({
        'editpage.layout.properties.header': 'Header',
        'editpage.layout.properties.footer': 'Footer'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotLayoutPropertiesComponent, TestHostComponent],
            imports: [
                DotLayoutPropertiesItemModule,
                DotLayoutSidebarModule,
                OverlayPanelModule,
                ButtonModule,
                ReactiveFormsModule
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotLayoutPropertiesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    });

    xit('should modify the group model', () => {

    });
});
