import { DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';

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
            imports: [OverlayPanelModule, BrowserAnimationsModule, DotMessagePipe],
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
        const radioButtons = fixture.debugElement.queryAll(
            By.css('.dot-sidebar-properties__radio-buttons-container p-radioButton')
        );

        expect(radioButtons.length).toEqual(3);
        expect(radioButtons[0].attributes.value).toEqual('small');
        expect(radioButtons[1].attributes.value).toEqual('medium');
        expect(radioButtons[2].attributes.value).toEqual('large');
    });

    it('should toggle overlay panel', () => {
        const button = fixture.debugElement.query(By.css('button'));
        spyOn(component.overlay, 'toggle');

        button.nativeElement.click();
        expect(component.overlay.toggle).toHaveBeenCalledTimes(1);
    });

    it('should hide overlay panel when a sidebar size property is clicked', () => {
        spyOn(component.overlay, 'hide');
        const radioButtons = fixture.debugElement.queryAll(
            By.css('.dot-sidebar-properties__radio-buttons-container p-radioButton')
        );
        radioButtons[0].nativeElement.click();
        expect(component.overlay.hide).toHaveBeenCalledTimes(1);
    });

    it('should send a layout-sidebar-change notification when a sidebar size property is updated', () => {
        spyOn(component.switch, 'emit');
        spyOn(dotEventsService, 'notify');
        const radioButtons = fixture.debugElement.queryAll(
            By.css('.dot-sidebar-properties__radio-buttons-container p-radioButton')
        );
        radioButtons[0].nativeElement.click();
        expect(dotEventsService.notify).toHaveBeenCalledWith('layout-sidebar-change');
        expect(component.switch.emit).toHaveBeenCalled();
    });
});
