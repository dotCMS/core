import { ComponentFixture} from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { Component, DebugElement } from '@angular/core';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { By } from '@angular/platform-browser';
import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';
import { OverlayPanelModule } from 'primeng/primeng';

describe('DotSidebarPropertiesComponent', () => {

    let component: DotSidebarPropertiesComponent;
    let fixture: ComponentFixture<DotSidebarPropertiesComponent>;

    beforeEach(() => {

        const messageServiceMock = new MockDotMessageService({
            'editpage.layout.sidebar.width.small': 'Small',
            'editpage.layout.sidebar.width.medium': 'Medium',
            'editpage.layout.sidebar.width.large': 'Large',
            'editpage.layout.sidebar.width.open': 'Open'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotSidebarPropertiesComponent],
            imports: [OverlayPanelModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(DotSidebarPropertiesComponent);
        component = fixture.componentInstance;
    });

    it('should has an overlay panel', () => {
        const pOverlayPanel = fixture.debugElement.query(By.css('p-overlaypanel'));

        expect(pOverlayPanel).toBeDefined();
    });

    it('should has 3 radio buttons', () => {
        const radioButtons = fixture.debugElement.query(By.css('.dot-sidebar-properties__radio-buttons-container'));

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
});
