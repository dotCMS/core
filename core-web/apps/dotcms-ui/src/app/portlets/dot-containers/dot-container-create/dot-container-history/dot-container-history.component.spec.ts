import { Component, DebugElement, ElementRef, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

import { DotContainerHistoryComponent } from './dot-container-history.component';

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
        <dot-container-history [containerId]="containerId"></dot-container-history>
    `
})
class DotTestHostComponent {
    containerId = '';
}

describe('ContainerHistoryComponent', () => {
    let hostComponent: DotTestHostComponent;
    let fixture: ComponentFixture<DotTestHostComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerHistoryComponent, IframeMockComponent, DotTestHostComponent],
            imports: [DotPortletBoxModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTestHostComponent);
        de = fixture.debugElement;
        hostComponent = fixture.componentInstance;
        hostComponent.containerId = '123';
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(hostComponent).toBeTruthy();
    });

    describe('history', () => {
        it('should set iframe history url', () => {
            hostComponent.containerId = '123';
            fixture.detectChanges();
            const permissions = de.query(By.css('[data-testId="historyIframe"]'));
            expect(permissions.componentInstance.src).toBe(
                '/html/containers/push_history.jsp?containerId=123&popup=true'
            );
        });
    });
});
