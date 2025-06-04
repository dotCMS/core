import { Component, DestroyRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotMessagePipe } from '@dotcms/ui';
import { INotification } from '@models/notifications';

import { DotNotificationListComponent } from './components/dot-notification-list/dot-notification-list.component';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@Component({
    selector: 'dot-toolbar-notifications',
    standalone: true,
    imports: [DotMessagePipe, DotNotificationListComponent, DotToolbarBtnOverlayComponent],
    styleUrls: ['./dot-toolbar-notifications.component.scss'],
    templateUrl: 'dot-toolbar-notifications.component.html'
})
export class DotToolbarNotificationsComponent implements OnInit {
    readonly #notificationService = inject(NotificationsService);
    readonly #destroyRef = inject(DestroyRef);

    readonly $overlayPanel = viewChild.required<DotToolbarBtnOverlayComponent>('overlayPanel');

    $notifications = signal<{
        data: INotification[];
        unreadCount: number;
        hasMore: boolean;
    }>({
        data: [],
        unreadCount: 0,
        hasMore: false
    });

    ngOnInit(): void {
        this.#getNotifications();
    }

    loadMore(): void {
        this.#notificationService
            .getAllNotifications()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((res) => {
                this.$notifications.set({
                    data: res.entity.notifications,
                    unreadCount: res.entity.totalUnreadNotifications,
                    hasMore: res.entity.total > res.entity.notifications.length
                });
            });
    }

    #getNotifications(): void {
        this.#notificationService
            .getLastNotifications()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((res) => {
                this.$notifications.set({
                    data: res.entity.notifications,
                    unreadCount: res.entity.totalUnreadNotifications,
                    hasMore: res.entity.total > res.entity.notifications.length
                });
            });
    }

    onDismissNotification($event: { id: string }): void {
        const notificationId = $event.id;

        this.#notificationService
            .dismissNotifications({ items: [notificationId] })
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((res) => {
                if (res.errors.length) {
                    return;
                }

                this.$notifications.update((state) => ({
                    ...state,
                    data: state.data.filter((item) => {
                        return item.id !== notificationId;
                    })
                }));

                if (this.$notifications().unreadCount) {
                    this.$notifications.update((state) => ({
                        ...state,
                        unreadCount: state.unreadCount - 1
                    }));
                }

                if (!this.$notifications().data.length && !this.$notifications().unreadCount) {
                    this.clearNotitications();
                }
            });
    }

    private clearNotitications(): void {
        this.$notifications.set({
            data: [],
            unreadCount: 0,
            hasMore: false
        });
        this.$overlayPanel().hide();
    }
}
