import { DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    inject,
    OnDestroy,
    output,
    signal,
    viewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { Menu, MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentletVersion } from '@dotcms/dotcms-models';
import {
    DotCopyButtonComponent,
    DotGravatarDirective,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../../../../models/dot-edit-content.model';

/**
 * Component that displays a single history timeline item with version details and actions.
 * Shows version information, user details, status chips, and provides action menu.
 *
 * @example
 * ```html
 * <dot-history-timeline-item
 *   [item]="versionItem"
 *   (actionTriggered)="onTimelineItemAction($event)">
 * </dot-history-timeline-item>
 * ```
 */
@Component({
    selector: 'dot-history-timeline-item',
    imports: [
        AvatarModule,
        ButtonModule,
        ChipModule,
        MenuModule,
        TooltipModule,
        DotCopyButtonComponent,
        DotGravatarDirective,
        DotMessagePipe,
        DotRelativeDatePipe,
        DatePipe
    ],
    providers: [DatePipe],
    templateUrl: './dot-history-timeline-item.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineItemComponent implements OnDestroy {
    private readonly datePipe = inject(DatePipe);
    private readonly dotMessageService = inject(DotMessageService);

    private static readonly MENU_HIDE_DELAY_MS = 200;
    private hideTimer: ReturnType<typeof setTimeout> | null = null;
    private menuEnterHandler: (() => void) | null = null;
    private menuLeaveHandler: (() => void) | null = null;

    /**
     * The version item to display
     * @readonly
     */
    $item = input.required<DotCMSContentletVersion>({ alias: 'item' });

    /**
     * The index of this item in the timeline (0-based)
     * Used to determine which actions are available
     * @readonly
     */
    $itemIndex = input<number>(0, { alias: 'itemIndex' });

    /**
     * Whether this timeline item is currently active (being viewed)
     * @readonly
     */
    $isActive = input<boolean>(false, { alias: 'isActive' });

    /**
     * Event emitted when an action is triggered on the timeline item
     */
    actionTriggered = output<DotHistoryTimelineItemAction>();

    /**
     * Reference to the PrimeNG menu so we can programmatically hide it
     * after a command callback completes.
     */
    readonly versionMenu = viewChild<Menu>('versionMenu');

    /**
     * Signal for cached translations map
     * Contains static translations for menu labels
     */
    private readonly $labels = signal({
        restore: this.dotMessageService.get('edit.content.sidebar.history.menu.restore'),
        compare: this.dotMessageService.get('edit.content.sidebar.history.menu.compare'),
        delete: this.dotMessageService.get('edit.content.sidebar.history.menu.delete')
    });

    /**
     * Last 6 characters of the version inode — used as the label of the
     * inode copy button so the user sees a short, recognizable handle
     * while still being able to copy the full inode.
     */
    readonly $inodeShort = computed(() => {
        const inode = this.$item().inode;
        return inode ? inode.slice(-6) : '';
    });

    /**
     * Computed signal that generates menu items for version actions based on
     * the version's status:
     * - Draft (working && !live): no actions
     * - Published (live): Restore + Compare
     * - Historical (!working && !live): Restore + Compare + Delete
     */
    readonly $menuItems = computed<MenuItem[]>(() => {
        const labels = this.$labels();
        const item = this.$item();
        const isDraft = item.working && !item.live;
        const isPublished = item.live;

        if (isDraft) {
            return [];
        }

        const items: MenuItem[] = [
            {
                id: 'restore',
                label: labels.restore,
                command: () => {
                    this.versionMenu()?.hide();
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.RESTORE,
                        item
                    });
                }
            },
            {
                id: 'compare',
                label: labels.compare,
                command: () => {
                    this.versionMenu()?.hide();
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.COMPARE,
                        item
                    });
                }
            }
        ];

        if (!isPublished) {
            items.push({
                id: 'delete',
                label: labels.delete,
                command: () => {
                    this.versionMenu()?.hide();
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.DELETE,
                        item
                    });
                }
            });
        }

        return items;
    });

    /**
     * Computed signal that determines the timeline marker CSS class based on content status
     * Uses reactive approach for better performance and consistency
     */
    readonly $timelineMarkerClass = computed(() => {
        const item = this.$item();

        if (item.live) {
            return 'dot-history-timeline-item__marker--live';
        } else if (item.working) {
            return 'dot-history-timeline-item__marker--draft';
        }

        return '';
    });

    /**
     * Schedules hiding the version menu after a short delay so the user has time
     * to move the cursor from the kebab button into the overlay (or vice versa)
     * without it disappearing immediately.
     */
    protected scheduleHideMenu(): void {
        this.cancelHideMenu();
        this.hideTimer = setTimeout(() => {
            this.versionMenu()?.hide();
            this.hideTimer = null;
        }, DotHistoryTimelineItemComponent.MENU_HIDE_DELAY_MS);
    }

    /**
     * Cancels any pending hide; called when the cursor re-enters the kebab
     * wrapper or the menu overlay.
     */
    protected cancelHideMenu(): void {
        if (this.hideTimer !== null) {
            clearTimeout(this.hideTimer);
            this.hideTimer = null;
        }
    }

    /**
     * When the popup overlay is shown, attach mouseenter/mouseleave listeners so
     * the cursor can leave the kebab wrapper and enter the overlay without the
     * menu closing.
     */
    protected onMenuShown(): void {
        const overlay = this.getMenuOverlayElement();
        if (!overlay) {
            return;
        }
        this.menuEnterHandler = () => this.cancelHideMenu();
        this.menuLeaveHandler = () => this.scheduleHideMenu();
        overlay.addEventListener('mouseenter', this.menuEnterHandler);
        overlay.addEventListener('mouseleave', this.menuLeaveHandler);
    }

    protected onMenuHidden(): void {
        this.cancelHideMenu();
        this.detachOverlayListeners();
    }

    ngOnDestroy(): void {
        this.cancelHideMenu();
        this.detachOverlayListeners();
    }

    private detachOverlayListeners(): void {
        const overlay = this.getMenuOverlayElement();
        if (overlay) {
            if (this.menuEnterHandler) {
                overlay.removeEventListener('mouseenter', this.menuEnterHandler);
            }
            if (this.menuLeaveHandler) {
                overlay.removeEventListener('mouseleave', this.menuLeaveHandler);
            }
        }
        this.menuEnterHandler = null;
        this.menuLeaveHandler = null;
    }

    private getMenuOverlayElement(): HTMLElement | null {
        const menu = this.versionMenu();
        const ref = menu?.containerViewChild?.();
        return (ref?.nativeElement as HTMLElement | undefined) ?? null;
    }
}
