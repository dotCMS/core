import { Component, DebugElement, ElementRef, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ContainerHistoryComponent } from './container-history.component';
import { DotPortletBoxModule } from '@components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@Component({
    selector: 'dot-iframe',
    template: ''
})
export class IframeMockComponent {
    @Input() src: string;
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

describe('ContainerHistoryComponent', () => {
    let component: ContainerHistoryComponent;
    let fixture: ComponentFixture<ContainerHistoryComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ContainerHistoryComponent, IframeMockComponent],
            imports: [DotPortletBoxModule]
        }).compileComponents();

        fixture = TestBed.createComponent(ContainerHistoryComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('history', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should set iframe history url', () => {
            const permissions = de.query(By.css('[data-testId="historyIframe"]'));
            expect(permissions.componentInstance.src).toBe('/html/containers/push_history.jsp');
        });
    });
});
