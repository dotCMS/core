import { MockProvider } from 'ng-mocks';

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';

import { DotAnalyticsStateMessageComponent } from './dot-analytics-state-message.component';

@Component({
    template: `
        <dot-analytics-state-message [message]="message" [icon]="icon" />
    `
})
class TestHostComponent {
    message = 'test.message';
    icon = 'pi-info-circle';
}

describe('DotAnalyticsStateMessageComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAnalyticsStateMessageComponent],
            declarations: [TestHostComponent],
            providers: [MockProvider(DotMessageService)]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render the icon with correct classes', () => {
        const iconElement = fixture.debugElement.query(By.css('i'));

        expect(iconElement).toBeTruthy();
        expect(iconElement.nativeElement.className).toContain('pi');
        expect(iconElement.nativeElement.className).toContain('pi-info-circle');
        expect(iconElement.nativeElement.className).toContain('text-gray-400');
        expect(iconElement.nativeElement.className).toContain('text-2xl');
    });

    it('should render the message', () => {
        const messageElement = fixture.debugElement.query(By.css('.state-message'));

        expect(messageElement).toBeTruthy();
        expect(messageElement.nativeElement.textContent.trim()).toBe('test.message');
    });

    it('should update when inputs change', () => {
        component.message = 'new.message';
        component.icon = 'pi-exclamation-triangle';
        fixture.detectChanges();

        const iconElement = fixture.debugElement.query(By.css('i'));
        const messageElement = fixture.debugElement.query(By.css('.state-message'));

        expect(iconElement.nativeElement.className).toContain('pi-exclamation-triangle');
        expect(messageElement.nativeElement.textContent.trim()).toBe('new.message');
    });
});
