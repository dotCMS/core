import { Component, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';

import { DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { NotificationsService } from '../../../../../api/services/notifications-service';
import { INotification } from '../../../../../shared/models/notifications/notification.model';
import { DotDropdownComponent } from '../../../_common/dot-dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotToolbarAnnouncementsComponent } from '../dot-toolbar-announcements/dot-toolbar-announcements.component';
import { AnnouncementsStore } from '../dot-toolbar-announcements/store/dot-announcements.store';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-toolbar-notifications',
    styleUrls: ['./dot-toolbar-notifications.component.scss'],
    templateUrl: 'dot-toolbar-notifications.component.html'
})
export class DotToolbarNotificationsComponent implements OnInit {
    @ViewChild(DotDropdownComponent, { static: true }) dropdown: DotDropdownComponent;

    @ViewChild('toolbarAnnouncements') toolbarAnnouncements: DotToolbarAnnouncementsComponent;
    existsMoreToLoad = false;
    notifications: INotification[] = [];
    notificationsUnreadCount = 0;
    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;
    annocumentsMarkedAsRead = false;
    activeAnnouncements = false;

    private isNotificationsMarkedAsRead = false;
    private showNotifications = false;

    showUnreadAnnouncement = this.announcementsStore.showUnreadAnnouncement;

    constructor(
        public iframeOverlayService: IframeOverlayService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private notificationService: NotificationsService,
        private announcementsStore: AnnouncementsStore
    ) {}

    ngOnInit(): void {
        this.getNotifications();
        this.subscribeToNotifications();

        this.loginService.watchUser(this.getNotifications.bind(this));
        this.announcementsStore.load();
    }

    dismissAllNotifications(): void {
        const items = this.notifications.map((item) => item.id);
        this.notificationService.dismissNotifications({ items: items }).subscribe((res) => {
            // TODO: I think we should get here res and err
            if (res.errors.length) {
                return;
            }

            this.clearNotitications();
        });
    }

    onDismissNotification($event): void {
        const notificationId = $event.id;

        this.notificationService
            .dismissNotifications({ items: [notificationId] })
            .subscribe((res) => {
                if (res.errors.length) {
                    return;
                }

                this.notifications = this.notifications.filter((item) => {
                    return item.id !== notificationId;
                });

                if (this.notificationsUnreadCount) {
                    this.notificationsUnreadCount--;
                }

                if (!this.notifications.length && !this.notificationsUnreadCount) {
                    this.clearNotitications();
                }
            });
    }

    loadMore(): void {
        this.notificationService.getAllNotifications().subscribe((res) => {
            this.notificationsUnreadCount = res.entity.totalUnreadNotifications;
            this.notifications = res.entity.notifications;
            this.existsMoreToLoad = false;
        });
    }

    toggleNotifications(): void {
        this.showNotifications = !this.showNotifications;

        if (this.showNotifications && !this.isNotificationsMarkedAsRead) {
            this.markAllAsRead();
        }
    }

    private clearNotitications(): void {
        this.notifications = [];
        this.notificationsUnreadCount = 0;
        this.showNotifications = false;
        this.dropdown.closeIt();
    }

    private getNotifications(): void {
        this.notificationService.getLastNotifications().subscribe((res) => {
            this.notificationsUnreadCount = res.entity.totalUnreadNotifications;
            this.notifications = res.entity.notifications;
            this.existsMoreToLoad = res.entity.total > res.entity.notifications.length;
        });
    }

    private markAllAsRead(): void {
        this.notificationService.markAllAsRead().subscribe(() => {
            this.isNotificationsMarkedAsRead = true;
            this.notificationsUnreadCount = 0;
        });
    }

    private subscribeToNotifications(): void {
        this.dotcmsEventsService
            .subscribeTo<INotification>('NOTIFICATION')
            .subscribe((data: INotification) => {
                this.notifications.unshift(data);
                this.notificationsUnreadCount++;
                this.isNotificationsMarkedAsRead = false;
            });
    }

    onActiveAnnouncements(event: CustomEvent): void {
        this.activeAnnouncements = true;
        this.toolbarAnnouncements.toggleDialog(event);
    }
    markAnnocumentsAsRead(): void {
        this.activeAnnouncements = false;
        this.announcementsStore.markAnnouncementsAsRead();
    }
}
