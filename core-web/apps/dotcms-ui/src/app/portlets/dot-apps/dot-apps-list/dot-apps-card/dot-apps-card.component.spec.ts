import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAppsCardComponent } from './dot-apps-card.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'markdown',
    template: `
        <ng-content></ng-content>
    `
})
class MockMarkdownComponent {}

describe('DotAppsCardComponent', () => {
    let component: DotAppsCardComponent;
    let fixture: ComponentFixture<DotAppsCardComponent>;

    const messageServiceMock = new MockDotMessageService({
        'apps.configurations': 'Configurations',
        'apps.no.configurations': 'No Configurations',
        'apps.invalid.configurations': 'Invalid Configurations'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [DotAppsCardComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        })
            .overrideComponent(DotAppsCardComponent, {
                set: {
                    imports: [
                        CommonModule,
                        CardModule,
                        AvatarModule,
                        BadgeModule,
                        MockMarkdownComponent,
                        TooltipModule,
                        DotAvatarDirective,
                        DotMessagePipe
                    ]
                }
            })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotAppsCardComponent);
        component = fixture.debugElement.componentInstance;
    });

    describe('With configuration', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            });
            fixture.detectChanges();
        });

        it('should not have warning icon', () => {
            expect(fixture.debugElement.query(By.css('.pi-exclamation-triangle'))).toBeFalsy();
        });

        it('should not have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-apps-card__disabled')
            ).toBeFalsy();
        });

        it('should have avatar with right values', () => {
            const avatar = fixture.debugElement.query(By.css('p-avatar'));

            const { image, size } = avatar.componentInstance;

            expect(image).toBe(component.$app().iconUrl);
            expect(size).toBe('large');
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__name')).nativeElement.textContent
            ).toBe(component.$app().name);

            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__configurations')).nativeElement
                    .textContent
            ).toContain(
                `${component.$app().configurationsCount} ${messageServiceMock.get(
                    'apps.configurations'
                )}`
            );

            expect(
                fixture.debugElement.query(By.css('.p-card-content')).nativeElement.textContent
            ).toContain(component.$app().description);
        });
    });

    describe('With No configuration & warnings', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: false,
                configurationsCount: 0,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg',
                sitesWithWarnings: 2
            });
            fixture.detectChanges();
        });

        it('should have warning icon', () => {
            const warningIcon = fixture.debugElement.query(By.css('.pi-exclamation-triangle'));
            expect(warningIcon).toBeTruthy();
        });

        it('should have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-apps-card__disabled')
            ).toBeTruthy();
        });

        it('should have avatar with right values', () => {
            expect(fixture.debugElement.query(By.css('.p-badge'))).toBeFalsy();
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__configurations')).nativeElement
                    .textContent
            ).toContain(messageServiceMock.get('apps.no.configurations'));
        });
    });
});
