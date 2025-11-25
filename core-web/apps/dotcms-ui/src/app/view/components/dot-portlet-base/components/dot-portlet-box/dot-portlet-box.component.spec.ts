import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPortletBoxComponent } from './dot-portlet-box.component';

@Component({
    template: `
        <dot-portlet-box><div>Hello World</div></dot-portlet-box>
    `
})
class DefaultTestHostComponent {}

describe('DotPortletBoxComponent', () => {
    let component: DebugElement;
    let fixture: ComponentFixture<DotPortletBoxComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotPortletBoxComponent, DefaultTestHostComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DefaultTestHostComponent);
        de = fixture.debugElement;
        component = de.query(By.css('dot-portlet-box'));
        fixture.detectChanges();
    });

    it('should show children', () => {
        expect(component.nativeNode.innerHTML).toBe('<div>Hello World</div>');
    });
});
