import {
    Component,
    DebugElement,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    ViewChild
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotRouterService } from '@dotcms/data-access';
import { MockDotRouterService } from '@dotcms/utils-testing';

import { DotContainerHistoryComponent } from './dot-container-history.component';

import { DotPortletBoxModule } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.module';

@Component({
    selector: 'dot-iframe',
    template: '',
    standalone: false
})
export class IframeMockComponent {
    @Input() src: string;
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

@Component({
    selector: `dot-host-component`,
    template: `
        <dot-container-history [containerId]="containerId"></dot-container-history>
    `,
    standalone: false
})
class DotTestHostComponent {
    containerId = '';
}

describe('ContainerHistoryComponent', () => {
    let hostComponent: DotTestHostComponent;
    let fixture: ComponentFixture<DotTestHostComponent>;
    let de: DebugElement;
    let dotRouterService: DotRouterService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerHistoryComponent, IframeMockComponent, DotTestHostComponent],
            imports: [DotPortletBoxModule],
            providers: [{ provide: DotRouterService, useClass: MockDotRouterService }]
        }).compileComponents();

        fixture = TestBed.createComponent(DotTestHostComponent);
        de = fixture.debugElement;
        hostComponent = fixture.componentInstance;
        hostComponent.containerId = '123';
        fixture.detectChanges();

        dotRouterService = TestBed.inject(DotRouterService);
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

    describe('onCustomEvent', () => {
        it('should call the service method when dotDialog emits custom output', () => {
            const historyIframe: IframeMockComponent = de.query(
                By.css('[data-testId="historyIframe"]')
            ).componentInstance;

            const customEvent = new CustomEvent('custom', {
                detail: {
                    data: { id: '456' },
                    name: 'bring-back-version'
                }
            });

            historyIframe.custom.emit(customEvent);

            expect(dotRouterService.goToEditContainer).toHaveBeenCalledWith('456');
            expect(dotRouterService.goToEditContainer).toHaveBeenCalledTimes(1);
        });
    });
});
