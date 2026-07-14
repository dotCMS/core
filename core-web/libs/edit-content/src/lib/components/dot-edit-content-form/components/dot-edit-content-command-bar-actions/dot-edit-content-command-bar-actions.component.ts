import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    input
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotPermissionsIframeDialogComponent, DotPermissionsIframeDialogData } from '@dotcms/ui';

import { DotReferencesDialogData } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarReferencesDialogComponent } from '../../../dot-edit-content-sidebar/components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-references-dialog/dot-edit-content-sidebar-references-dialog.component';
import { DotRulesDialogComponent } from '../../../dot-edit-content-sidebar/components/dot-edit-content-sidebar-rules/components/rules-dialog/rules-dialog.component';

export const CONTENTLET_PERMISSIONS_IFRAME_PATH = '/html/portlet/ext/contentlet/permissions.jsp';

/**
 * Overflow ("...") menu for the edit-content command bar.
 *
 * Renders a single trigger button that toggles a popup menu with the secondary
 * contentlet actions (Permissions, Rules, View references). Each action opens its
 * own dialog; this component owns the dialog lifecycle and guards against opening
 * duplicate instances.
 */
@Component({
    selector: 'dot-edit-content-command-bar-actions',
    imports: [ButtonModule, MenuModule],
    templateUrl: './dot-edit-content-command-bar-actions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DialogService]
})
export class DotEditContentCommandBarActionsComponent {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    #permissionsDialogRef: DynamicDialogRef | undefined;
    #rulesDialogRef: DynamicDialogRef | undefined;
    #referencesDialogRef: DynamicDialogRef | undefined;

    /** The contentlet the command bar acts on. Used as a fallback source for the title. */
    readonly contentlet = input<DotCMSContentlet | null>(null);

    /** Contentlet identifier used by the permissions, rules and references dialogs. */
    readonly identifier = input<string>('');

    /** Contentlet language id used by the permissions dialog. */
    readonly languageId = input<number>(0);

    /** Whether the contentlet is a page. Controls visibility of the Rules action. */
    readonly isPage = input<boolean>(false);

    /** Whether the contentlet has at least one page reference. Disables the references action. */
    readonly hasReferences = input<boolean>(false);

    /** Contentlet title used for the references dialog header. */
    readonly title = input<string>('');

    /**
     * Pass-through styling for the overflow menu: flush list, square item content,
     * and a taller hit area for each link. Replaces the deprecated `styleClass`.
     */
    readonly menuPt = {
        list: { class: 'p-0!' },
        itemContent: { class: 'rounded-none!' },
        itemLink: { class: 'py-3!' }
    };

    /** Menu model for the overflow popup. Rebuilt reactively from the inputs. */
    readonly $model = computed<MenuItem[]>(() => {
        const items: MenuItem[] = [
            {
                label: this.#dotMessageService.get('edit.content.sidebar.permissions.title'),
                testId: 'command-bar-action-permissions',
                command: () => this.openPermissionsDialog()
            }
        ];

        if (this.isPage()) {
            items.push({
                label: this.#dotMessageService.get('edit.content.sidebar.rules.title'),
                testId: 'command-bar-action-rules',
                command: () => this.openRulesDialog()
            });
        }

        items.push(
            { separator: true },
            {
                label: this.#dotMessageService.get('edit.content.sidebar.command-bar.references'),
                testId: 'command-bar-action-references',
                disabled: !this.hasReferences(),
                command: () => this.openReferencesDialog()
            }
        );

        return items;
    });

    constructor() {
        this.#destroyRef.onDestroy(() => {
            this.#permissionsDialogRef?.close();
            this.#rulesDialogRef?.close();
            this.#referencesDialogRef?.close();
        });
    }

    /**
     * Opens the permissions dialog with an iframe for the current contentlet.
     * Prevents opening multiple instances if the action is triggered repeatedly.
     */
    openPermissionsDialog(): void {
        if (this.#permissionsDialogRef) return;

        const id = this.identifier();
        const langId = this.languageId();
        if (!id || !langId) return;

        this.#permissionsDialogRef = this.#dialogService.open(DotPermissionsIframeDialogComponent, {
            header: this.#dotMessageService.get('edit.content.sidebar.permissions.title'),
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: {
                url: this.#buildPermissionsUrl(id, langId)
            } satisfies DotPermissionsIframeDialogData,
            modal: true,
            appendTo: 'body',
            closeOnEscape: true,
            closable: true,
            draggable: false,
            resizable: false,
            position: 'center'
        });

        this.#permissionsDialogRef.onClose
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.#permissionsDialogRef = undefined;
            });
    }

    /**
     * Opens the rules dialog for the current contentlet.
     * Prevents opening multiple instances if the action is triggered repeatedly.
     */
    openRulesDialog(): void {
        if (this.#rulesDialogRef) return;

        const id = this.identifier();
        if (!id) return;

        const header = this.#dotMessageService.get('edit.content.sidebar.rules.title');
        this.#rulesDialogRef = this.#dialogService.open(DotRulesDialogComponent, {
            header,
            width: 'min(92vw, 75rem)',
            data: { identifier: id },
            modal: true,
            appendTo: 'body',
            closeOnEscape: true,
            closable: true,
            draggable: false,
            keepInViewport: false,
            resizable: false,
            position: 'center'
        });

        this.#rulesDialogRef.onClose.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
            this.#rulesDialogRef = undefined;
        });
    }

    /** Opens the references dialog showing all pages that include this contentlet. */
    openReferencesDialog(): void {
        if (this.#referencesDialogRef) return;

        // The menu item is disabled when there are no references; guard the public method too
        // so a direct/keyboard invocation can't open an empty dialog.
        if (!this.hasReferences()) return;

        const identifier = this.identifier();
        if (!identifier) return;

        this.#referencesDialogRef = this.#dialogService.open(
            DotEditContentSidebarReferencesDialogComponent,
            {
                header: this.#dotMessageService.get(
                    'edit.content.sidebar.references.dialog.title',
                    this.title() || this.contentlet()?.title || ''
                ),
                width: 'min(92vw, 60rem)',
                contentStyle: { padding: '0', overflow: 'auto' },
                data: { identifier } satisfies DotReferencesDialogData,
                modal: true,
                appendTo: 'body',
                closeOnEscape: true,
                closable: true,
                draggable: false,
                resizable: false,
                position: 'center'
            }
        );

        this.#referencesDialogRef.onClose
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.#referencesDialogRef = undefined;
            });
    }

    #buildPermissionsUrl(identifier: string, languageId: number): string {
        const params = new URLSearchParams({
            contentletId: identifier,
            languageId: String(languageId),
            popup: 'true'
        });
        return `${CONTENTLET_PERMISSIONS_IFRAME_PATH}?${params.toString()}`;
    }
}
