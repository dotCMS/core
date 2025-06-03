import { Component, DestroyRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotMessagePipe } from '@dotcms/ui';
import { INotification } from '@models/notifications';

import { DotNotificationListComponent } from '../dot-notification-list/dot-notification-list.component';

@Component({
    selector: 'dot-notifications',
    standalone: true,
    imports: [ButtonModule, OverlayPanelModule, DotMessagePipe, DotNotificationListComponent],
    styleUrls: ['./dot-notifications.component.scss'],
    templateUrl: 'dot-notifications.component.html'
})
export class DotNotificationsComponent implements OnInit {
    readonly #notificationService = inject(NotificationsService);
    readonly #destroyRef = inject(DestroyRef);
    readonly $overlayPanel = viewChild.required<OverlayPanel>('op');

    $isOverlayPanelVisible = signal(false);

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

    onShow(): void {
        this.$isOverlayPanelVisible.set(true);
    }

    onHide(): void {
        this.$isOverlayPanelVisible.set(false);
    }

    onToggle(): void {
        const isVisible = this.$isOverlayPanelVisible();

        if (isVisible) {
            this.onHide();
        } else {
            this.onShow();
        }
    }
}
