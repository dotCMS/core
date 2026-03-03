import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    inject,
    OnInit,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';

import { DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

import { DotNotificationItemComponent } from './components/dot-notification-item/dot-notification-item.component';

import { NotificationsService } from '../../../../../api/services/notifications-service';
import { INotification } from '../../../../../shared/models/notifications/notification.model';
import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@Component({
    selector: 'dot-toolbar-notifications',
    templateUrl: 'dot-toolbar-notifications.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotToolbarBtnOverlayComponent,
        DotNotificationItemComponent
    ]
})
export class DotToolbarNotificationsComponent implements OnInit {
    readonly #notificationService = inject(NotificationsService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #loginService = inject(LoginService);

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
        this.#subscribeToNotifications();
        this.#loginService.watchUser(this.#getNotifications.bind(this));
    }

    loadMore(): void {
        this.#notificationService
            .getAllNotifications()
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((res) => {
                this.$notifications.set({
                    data: res.entity.notifications,
                    unreadCount: res.entity.totalUnreadNotifications,
                    hasMore: false
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

    dismissAllNotifications(): void {
        const items = this.$notifications().data.map((item) => item.id);
        this.#notificationService.dismissNotifications({ items: items }).subscribe((res) => {
            if (res.errors.length) {
                return;
            }

            this.#clearNotitications();
        });
    }

    #subscribeToNotifications(): void {
        this.#dotcmsEventsService
            .subscribeTo<INotification>('NOTIFICATION')
            .subscribe((data: INotification) => {
                this.$notifications.update((state) => ({
                    data: [data, ...state.data],
                    unreadCount: state.unreadCount + 1,
                    hasMore: false
                }));
            });
    }

    markAllAsRead(): void {
        this.#notificationService.markAllAsRead().subscribe(() => {
            this.$notifications.update((state) => ({
                ...state,
                unreadCount: 0
            }));
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
                    data: state.data.filter((item) => item.id !== notificationId),
                    unreadCount: Math.max(0, state.unreadCount - 1)
                }));

                const { data, unreadCount } = this.$notifications();

                if (data.length === 0 && unreadCount === 0) {
                    this.#clearNotitications();
                }
            });
    }

    #clearNotitications(): void {
        this.$notifications.set({
            data: [],
            unreadCount: 0,
            hasMore: false
        });
        this.$overlayPanel().hide();
    }
}
