import { async, ComponentFixture } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { By } from '@angular/platform-browser';
import { DotServiceIntegrationCardComponent } from './dot-service-integration-card.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { CardModule } from 'primeng/primeng';

describe('DotServiceIntegrationCardComponent', () => {
    let component: DotServiceIntegrationCardComponent;
    let fixture: ComponentFixture<DotServiceIntegrationCardComponent>;

    const messageServiceMock = new MockDotMessageService({
        'service.integration.configurations': 'Configurations',
        'service.integration.no.configurations': 'No Configurations'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [CardModule, DotAvatarModule],
            declarations: [DotServiceIntegrationCardComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotServiceIntegrationCardComponent);
        component = fixture.debugElement.componentInstance;
    });

    describe('With configuration', () => {
        beforeEach(() => {
            component.serviceIntegration = {
                configurationsCount: 1,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            };
            fixture.detectChanges();
        });

        it('should not have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-service-integration-card__disabled')
            ).toBeFalsy();
        });

        it('should have avatar with right values', () => {
            const avatar = fixture.debugElement.query(By.css('dot-avatar')).componentInstance;
            expect(avatar.size).toBe(40);
            expect(avatar.showDot).toBe(true);
            expect(avatar.url).toBe(component.serviceIntegration.iconUrl);
            expect(avatar.label).toBe(component.serviceIntegration.name);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-service-integration-card__name'))
                    .nativeElement.innerText
            ).toBe(component.serviceIntegration.name);

            expect(
                fixture.debugElement.query(By.css('.dot-service-integration-card__configurations'))
                    .nativeElement.textContent
            ).toContain(
                `${component.serviceIntegration.configurationsCount} ${component.messagesKey['service.integration.configurations']}`
            );

            expect(
                fixture.debugElement.query(By.css('.ui-card-content')).nativeElement.textContent
            ).toContain(component.serviceIntegration.description);
        });
    });

    describe('With No configuration', () => {
        beforeEach(() => {
            component.serviceIntegration = {
                configurationsCount: 0,
                key: 'asana',
                name: 'Asana',
                description: "It's asana to keep track of your asana events",
                iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
            };
            fixture.detectChanges();
        });

        it('should have disabled css class', () => {
            expect(
                fixture.debugElement
                    .query(By.css('p-card'))
                    .nativeElement.classList.contains('dot-service-integration-card__disabled')
            ).toBeTruthy();
        });

        it('should have avatar with right values', () => {
            expect(fixture.debugElement.query(By.css('dot-avatar')).componentInstance.showDot).toBe(
                false
            );
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-service-integration-card__configurations'))
                    .nativeElement.textContent
            ).toContain(component.messagesKey['service.integration.no.configurations']);
        });
    });
});
