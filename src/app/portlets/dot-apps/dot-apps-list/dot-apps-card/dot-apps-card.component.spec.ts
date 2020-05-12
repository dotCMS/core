import { async, ComponentFixture } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { By } from '@angular/platform-browser';
import { DotAppsCardComponent } from './dot-apps-card.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { CardModule, TooltipModule } from 'primeng/primeng';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { NgxMdModule } from 'ngx-md';

describe('DotAppsCardComponent', () => {
    let component: DotAppsCardComponent;
    let fixture: ComponentFixture<DotAppsCardComponent>;

    const messageServiceMock = new MockDotMessageService({
        'apps.configurations': 'Configurations',
        'apps.no.configurations': 'No Configurations',
        'apps.invalid.configurations': 'Invalid Configurations'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [CardModule, DotAvatarModule, DotIconModule, NgxMdModule, TooltipModule],
            declarations: [DotAppsCardComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsCardComponent);
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
                `${component.app.configurationsCount} ${component.messagesKey['apps.configurations']}`
            );

            expect(
                fixture.debugElement.query(By.css('.ui-card-content')).nativeElement.textContent
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
                `${component.app.sitesWithWarnings} ${component.messagesKey['apps.invalid.configurations']}`
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
            ).toContain(component.messagesKey['apps.no.configurations']);
        });
    });
});
