import { NgClass } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item/dot-apps-configuration-item.component';
import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list.component';

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

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                NgClass,
                ButtonModule,
                DotAppsConfigurationItemComponent,
                HttpClientTestingModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                DotAppsConfigurationListComponent
            ],
            declarations: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotAlertConfirmService,
                ConfirmationService
            ]
        });

        fixture = TestBed.createComponent(DotAppsConfigurationListComponent);
        component = fixture.debugElement.componentInstance;
        fixture.componentRef.setInput('itemsPerPage', 40);
        fixture.componentRef.setInput('siteConfigurations', sites);
    }));

    describe('With more data to load', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('hideLoadDataButton', false);
            fixture.detectChanges();
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement
                    .queryAll(By.css('dot-apps-configuration-item'))[0]
                    .componentInstance.site()
            ).toBe(component.siteConfigurations()[0]);
            expect(
                fixture.debugElement
                    .query(By.css('.dot-apps-configuration-list__show-more'))
                    .nativeElement.textContent.trim()
            ).toBe(messageServiceMock.get('apps.configurations.show.more'));
        });

        it('should emit action for edit --> Site Item', () => {
            jest.spyOn(component.edit, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.edit.emit(sites[0]);
            expect(component.edit.emit).toHaveBeenCalledWith(sites[0]);
            expect(component.edit.emit).toHaveBeenCalledTimes(1);
        });

        it('should emit action for export --> Site Item', () => {
            jest.spyOn(component.export, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.export.emit(sites[0]);
            expect(component.export.emit).toHaveBeenCalledWith(sites[0]);
            expect(component.export.emit).toHaveBeenCalledTimes(1);
        });

        it('should emit action for delete --> Site Item', () => {
            jest.spyOn(component.delete, 'emit');
            const siteItem = fixture.debugElement.queryAll(By.css('dot-apps-configuration-item'))[0]
                .componentInstance;

            siteItem.delete.emit(sites[0]);
            expect(component.delete.emit).toHaveBeenCalledWith(sites[0]);
            expect(component.delete.emit).toHaveBeenCalledTimes(1);
        });

        it('should Load More button be enabled', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__show-more'))
                    .nativeElement.disabled
            ).toBe(false);
        });

        it('should Load More button emit action', () => {
            jest.spyOn(component.loadData, 'emit');
            const loadMore = fixture.debugElement.query(
                By.css('.dot-apps-configuration-list__show-more')
            );

            loadMore.triggerEventHandler('click', {});
            expect(component.loadData.emit).toHaveBeenCalledWith({
                first: component.siteConfigurations().length,
                rows: component.itemsPerPage()
            });
        });
    });

    describe('With no more data to load', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('hideLoadDataButton', true);
            fixture.detectChanges();
        });

        it('should Load More button be enabled', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-list__show-more'))
            ).toBeFalsy();
        });
    });
});
