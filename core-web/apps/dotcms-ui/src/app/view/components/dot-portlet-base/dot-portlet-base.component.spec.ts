import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPortletBoxComponent } from './components/dot-portlet-box/dot-portlet-box.component';
import { DotPortletBaseComponent } from './dot-portlet-base.component';
@Component({
    template: `
        <dot-portlet-base><div>Hello World</div></dot-portlet-base>
    `,
    imports: [DotPortletBaseComponent]
})
class DefaultTestHostComponent {}

@Component({
    template: `
        <dot-portlet-base [boxed]="false"><div>Hello World</div></dot-portlet-base>
    `,
    imports: [DotPortletBaseComponent]
})
class DefaultTestHostUnboxedComponent {}

@Component({
    selector: 'dot-portlet-toolbar',
    template: ``
})
class DotToolbarMockComponent {}

@Component({
    template: `
        <dot-portlet-base>
            <dot-portlet-toolbar></dot-portlet-toolbar>
            <div>Hello World</div>
        </dot-portlet-base>
    `,
    imports: [DotPortletBaseComponent, DotToolbarMockComponent]
})
class DefaultTestHostWithToolbarComponent {}

describe('DotPortletBaseComponent', () => {
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                CommonModule,
                DotPortletBaseComponent,
                DotPortletBoxComponent,
                DefaultTestHostComponent,
                DefaultTestHostUnboxedComponent,
                DefaultTestHostWithToolbarComponent,
                DotToolbarMockComponent
            ]
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

    it('should render unboxed content', fakeAsync(() => {
        const testFixture = TestBed.createComponent(DefaultTestHostUnboxedComponent);
        testFixture.detectChanges();
        tick(); // Wait for projection to complete
        testFixture.detectChanges(); // Trigger another change detection after tick

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));
        const component = content.componentInstance;

        // Verify that boxed is false
        expect(component.boxed).toBe(false);

        // Verify that dot-portlet-box is not rendered when boxed is false
        const box: DebugElement = content.query(By.css('dot-portlet-box'));
        expect(box).toBeNull();

        // After fixing the template to use *ngTemplateOutlet="unboxed" in @else,
        // the projected content should now be available
        const child = content.query(By.css('div'));
        expect(child).not.toBeNull();
        expect(child.nativeElement.innerHTML).toBe('Hello World');
    }));

    it('should render toolbar as first child', () => {
        const testFixture = TestBed.createComponent(DefaultTestHostWithToolbarComponent);
        testFixture.detectChanges();

        const content: DebugElement = testFixture.debugElement.query(By.css('dot-portlet-base'));

        expect(content.nativeNode.firstChild.nodeName).toBe('DOT-PORTLET-TOOLBAR');
    });
});
