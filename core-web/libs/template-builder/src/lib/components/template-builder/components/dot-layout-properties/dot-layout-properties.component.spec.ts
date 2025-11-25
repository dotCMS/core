import { describe, expect, it } from '@jest/globals';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotLayoutPropertiesItemModule } from './dot-layout-properties-item/dot-layout-properties-item.module';
import { DotLayoutPropertiesComponent } from './dot-layout-properties.component';
import { DotLayoutSidebarModule } from './dot-layout-property-sidebar/dot-layout-property-sidebar.module';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-test-host-component',
    template: `
        <form [formGroup]="group">
            <dot-layout-properties></dot-layout-properties>
        </form>
    `
})
class TestHostComponent {
    group: UntypedFormGroup;
    constructor() {
        this.group = new UntypedFormGroup({
            layout: new UntypedFormControl({
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

    it.skip('should modify the group model', () => {
        expect(comp.group).toBeDefined();
        expect(de).toBeDefined();
    });
});
