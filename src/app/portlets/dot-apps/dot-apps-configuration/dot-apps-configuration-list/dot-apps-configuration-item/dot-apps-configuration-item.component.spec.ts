import { async, ComponentFixture } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { TooltipModule } from 'primeng/primeng';

const messages = {
    'apps.key': 'Key',
    'apps.configurations': 'Configurations',
    'apps.no.configurations': 'No Configurations',
    'apps.confirmation.delete.all.button': 'Delete All',
    'apps.confirmation.title': 'Are you sure?',
    'apps.confirmation.description.show.more': 'Show More',
    'apps.confirmation.description.show.less': 'Show Less',
    'apps.confirmation.delete.all.message': 'Delete all?',
    'apps.confirmation.accept': 'Ok',
    'apps.search.placeholder': 'Search by name',
    'apps.invalid.secrets': 'Invalid Secrets'
};

const sites = [
    {
        configured: true,
        id: '123',
        name: 'demo.dotcms.com',
        secretsWithWarnings: 2
    },
    {
        configured: false,
        id: '456',
        name: 'host.example.com'
    }
];

describe('DotAppsConfigurationItemComponent', () => {
    let component: DotAppsConfigurationItemComponent;
    let fixture: ComponentFixture<DotAppsConfigurationItemComponent>;
    let dialogService: DotAlertConfirmService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                imports: [
                    CommonModule,
                    DotIconButtonModule,
                    DotIconModule,
                    TooltipModule
                ],
                declarations: [DotAppsConfigurationItemComponent],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });
        })
    );

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationItemComponent);
        component = fixture.debugElement.componentInstance;
        dialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
    });

    describe('With configuration', () => {
        beforeEach(() => {
            component.site = sites[0];
            fixture.detectChanges();
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__name'))
                    .nativeElement.innerText
            ).toBe(sites[0].name);

            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__host-key'))
                    .nativeElement.textContent
            ).toContain(`${messageServiceMock.get('apps.key')} ${sites[0].id}`);
        });

        it('should have 2 icon buttons for delete and edit', () => {
            const buttons = fixture.debugElement.queryAll(By.css('dot-icon-button'));
            expect(buttons.length).toBe(2);
            expect(buttons[0].componentInstance.icon).toBe('delete_outline');
            expect(buttons[1].componentInstance.icon).toBe('edit');
        });

        it('should have warning icon', () => {
            const warningIcon = fixture.debugElement.query(By.css('dot-icon'));
            expect(warningIcon).toBeTruthy();
            expect(warningIcon.attributes['name']).toBe('warning');
            expect(warningIcon.attributes['size']).toBe('18');
            expect(warningIcon.attributes['ng-reflect-text']).toBe(
                `${component.site.secretsWithWarnings} ${messageServiceMock.get(
                    'apps.invalid.secrets'
                )}`
            );
        });

        it('should emit delete action', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const deleteBtn = fixture.debugElement.queryAll(By.css('dot-icon-button'))[0];

            spyOn(dialogService, 'confirm').and.callFake(conf => {
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

        it('should not have warning icon', () => {
            expect(fixture.debugElement.query(By.css('dot-icon')).attributes['name']).not.toBe(
                'warning'
            );
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
