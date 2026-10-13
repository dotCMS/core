import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotLanguage
} from '@dotcms/dotcms-models';
import {
    DotContentletStatusBadgeComponent,
    DotContentThumbnailComponent,
    DotGravatarDirective,
    DotLocaleTagPipe,
    DotMessagePipe
} from '@dotcms/ui';

/** What the backend puts in a user's name field when the user cannot be resolved. */
const UNRESOLVED_NAME = 'N/A';

/** Gives each card's described-by targets ids no other card on the page can share. */
let nextCardId = 0;

/**
 * Returns a usable display name, or `null` for the values the backend uses to say it has none.
 *
 * @param name The name as the listing carries it.
 */
function displayName(name: string | null | undefined): string | null {
    const trimmed = name?.trim();

    return trimmed && trimmed !== UNRESOLVED_NAME ? trimmed : null;
}

/**
 * One Content Drive item drawn as a card (#37930): the preview on top, with the editor's avatar and
 * the status over its top-left corner and the selection checkbox over its top-right corner; the
 * title below; then the language, the content type, the lock state and the "more actions" button.
 *
 * Presentational only. The grid owns the selection, the keyboard and the drag, and sets the card's
 * role, tab stop and `aria-selected` on its host. The card reports three gestures:
 *
 * - `open` — a plain click anywhere that is not one of the card's own controls.
 * - `toggleSelect` — a click on the checkbox (the grid reads Shift off the event for ranges).
 * - `menu` — a click on the ⋮ button.
 */
@Component({
    selector: 'dot-content-drive-grid-card',
    imports: [
        AvatarModule,
        ButtonModule,
        CardModule,
        CheckboxModule,
        DotContentletStatusBadgeComponent,
        DotContentThumbnailComponent,
        DotGravatarDirective,
        DotLocaleTagPipe,
        DotMessagePipe,
        FormsModule,
        TagModule,
        TooltipModule
    ],
    templateUrl: './dot-content-drive-grid-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'group block min-w-0 rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-(--p-primary-color) focus-visible:ring-offset-2',
        '[class.opacity-60]': '$busy()',
        '[class.pointer-events-none]': '$busy()',
        '[attr.aria-busy]': '$busy() ? "true" : null',
        '[attr.aria-label]': '$title()',
        '[attr.aria-describedby]': '$describedBy()',
        '(click)': 'onCardClick($event)'
    }
})
export class DotContentDriveGridCardComponent {
    /** The folder or content item the card shows. */
    $item = input.required<DotContentDriveItem>({ alias: 'item' });

    /** Whether the item is in the Drive's selection. */
    $selected = input(false, { alias: 'selected' });

    /** Whether a bulk action is running on the item: dimmed, not clickable, spinner for preview. */
    $busy = input(false, { alias: 'busy' });

    /** A grid the user cannot act on: no ⋮ button. */
    $readOnly = input(false, { alias: 'readOnly' });

    /** Whether items are being dragged over this card. */
    $dragOver = input(false, { alias: 'dragOver' });

    /** Languages by id, to show the item's locale tag. */
    $languagesMap = input(new Map<number, DotLanguage>(), { alias: 'languagesMap' });

    /** A plain click on the card: the item should open. */
    open = output<MouseEvent>();

    /** A click on the checkbox: the item should be added to or removed from the selection. */
    toggleSelect = output<MouseEvent>();

    /** A click on the ⋮ button: the item's actions menu should open. */
    menu = output<MouseEvent>();

    /** Prefix for the ids the card's description points at. */
    protected readonly id = `dot-grid-card-${nextCardId++}`;

    protected readonly $isFolder = computed(() => this.$item().type === 'folder');

    /** The item as content, or `null` for a folder. Narrows the template's reads. */
    protected readonly $contentlet = computed(() =>
        this.$isFolder() ? null : (this.$item() as DotCMSContentlet)
    );

    /** A folder shows its name, as the table does; content shows its title. */
    protected readonly $title = computed(() => {
        const item = this.$item();

        return this.$isFolder() ? (item as DotContentDriveFolder).name : item.title;
    });

    /**
     * The user the table shows in its Edited By column: the last editor for content, the owner for
     * a folder. `null` when there is none to show.
     */
    protected readonly $editorName = computed(() => {
        const item = this.$item();

        return displayName(
            this.$isFolder()
                ? (item as DotContentDriveFolder).owner
                : (item as DotCMSContentlet).modUserName
        );
    });

    /**
     * Who holds the lock, when the listing can name them. Drive results carry `lockedBy` as an
     * object with the holder's names; some other sources carry only their id, which cannot be shown.
     */
    protected readonly $lockHolder = computed(() => {
        const contentlet = this.$contentlet();

        if (!contentlet?.locked) {
            return null;
        }

        const lockedBy = contentlet.lockedBy;

        if (lockedBy && typeof lockedBy === 'object') {
            const first = displayName(lockedBy.firstName);

            return first ? displayName(`${first} ${lockedBy.lastName ?? ''}`) : null;
        }

        return displayName(contentlet.lockedByName);
    });

    /**
     * What a screen reader reads after the title: status, language, type, editor and lock, in the
     * order they sit on the card. Points at the visible elements where they have text, and at
     * visually hidden labels for the avatar and the lock, which are pictures.
     */
    protected readonly $describedBy = computed(() => {
        const ids = this.$isFolder()
            ? ['type', 'editor']
            : ['status', 'language', 'type', 'editor', 'lock'];

        return ids.map((part) => `${this.id}-${part}`).join(' ');
    });

    /**
     * Opens the item on a plain click.
     *
     * A click with a modifier never opens, the same rule the table keeps for Shift: those keys
     * belong to selecting. The second click of a double-click is ignored too, so one double-click
     * opens the item once.
     *
     * @param event The click that reached the card.
     */
    protected onCardClick(event: MouseEvent): void {
        if (event.detail > 1 || event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }

        this.open.emit(event);
    }

    /**
     * Reports a checkbox click. Kept off the card so ticking never opens the item.
     *
     * @param event The click on the checkbox.
     */
    protected onCheckboxClick(event: MouseEvent): void {
        event.stopPropagation();
        event.preventDefault();
        this.toggleSelect.emit(event);
    }

    /**
     * Reports a ⋮ click. Kept off the card so it never opens the item.
     *
     * @param event The click on the button.
     */
    protected onMenuClick(event: MouseEvent): void {
        event.stopPropagation();
        this.menu.emit(event);
    }

    /**
     * Stops a click on the avatar or the lock from opening the item. On a touch screen that tap is
     * how the user reads the name behind it.
     *
     * @param event The click.
     */
    protected keepOnCard(event: Event): void {
        event.stopPropagation();
    }
}
