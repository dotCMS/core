import { async, ComponentFixture } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { CommonModule } from '@angular/common';
import { DotServiceIntegrationConfigurationItemComponent } from './dot-service-integration-configuration-item.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { By } from '@angular/platform-browser';

const messages = {
    'service.integration.key': 'Key',
    'service.integration.configurations': 'Configurations',
    'service.integration.no.configurations': 'No Configurations',
    'service.integration.confirmation.delete.all.button': 'Delete All',
    'service.integration.confirmation.title': 'Are you sure?',
    'service.integration.confirmation.description.show.more': 'Show More',
    'service.integration.confirmation.description.show.less': 'Show Less',
    'service.integration.confirmation.delete.all.message': 'Delete all?',
    'service.integration.confirmation.accept': 'Ok',
    'service.integration.search.placeholder': 'Search by name'
};

const sites = [
    {
        configured: true,
        id: '123',
        name: 'demo.dotcms.com'
    },
    {
        configured: false,
        id: '456',
        name: 'host.example.com'
    }
];

describe('DotServiceIntegrationConfigurationItemComponent', () => {
    let component: DotServiceIntegrationConfigurationItemComponent;
    let fixture: ComponentFixture<DotServiceIntegrationConfigurationItemComponent>;
    let dialogService: DotAlertConfirmService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [CommonModule, DotIconButtonModule],
            declarations: [DotServiceIntegrationConfigurationItemComponent],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotServiceIntegrationConfigurationItemComponent);
        component = fixture.debugElement.componentInstance;
        dialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
    });

    describe('With configuration', () => {
        beforeEach(() => {
            component.site = sites[0];
            fixture.detectChanges();
        });

        it('should load messages keys', () => {
            expect(component.messagesKey).toBe(messages);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration-list__name')
                ).nativeElement.innerText
            ).toBe(sites[0].name);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration-list__host-key')
                ).nativeElement.textContent
            ).toContain(`${component.messagesKey['service.integration.key']} ${sites[0].id}`);
        });

        it('should have 2 icon buttons for delete and edit', () => {
            const buttons = fixture.debugElement.queryAll(By.css('dot-icon-button'));
            expect(buttons.length).toBe(2);
            expect(buttons[0].componentInstance.icon).toBe('delete_outline');
            expect(buttons[1].componentInstance.icon).toBe('edit');
        });

        it('should emit delete action', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const deleteBtn = fixture.debugElement.queryAll(By.css('dot-icon-button'))[0];

            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            spyOn(component.delete, 'emit');

            deleteBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy,
                site: sites[0]
            });
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(component.delete.emit).toHaveBeenCalledWith(sites[0]);
        });

        it('should emit edit action with a site', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const editBtn = fixture.debugElement.queryAll(By.css('dot-icon-button'))[1];

            spyOn(component.edit, 'emit');

            editBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy,
                site: sites[0]
            });
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(component.edit.emit).toHaveBeenCalledWith(sites[0]);
        });

        it('should emit edit action when host component clicked', () => {
            spyOn(component.edit, 'emit');
            fixture.debugElement.triggerEventHandler('click', {
                stopPropagation: () => {}
            });
            expect(component.edit.emit).toHaveBeenCalledWith(sites[0]);
        });
    });

    describe('With No configuration', () => {
        beforeEach(() => {
            component.site = sites[1];
            fixture.detectChanges();
        });

        it('should have 1 icon button for create', () => {
            const buttons = fixture.debugElement.queryAll(By.css('dot-icon-button'));
            expect(buttons.length).toBe(1);
            expect(buttons[0].componentInstance.icon).toBe('add_circle');
        });

        it('should emit edit action with No site', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const createBtn = fixture.debugElement.queryAll(By.css('dot-icon-button'))[0];

            spyOn(component.edit, 'emit');

            createBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy,
                site: sites[1]
            });
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(component.edit.emit).toHaveBeenCalledWith(sites[1]);
        });
    });
});
