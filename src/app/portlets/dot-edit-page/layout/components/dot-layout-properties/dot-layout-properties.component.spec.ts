import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { ReactiveFormsModule, FormGroup, FormControl } from '@angular/forms';
import { OverlayPanelModule, ButtonModule } from 'primeng/primeng';
import { DotLayoutSidebarModule } from './dot-layout-property-sidebar/dot-layout-property-sidebar.module';
import { DotLayoutPropertiesItemModule } from './dot-layout-properties-item/dot-layout-properties-item.module';
import { DotLayoutPropertiesComponent } from './dot-layout-properties.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';

@Component({
    selector: 'dot-test-host-component',
    template: `<form [formGroup]="group">
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

    const messageServiceMock = new MockDotMessageService({
        'editpage.layout.properties.header': 'Header',
        'editpage.layout.properties.footer': 'Footer'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotLayoutPropertiesComponent, TestHostComponent],
            imports: [
                DotLayoutPropertiesItemModule,
                DotLayoutSidebarModule,
                OverlayPanelModule,
                ButtonModule,
                ReactiveFormsModule
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLayoutPropertiesComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    });

    xit('should modify the group model', () => {
        expect(comp.group).toBeDefined();
        expect(de).toBeDefined();
    });
});
