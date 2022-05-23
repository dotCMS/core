import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { CommonModule } from '@angular/common';
import { By } from '@angular/platform-browser';
import { ButtonModule } from 'primeng/button';
import { DotAppsConfigurationItemModule } from './dot-apps-configuration-item/dot-apps-configuration-item.module';
import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';

const messages = {
    'apps.configurations.show.more': 'SHOW MORE'
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

describe('DotAppsConfigurationListComponent', () => {
    let component: DotAppsConfigurationListComponent;
    let fixture: ComponentFixture<DotAppsConfigurationListComponent>;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [
                    CommonModule,
                    ButtonModule,
                    DotAppsConfigurationItemModule,
                    HttpClientTestingModule,
                    DotPipesModule
                ],
                declarations: [DotAppsConfigurationListComponent],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    DotAlertConfirmService,
                    ConfirmationService
                ]
            });

            fixture = TestBed.createComponent(DotAppsConfigurationListComponent);
            component = fixture.debugElement.componentInstance;
            component.itemsPerPage = 40;
            component.siteConfigurations = sites;
        })
    );

    describe('With more data to load', () => {
        beforeEach(() => {
            component.hideLoadDataButton = false;
            fixture.detectChanges();
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                    .componentInstance.site
            ).toBe(component.siteConfigurations[0]);
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__show-more'))
                    .nativeElement.outerText
            ).toBe(messageServiceMock.get('apps.configurations.show.more'));
        });

        it('should emit action for edit --> Site Item', () => {
            spyOn(component.edit, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.edit.emit(sites[0]);
            expect(component.edit.emit).toHaveBeenCalledWith(sites[0]);
        });

        it('should emit action for export --> Site Item', () => {
            spyOn(component.export, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.export.emit(sites[0]);
            expect(component.export.emit).toHaveBeenCalledWith(sites[0]);
        });

        it('should emit action for delete --> Site Item', () => {
            spyOn(component.delete, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.delete.emit(sites[0]);
            expect(component.delete.emit).toHaveBeenCalledWith(sites[0]);
        });

        it('should Load More button be enabled', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__show-more'))
                    .nativeElement.disabled
            ).toBe(false);
        });

        it('should Load More button emit action', () => {
            spyOn(component.loadData, 'emit');
            const loadMore = fixture.debugElement.query(
                By.css('.dot-apps-configuration-list__show-more')
            );

            loadMore.triggerEventHandler('click', {});
            expect(component.loadData.emit).toHaveBeenCalledWith({
                first: component.siteConfigurations.length,
                rows: component.itemsPerPage
            });
        });
    });

    describe('With no more data to load', () => {
        beforeEach(() => {
            component.hideLoadDataButton = true;
            fixture.detectChanges();
        });

        it('should Load More button be enabled', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__show-more'))
            ).toBeFalsy();
        });
    });
});
