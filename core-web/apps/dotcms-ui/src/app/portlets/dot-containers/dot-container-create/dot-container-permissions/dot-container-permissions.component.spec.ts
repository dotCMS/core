import { Component, DebugElement, ElementRef, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotContainerPermissionsComponent } from './dot-container-permissions.component';

import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

@Component({
    selector: `dot-host-component`,
    template: `
        <dot-container-permissions [containerId]="containerId"></dot-container-permissions>
    `,
    standalone: false
})
class DotTestHostComponent {
    containerId = '';
}

describe('ContainerPermissionsComponent', () => {
    let hostComponent: DotTestHostComponent;
    let fixture: ComponentFixture<DotTestHostComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTestHostComponent],
            imports: [DotContainerPermissionsComponent, DotPortletBoxComponent, IframeMockComponent]
        })
            .overrideComponent(DotContainerPermissionsComponent, {
                remove: { imports: [IframeComponent] },
                add: { imports: [IframeMockComponent] }
            })
            .compileComponents();

        fixture = TestBed.createComponent(DotContainerPermissionsComponent);
        de = fixture.debugElement;
        hostComponent = fixture.componentInstance;
        hostComponent.containerId = '123';
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(hostComponent).toBeTruthy();
    });

    describe('permissions', () => {
        it('should set iframe permissions url', () => {
            hostComponent.containerId = '123';
            fixture.detectChanges();
            const permissions = de.query(By.css('[data-testId="permissionsIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/containers/permissions.jsp?containerId=123&popup=true'
            );
        });
    });
});
