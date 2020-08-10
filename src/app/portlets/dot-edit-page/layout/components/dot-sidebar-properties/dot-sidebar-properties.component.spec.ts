import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';
import { OverlayPanelModule } from 'primeng/primeng';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DebugElement } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('DotSidebarPropertiesComponent', () => {
    let component: DotSidebarPropertiesComponent;
    let fixture: ComponentFixture<DotSidebarPropertiesComponent>;
    let de: DebugElement;
    let dotEventsService: DotEventsService;
    let mainButton: HTMLElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.layout.sidebar.width.small': 'Small',
            'editpage.layout.sidebar.width.medium': 'Medium',
            'editpage.layout.sidebar.width.large': 'Large',
            'editpage.layout.sidebar.width.open': 'Open'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotSidebarPropertiesComponent],
            imports: [OverlayPanelModule, BrowserAnimationsModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(DotSidebarPropertiesComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        dotEventsService = de.injector.get(DotEventsService);

        mainButton = de.query(By.css('button')).nativeElement;
        mainButton.dispatchEvent(new MouseEvent('click'));
        fixture.detectChanges();
    });

    it('should has an overlay panel', () => {
        const pOverlayPanel = fixture.debugElement.query(By.css('p-overlaypanel'));
        expect(pOverlayPanel).toBeDefined();
    });

    it('should has 3 radio buttons', () => {
        const radioButtons = fixture.debugElement.query(
            By.css('.dot-sidebar-properties__radio-buttons-container')
        );

        expect(radioButtons.children.length).toEqual(3);
        expect(radioButtons.children[0].attributes.value).toEqual('small');
        expect(radioButtons.children[1].attributes.value).toEqual('medium');
        expect(radioButtons.children[2].attributes.value).toEqual('large');
    });

    it('should toggle overlay panel', () => {
        const button = fixture.debugElement.query(By.css('button'));
        spyOn(component.overlay, 'toggle');

        button.nativeElement.click();
        expect(component.overlay.toggle).toHaveBeenCalledTimes(1);
    });

    it('should hide overlay panel when a sidebar size property is clicked', () => {
        spyOn(component.overlay, 'hide');
        const radioButtons = fixture.debugElement.query(
            By.css('.dot-sidebar-properties__radio-buttons-container')
        );
        radioButtons.children[0].nativeElement.click();
        expect(component.overlay.hide).toHaveBeenCalledTimes(1);
    });

    it('should send a layout-sidebar-change notification when a sidebar size property is updated', () => {
        spyOn(component.change, 'emit');
        spyOn(dotEventsService, 'notify');
        const radioButtons = fixture.debugElement.query(
            By.css('.dot-sidebar-properties__radio-buttons-container')
        );
        radioButtons.children[0].nativeElement.click();
        expect(dotEventsService.notify).toHaveBeenCalledWith('layout-sidebar-change');
        expect(component.change.emit).toHaveBeenCalled();
    });
});
