import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletBaseComponent } from './dot-portlet-base.component';
@Component({
    template: `
        <dot-portlet-base><div>Hello World</div></dot-portlet-base>
    `,
    standalone: false
})
class DefaultTestHostComponent {}

@Component({
    template: `
        <dot-portlet-base [boxed]="false"><div>Hello World</div></dot-portlet-base>
    `,
    standalone: false
})
class DefaultTestHostUnboxedComponent {}

@Component({
    template: `
        <dot-portlet-base>
            <dot-portlet-toolbar></dot-portlet-toolbar>
            <div>Hello World</div>
        </dot-portlet-base>
    `,
    standalone: false
})
class DefaultTestHostWithToolbarComponent {}

@Component({
    selector: 'dot-portlet-toolbar',
    template: ``,
    standalone: false
})
class DotToolbarMockComponent {}

describe('DotPortletBaseComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotPortletBaseComponent,
                DefaultTestHostComponent,
                DotPortletBoxComponent,
                DefaultTestHostUnboxedComponent,
                DefaultTestHostWithToolbarComponent,
                DotToolbarMockComponent
            ],
            imports: [CommonModule]
        }).compileComponents();
    });

    it('should render boxed content', () => {
        const testFixture = TestBed.createComponent(DefaultTestHostComponent);
        testFixture.detectChanges();

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));

        const box: DebugElement = content.query(By.css('dot-portlet-box'));
        expect(box).toBeDefined();

        expect(box.nativeElement.innerHTML).toContain('<div>Hello World</div>');

        const toolbar: DebugElement = content.query(By.css('dot-portlet-toolbar'));
        expect(toolbar).toBeNull();
    });

    it('should render unboxed content', () => {
        const testFixture = TestBed.createComponent(DefaultTestHostUnboxedComponent);
        testFixture.detectChanges();

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));

        const box: DebugElement = content.query(By.css('dot-portlet-box'));
        expect(box).toBeNull();

        const child = content.query(By.css('div'));
        expect(child.nativeElement.innerHTML).toBe('Hello World');
    });

    it('should render toolbar as first child', () => {
        const testFixture = TestBed.createComponent(DefaultTestHostWithToolbarComponent);
        testFixture.detectChanges();

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));

        expect(content.nativeNode.firstChild.nodeName).toBe('DOT-PORTLET-TOOLBAR');
    });
});
