import { Component, DebugElement, ElementRef, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ContainerPermissionsComponent } from './container-permissions.component';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

describe('ContainerPermissionsComponent', () => {
    let component: ContainerPermissionsComponent;
    let fixture: ComponentFixture<ContainerPermissionsComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ContainerPermissionsComponent, IframeMockComponent],
            imports: [DotPortletBoxModule]
        }).compileComponents();

        fixture = TestBed.createComponent(ContainerPermissionsComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('permissions', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should set iframe permissions url', () => {
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe('/html/containers/permissions.jsp');
        });
    });
});
