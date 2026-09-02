import { EMPTY, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    ElementRef,
    inject,
    linkedSignal,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

import { catchError, debounceTime, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotOsgiService,
    OSGI_EXTRA_PACKAGES_RESET
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

export const EXTRA_PACKAGES_RESET_RESULT = 'restart' as const;

/** Debounce delay (ms) before scrolling to a match while the user is typing. */
export const SEARCH_DEBOUNCE_MS = 500;

@Component({
    selector: 'dot-plugins-extra-packages',
    standalone: true,
    imports: [
        FormsModule,
        TextareaModule,
        ButtonModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        DotMessagePipe
    ],
    templateUrl: './dot-plugins-extra-packages.component.html',
    providers: [ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPluginsExtraPackagesComponent {
    readonly #ref = inject(DynamicDialogRef);
    readonly #osgiService = inject(DotOsgiService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    private readonly textareaRef = viewChild<ElementRef<HTMLTextAreaElement>>('packagesTextarea');

    readonly #packagesResponse = toSignal(
        this.#osgiService.getExtraPackages().pipe(
            take(1),
            catchError((error) => {
                this.#httpErrorManager.handle(error);
                return EMPTY;
            })
        )
    );

    extraPackages = linkedSignal(() => this.#packagesResponse()?.entity ?? '');
    saving = signal(false);
    resetting = signal(false);

    searchQuery = signal('');
    readonly currentMatchIndex = signal(0);

    /** All start positions of `searchQuery` within the textarea content (case-insensitive). */
    readonly matchPositions = computed(() => {
        const query = this.searchQuery().toLowerCase();
        if (!query) return [];
        const text = this.extraPackages().toLowerCase();
        const positions: number[] = [];
        let idx = text.indexOf(query);
        while (idx !== -1) {
            positions.push(idx);
            idx = text.indexOf(query, idx + 1);
        }
        return positions;
    });

    readonly matchCount = computed(() => this.matchPositions().length);

    readonly #searchSubject = new Subject<void>();

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(SEARCH_DEBOUNCE_MS), takeUntilDestroyed())
            .subscribe(() => this.#scrollToMatch(0));
    }

    /** Updates the query immediately (so the match count refreshes while typing) but debounces
     *  the actual scroll + selection so the textarea doesn't jump on every keystroke. */
    onSearchChange(query: string): void {
        this.searchQuery.set(query);
        this.currentMatchIndex.set(0);
        this.#searchSubject.next();
    }

    nextMatch(): void {
        const next = (this.currentMatchIndex() + 1) % this.matchCount();
        this.currentMatchIndex.set(next);
        this.#scrollToMatch(next, { focus: true });
    }

    prevMatch(): void {
        const prev = (this.currentMatchIndex() - 1 + this.matchCount()) % this.matchCount();
        this.currentMatchIndex.set(prev);
        this.#scrollToMatch(prev, { focus: true });
    }

    save(): void {
        const text = this.extraPackages();
        this.saving.set(true);
        this.#osgiService
            .updateExtraPackages(text)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.saving.set(false);
                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.saving.set(false);
                this.#ref.close(true);
            });
    }

    close(): void {
        this.#ref.close(false);
    }

    confirmReset(): void {
        this.#confirmationService.confirm({
            message: this.#dotMessageService.get('plugins.extra-packages.reset.confirm.message'),
            header: this.#dotMessageService.get('plugins.extra-packages.reset'),
            acceptLabel: this.#dotMessageService.get('Ok'),
            rejectLabel: this.#dotMessageService.get('Cancel'),
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#doReset()
        });
    }

    #doReset(): void {
        this.resetting.set(true);
        this.#osgiService
            .updateExtraPackages(OSGI_EXTRA_PACKAGES_RESET)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.resetting.set(false);
                    return EMPTY;
                })
            )
            .subscribe(() => {
                this.resetting.set(false);
                this.#ref.close(EXTRA_PACKAGES_RESET_RESULT);
            });
    }

    /**
     * Selects the match at `index` and scrolls to center it. Focus is only moved to the
     * textarea when `focus: true` is passed (explicit ▲/▼ navigation); the search-as-you-type
     * path must leave focus on the search input so further keystrokes don't edit the textarea.
     */
    #scrollToMatch(index: number, { focus = false }: { focus?: boolean } = {}): void {
        const positions = this.matchPositions();
        const textarea = this.textareaRef()?.nativeElement;
        if (!textarea || positions.length === 0) return;

        const start = positions[index];
        const end = start + this.searchQuery().length;

        if (focus) textarea.focus();
        textarea.setSelectionRange(start, end);
        textarea.scrollTop = this.#scrollTopForChar(textarea, start);
    }

    /**
     * Measures the pixel offset of `charIndex` inside the textarea by cloning its text-layout
     * styles into a hidden mirror div and reading a sentinel span's `offsetTop`.
     * This is the standard technique used by text editors and handles word-wrap correctly.
     */
    #scrollTopForChar(textarea: HTMLTextAreaElement, charIndex: number): number {
        const cs = getComputedStyle(textarea);
        const mirror = document.createElement('div');

        Object.assign(mirror.style, {
            position: 'absolute',
            top: '0',
            left: '-9999px',
            overflow: 'hidden',
            whiteSpace: 'pre-wrap',
            wordWrap: 'break-word',
            boxSizing: 'border-box',
            width: `${textarea.clientWidth}px`,
            paddingTop: cs.paddingTop,
            paddingRight: cs.paddingRight,
            paddingBottom: cs.paddingBottom,
            paddingLeft: cs.paddingLeft,
            fontSize: cs.fontSize,
            fontFamily: cs.fontFamily,
            fontWeight: cs.fontWeight,
            lineHeight: cs.lineHeight
        });

        mirror.appendChild(document.createTextNode(textarea.value.substring(0, charIndex)));
        const sentinel = mirror.appendChild(document.createElement('span'));
        sentinel.textContent = '\u200b';

        document.body.appendChild(mirror);
        const top = sentinel.offsetTop;
        document.body.removeChild(mirror);

        return Math.max(0, top - textarea.clientHeight / 2);
    }
}
