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

<<<<<<< HEAD
import { ButtonModule } from 'primeng/button';

import { DotDropdownComponent } from '@components/_common/dot-dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { AnnouncementsStore } from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
=======
>>>>>>> main
import { NotificationsService } from '@dotcms/app/api/services/notifications-service';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
<<<<<<< HEAD
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { INotification } from '@models/notifications';

import { DotNotificationsListComponent } from './components/dot-notifications/dot-notifications.component';

import { DotToolbarAnnouncementsComponent } from '../dot-toolbar-announcements/dot-toolbar-announcements.component';
=======
import { INotification } from '@models/notifications';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';
>>>>>>> main

@Component({
    selector: 'dot-toolbar-notifications',
    styleUrls: ['./dot-toolbar-notifications.component.scss'],
    templateUrl: 'dot-toolbar-notifications.component.html',
<<<<<<< HEAD
    standalone: true,
    imports: [
        DotShowHideFeatureDirective,
        ButtonModule,
        DotToolbarAnnouncementsComponent,
        DotDropdownComponent,
        DotNotificationsListComponent,
        DotMessagePipe
    ],
    providers: [AnnouncementsStore]
})
export class DotToolbarNotificationsComponent implements OnInit {
    iframeOverlayService = inject(IframeOverlayService);
    private dotcmsEventsService = inject(DotcmsEventsService);
    private loginService = inject(LoginService);
    private notificationService = inject(NotificationsService);

    readonly #announcementsStore = inject(AnnouncementsStore);
=======
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolbarNotificationsComponent implements OnInit {
    readonly #notificationService = inject(NotificationsService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotcmsEventsService = inject(DotcmsEventsService);
    readonly #loginService = inject(LoginService);
>>>>>>> main

    readonly $overlayPanel = viewChild.required<DotToolbarBtnOverlayComponent>('overlayPanel');

<<<<<<< HEAD
    @ViewChild('toolbarAnnouncements') toolbarAnnouncements: DotToolbarAnnouncementsComponent;
    existsMoreToLoad = false;
    notifications: INotification[] = [];
    notificationsUnreadCount = 0;
    featureFlagAnnouncements = FeaturedFlags.FEATURE_FLAG_ANNOUNCEMENTS;
    annocumentsMarkedAsRead = false;
    activeAnnouncements = false;

    private isNotificationsMarkedAsRead = false;
    private showNotifications = false;

    showUnreadAnnouncement = this.#announcementsStore.showUnreadAnnouncement;
=======
    $notifications = signal<{
        data: INotification[];
        unreadCount: number;
        hasMore: boolean;
    }>({
        data: [],
        unreadCount: 0,
        hasMore: false
    });
>>>>>>> main

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
