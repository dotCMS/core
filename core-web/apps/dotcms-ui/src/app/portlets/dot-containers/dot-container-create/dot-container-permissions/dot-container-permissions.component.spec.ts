import { Component, DebugElement, ElementRef, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DotContainerPermissionsComponent } from './dot-container-permissions.component';
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
    let component: DotContainerPermissionsComponent;
    let fixture: ComponentFixture<DotContainerPermissionsComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerPermissionsComponent, IframeMockComponent],
            imports: [DotPortletBoxModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContainerPermissionsComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('permissions', () => {
        beforeEach(() => {
            component.containerId = '123';
            component.ngOnInit();
            fixture.detectChanges();
        });

        it('should set iframe permissions url', () => {
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/containers/permissions.jsp?containerId=123&popup=true'
            );
        });
    });
});
