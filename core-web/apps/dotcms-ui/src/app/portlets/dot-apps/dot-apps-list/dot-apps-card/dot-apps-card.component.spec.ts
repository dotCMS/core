import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotAvatarDirective, DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';
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
                        AvatarModule,
                        BadgeModule,
                        MockMarkdownComponent,
                        TooltipModule,
                        DotAvatarDirective,
                        DotColorIconComponent,
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
                    .query(By.css('article'))
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
            expect(fixture.debugElement.query(By.css('h3')).nativeElement.textContent.trim()).toBe(
                component.$app().name
            );

            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__configurations')).nativeElement
                    .textContent
            ).toContain(
                `${component.$app().configurationsCount} ${messageServiceMock.get(
                    'apps.configurations'
                )}`
            );

            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__description')).nativeElement
                    .textContent
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
                    .query(By.css('article'))
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

    describe('Icon rendering', () => {
        it('renders dot-color-icon with material icon when icon/color are set and no iconUrl', () => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'page-scanner',
                name: 'Page Scanner',
                description: 'desc',
                icon: 'search',
                color: '#3b82f6'
            });
            fixture.detectChanges();

            const colorIcon = fixture.debugElement.query(By.css('dot-color-icon'));
            expect(colorIcon).toBeTruthy();
            expect(colorIcon.componentInstance.color()).toBe('#3b82f6');
            expect(
                colorIcon.nativeElement
                    .querySelector('.material-symbols-outlined')
                    .textContent.trim()
            ).toBe('search');
            expect(fixture.debugElement.query(By.css('p-avatar'))).toBeFalsy();
        });

        it('iconUrl always wins over icon/color when both are set', () => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'page-scanner',
                name: 'Page Scanner',
                description: 'desc',
                iconUrl: 'https://example.com/icon.png',
                icon: 'search',
                color: '#3b82f6'
            });
            fixture.detectChanges();

            expect(fixture.debugElement.query(By.css('p-avatar'))).toBeTruthy();
            expect(fixture.debugElement.query(By.css('dot-color-icon'))).toBeFalsy();
        });

        it('falls back to label avatar when neither iconUrl nor icon is set', () => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'plain',
                name: 'Plain App',
                description: 'desc'
            });
            fixture.detectChanges();

            const avatar = fixture.debugElement.query(By.css('p-avatar'));
            expect(avatar).toBeTruthy();
            expect(avatar.componentInstance.image).toBeFalsy();
            expect(fixture.debugElement.query(By.css('dot-color-icon'))).toBeFalsy();
        });

        it('defaults color to surface when icon is set without color', () => {
            fixture.componentRef.setInput('app', {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'no-color',
                name: 'No Color',
                description: 'desc',
                icon: 'search'
            });
            fixture.detectChanges();

            const colorIcon = fixture.debugElement.query(By.css('dot-color-icon'));
            expect(colorIcon.componentInstance.color()).toBe('surface');
        });
    });
});
