import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { DotAppsCardComponent } from './dot-apps-card.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';
import { Component } from '@angular/core';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'markdown',
    template: `<ng-content></ng-content>`
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

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [
                    CardModule,
                    DotAvatarModule,
                    DotIconModule,
                    TooltipModule,
                    DotPipesModule
                ],
                declarations: [DotAppsCardComponent, MockMarkdownComponent],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotAppsCardComponent);
        component = fixture.debugElement.componentInstance;
    });

    describe('With configuration', () => {
        beforeEach(() => {
            component.app = {
                allowExtraParams: true,
                configurationsCount: 1,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            };
            fixture.detectChanges();
        });

        it('should not have warning icon', () => {
            expect(fixture.debugElement.query(By.css('dot-icon'))).toBeFalsy();
        });

        it('should not have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-apps-card__disabled')
            ).toBeFalsy();
        });

        it('should have avatar with right values', () => {
            const avatar = fixture.debugElement.query(By.css('dot-avatar')).componentInstance;
            expect(avatar.size).toBe(40);
            expect(avatar.showDot).toBe(true);
            expect(avatar.url).toBe(component.app.iconUrl);
            expect(avatar.label).toBe(component.app.name);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__name')).nativeElement.innerText
            ).toBe(component.app.name);

            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__configurations')).nativeElement
                    .textContent
            ).toContain(
                `${component.app.configurationsCount} ${messageServiceMock.get(
                    'apps.configurations'
                )}`
            );

            expect(
                fixture.debugElement.query(By.css('.p-card-content')).nativeElement.textContent
            ).toContain(component.app.description);
        });
    });

    describe('With No configuration & warnings', () => {
        beforeEach(() => {
            component.app = {
                allowExtraParams: false,
                configurationsCount: 0,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg',
                sitesWithWarnings: 2
            };
            fixture.detectChanges();
        });

        it('should have warning icon', () => {
            const warningIcon = fixture.debugElement.query(By.css('dot-icon'));
            expect(warningIcon).toBeTruthy();
            expect(warningIcon.attributes['name']).toBe('warning');
            expect(warningIcon.attributes['size']).toBe('18');
            expect(warningIcon.attributes['ng-reflect-text']).toBe(
                `${component.app.sitesWithWarnings} ${messageServiceMock.get(
                    'apps.invalid.configurations'
                )}`
            );
        });

        it('should have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-apps-card__disabled')
            ).toBeTruthy();
        });

        it('should have avatar with right values', () => {
            expect(fixture.debugElement.query(By.css('dot-avatar')).componentInstance.showDot).toBe(
                false
            );
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-card__configurations')).nativeElement
                    .textContent
            ).toContain(messageServiceMock.get('apps.no.configurations'));
        });
    });
});
