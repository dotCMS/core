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
    DotLocaleTagPipe,
    DotMessagePipe
} from '@dotcms/ui';

/** What the backend puts in a user's name field when the user cannot be resolved. */
const UNRESOLVED_NAME = 'N/A';

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
 * The body of a card in `dot-folder-list-view`'s grid layout, which the shell hands over as the
 * listing's `#card` template. Presentational only: the listing owns the selection, the keyboard and
 * the drag, and draws the element the card sits in (role, tab stop, `aria-selected`), which it
 * labels by `{cardId}-label` and describes by `{cardId}-description`. The card reports three
 * gestures:
 *
 * - `open` — a plain click anywhere that is not one of the card's own controls.
 * - `toggleSelect` — a click on the checkbox (the listing reads Shift off the event for ranges).
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
        DotLocaleTagPipe,
        DotMessagePipe,
        FormsModule,
        TagModule,
        TooltipModule
    ],
    templateUrl: './dot-content-drive-grid-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'block h-full',
        '(click)': 'onCardClick($event)'
    }
})
export class DotContentDriveGridCardComponent {
    /** The folder or content item the card shows. */
    $item = input.required<DotContentDriveItem>({ alias: 'item' });

    /**
     * The id the listing gave this card. The title and the summary take ids built from it, which
     * is how the listing names and describes the card to screen readers.
     */
    $cardId = input.required<string>({ alias: 'cardId' });

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
     * The avatar's letter: the first of the editor's name. Drawn by PrimeNG's avatar alone, never
     * through the Gravatar directive: the listing carries a name, and when a user has no first or
     * last name that name is their email, which the directive would hash and send to gravatar.com.
     */
    protected readonly $editorInitial = computed(
        () => this.$editorName()?.charAt(0).toUpperCase() ?? null
    );

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
     * Opens the item on a plain click.
     *
     * A click with a modifier never opens, the same rule the table keeps for Shift: those keys
     * belong to selecting. The second click of a double-click is ignored too, so one double-click
     * opens the item once.
     *
     * A click on the ⋮ button reaches here too (see {@link onMenuClick}) and is ignored.
     *
     * @param event The click that reached the card.
     */
    protected onCardClick(event: MouseEvent): void {
        if (event.detail > 1 || event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) {
            return;
        }

        if ((event.target as Element | null)?.closest('[data-testid="grid-card-menu"]')) {
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
     * Reports a ⋮ click.
     *
     * The click is left to bubble, unlike the checkbox's: the actions menu closes on PrimeNG's
     * document click listener, and stopping it here left the menu open on a second click, where the
     * table's ⋮ closes it. {@link onCardClick} ignores it on the way up, so it never opens the item.
     *
     * @param event The click on the button.
     */
    protected onMenuClick(event: MouseEvent): void {
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
