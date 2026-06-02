import { Subject, of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { AutoComplete, AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { InputTextModule } from 'primeng/inputtext';
import { Select } from 'primeng/select';

import { catchError, switchMap } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotContentSearchService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { LINK_SELECTION_KEY } from '../../extensions/selection-preserve.extension';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { isValidHttpUrl, linkHrefValidator } from '../../utils/url.utils';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

/** Rel-attribute values exposed in the Advanced section's dropdown. */
const REL_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
    { value: 'noopener noreferrer', label: 'noopener noreferrer' },
    { value: 'noopener', label: 'noopener' },
    { value: 'noreferrer', label: 'noreferrer' },
    { value: 'nofollow', label: 'nofollow' },
    { value: 'sponsored', label: 'sponsored' },
    { value: 'ugc', label: 'ugc' }
];

/** Max page-search suggestions shown in the URL autocomplete. Mirrors the legacy editor. */
const PAGE_SEARCH_LIMIT = 5;

/** A single internal-page suggestion rendered in the URL autocomplete dropdown. */
interface PageSearchResult {
    /** Contentlet title — the primary line in the suggestion row. */
    name: string;
    /** The page URL/path written into the link `href` when selected. */
    url: string;
    hasTitleImage?: boolean;
    inode?: string;
}

/** Narrow shape of the `/api/content/_search` response we consume. */
interface ContentletSearchEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

@Component({
    selector: 'dot-link-popover',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        AutoCompleteModule,
        InputTextModule,
        Select,
        EditorPopoverComponent,
        DotMessagePipe
    ],
    templateUrl: './link-popover.component.html'
})
export class LinkPopoverComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorPopoverService);

    readonly #contentSearch = inject(DotContentSearchService);
    readonly #store = inject(EditorStore);

    protected readonly relOptions = REL_OPTIONS;

    /** AutoComplete instance — used to force-hide the overlay when an external URL is typed. */
    protected readonly autoComplete = viewChild<AutoComplete>(AutoComplete);

    /**
     * The autocomplete's `[ngModel]`. Holds the typed string while the user types and,
     * momentarily, the selected {@link PageSearchResult} object on select — normalised back
     * to its `url` string in {@link onSelectPage}. The reactive `href` control (not this
     * signal) is the source of truth; it is kept in sync by an effect below.
     */
    protected readonly linkModel = signal<string | PageSearchResult>('');

    /** Internal-page suggestions for the URL field, refreshed as the user types. */
    protected readonly suggestions = signal<PageSearchResult[]>([]);

    /**
     * True when the current URL value parses as an http(s) URL. Suppresses the autocomplete
     * overlay (incl. the empty message) so typing/pasting an external URL is never blocked.
     */
    protected readonly isExternalUrl = signal(false);

    /** Debounced search queries; switchMap cancels in-flight requests as the user types. */
    readonly #searchTerm$ = new Subject<string>();

    /**
     * PrimeNG passthrough config for the rel `<p-select>`. Mirrors the toolbar's block-type
     * select so the two dropdowns stay visually consistent. Extract to a shared util when
     * a third caller appears; not now (premature abstraction).
     */
    protected readonly selectPt = {
        root: 'bg-white border border-indigo-200 rounded-md text-sm text-indigo-900 hover:border-indigo-300 transition-colors',
        label: '!text-indigo-900',
        dropdown: 'w-7 text-indigo-500',
        panel: 'bg-white border border-indigo-200 rounded-md shadow-lg mt-1',
        list: 'p-1',
        item: 'px-3 py-1.5 text-sm text-slate-700 rounded hover:bg-indigo-50 hover:text-indigo-700 aria-selected:bg-indigo-600 aria-selected:text-white'
    };

    protected readonly isEditing = computed(
        () => this.manager.linkPayload()?.initialValues != null
    );

    /**
     * Tracks whether the Advanced (Title / Aria Label / Rel) section is visible.
     * Auto-expands when an existing link's payload carries any of those values, so the
     * user immediately sees what they've set without an extra click.
     */
    protected readonly advancedOpen = signal(false);

    readonly form = new FormGroup({
        href: new FormControl<string>('', {
            nonNullable: true,
            validators: [Validators.required, linkHrefValidator]
        }),
        displayText: new FormControl<string>('', { nonNullable: true }),
        openInNewTab: new FormControl<boolean>(false, { nonNullable: true }),
        title: new FormControl<string>('', { nonNullable: true }),
        ariaLabel: new FormControl<string>('', { nonNullable: true }),
        rel: new FormControl<string | null>(null)
    });

    protected toggleAdvanced(): void {
        this.advancedOpen.update((v) => !v);
    }

    /**
     * Handles the autocomplete's `ngModelChange`. Mirrors the value into the reactive `href`
     * control synchronously (emitEvent:false keeps validation running without loops) so the
     * Insert button's validity and an Enter-to-insert always see the current value. The model
     * is a string while typing and, momentarily, the selected object on select.
     */
    protected onLinkModelChange(value: string | PageSearchResult): void {
        this.linkModel.set(value);
        const href = typeof value === 'string' ? value : (value?.url ?? '');
        this.form.controls.href.setValue(href, { emitEvent: false });
    }

    constructor() {
        // Run page searches off the debounced term stream. switchMap cancels the previous
        // request when a newer query arrives; catchError keeps the stream alive on failure.
        this.#searchTerm$
            .pipe(
                switchMap((term) =>
                    this.#contentSearch
                        .get<ContentletSearchEntity>({
                            query: this.#buildPageSearchQuery(term, this.#store.languageId()),
                            sort: 'modDate desc',
                            offset: 0,
                            limit: PAGE_SEARCH_LIMIT
                        })
                        .pipe(catchError(() => of<ContentletSearchEntity>({})))
                ),
                takeUntilDestroyed()
            )
            .subscribe((entity) => {
                const contentlets = entity?.jsonObjectView?.contentlets ?? [];
                this.suggestions.set(
                    contentlets.map((c) => ({
                        name: c.title,
                        url: c.path || c.urlMap,
                        hasTitleImage: c.hasTitleImage,
                        inode: c.inode
                    }))
                );
            });

        // Pre-populate the form when opened in edit mode.
        effect(() => {
            const payload = this.manager.linkPayload();
            untracked(() => {
                const values = payload?.initialValues;
                if (values) {
                    const title = values.title ?? '';
                    const ariaLabel = values.ariaLabel ?? '';
                    const rel = values.rel ?? null;
                    const href = values.href ?? '';
                    // emitEvent:false so prefilling an existing link doesn't fire a search.
                    this.form.setValue(
                        {
                            href,
                            displayText: values.displayText ?? '',
                            openInNewTab: values.target === '_blank',
                            title,
                            ariaLabel,
                            rel
                        },
                        { emitEvent: false }
                    );
                    this.linkModel.set(href);
                    // If any advanced field is populated, surface the section so the user
                    // can see what they previously set without hunting for the toggle.
                    this.advancedOpen.set(!!(title || ariaLabel || rel));
                }
            });
        });

        // Reset form when dialog closes.
        effect(() => {
            if (!this.manager.isOpen('link')) {
                untracked(() => {
                    this.form.reset({
                        href: '',
                        displayText: '',
                        openInNewTab: false,
                        title: '',
                        ariaLabel: '',
                        rel: null
                    });
                    this.linkModel.set('');
                    this.suggestions.set([]);
                    this.isExternalUrl.set(false);
                    this.advancedOpen.set(false);
                });
            }
        });

        // Manage the `link-editing` CSS class on the active link element.
        effect((onCleanup) => {
            if (!this.manager.isOpen('link')) return;
            const linkEl = this.manager.linkPayload()?.linkEl;
            if (!linkEl) return;
            linkEl.classList.add('link-editing');
            onCleanup(() => linkEl.classList.remove('link-editing'));
        });

        // Insert mode (no `linkEl`): once the URL input takes focus the browser stops
        // painting the editor's native selection, leaving the author with no hint of
        // which text will become the link. Paint the exact range with a ProseMirror
        // decoration that survives the blur; clear it when the popover closes.
        effect((onCleanup) => {
            if (!this.manager.isOpen('link')) return;
            if (this.manager.linkPayload()?.linkEl) return;
            const view = this.editor().view;
            view.dispatch(view.state.tr.setMeta(LINK_SELECTION_KEY, { active: true }));
            onCleanup(() =>
                view.dispatch(view.state.tr.setMeta(LINK_SELECTION_KEY, { active: false }))
            );
        });
    }

    /**
     * AutoComplete `completeMethod` handler. Debounced upstream by `[delay]`. Skips the
     * search and hides the overlay when the value is a full http(s) URL (external link);
     * otherwise pushes the term onto the search stream.
     */
    protected onComplete(event: { query: string }): void {
        const query = (event.query ?? '').trim();

        if (!query) {
            this.isExternalUrl.set(false);
            this.suggestions.set([]);
            return;
        }

        if (isValidHttpUrl(query)) {
            this.isExternalUrl.set(true);
            this.suggestions.set([]);
            // The overlay was opened by AutoComplete before completeMethod ran — force it
            // closed so an external URL never shows a (loading/empty) dropdown.
            const ac = this.autoComplete();
            if (ac) {
                ac.loading = false;
                ac.overlayVisible = false;
                ac.cd.markForCheck();
            }
            return;
        }

        this.isExternalUrl.set(false);
        this.#searchTerm$.next(query);
    }

    /**
     * Fills the URL field with the selected page's path. Normalises the autocomplete model
     * back to a plain string (it briefly holds the selected object) and clears suggestions.
     */
    protected onSelectPage(event: AutoCompleteSelectEvent): void {
        const url = (event.value as PageSearchResult)?.url ?? '';
        // Set synchronously: AutoComplete's Enter handler does not stop propagation, so the
        // container's Enter→onInsert may run in the same tick — href must already be current.
        this.onLinkModelChange(url);
        this.suggestions.set([]);
    }

    /**
     * Lucene query for internal-page search — pages (`basetype:5`) plus URL-mapped content,
     * matched by title / path / urlmap prefix. Built inline at the call site (matching the
     * slash-menu's `buildContentletByTypeQuery`); mirrors the legacy link popover.
     */
    #buildPageSearchQuery(term: string, languageId: number): string {
        return `+languageId:${languageId || 1} +deleted:false +working:true +(urlmap:* OR basetype:5) +(title:${term}* OR path:*${term}* OR urlmap:*${term}*)`;
    }

    onInsert(): void {
        if (this.form.controls.href.invalid) return;
        const { href, displayText, openInNewTab, title, ariaLabel, rel } = this.form.getRawValue();
        const payload = this.manager.linkPayload();
        const editor = this.editor();

        // Empty strings → null so renderHTML on the link mark omits the attribute and the
        // global `Link.HTMLAttributes.rel` default applies for `rel`.
        const linkAttrs = {
            href,
            target: openInNewTab ? '_blank' : null,
            title: title.trim() || null,
            'aria-label': ariaLabel.trim() || null,
            rel: (rel ?? '').trim() || null
        };

        if (payload?.linkEl) {
            // Edit mode — update the link in place using the pre-computed anchor position.
            const linkEl = payload.linkEl;
            const anchorPos =
                payload.anchorPos ??
                (() => {
                    try {
                        return editor.view.posAtDOM(linkEl, 0);
                    } catch {
                        return editor.state.selection.from;
                    }
                })();
            editor
                .chain()
                .focus()
                .setTextSelection(anchorPos)
                .extendMarkRange('link')
                .insertContent({
                    type: 'text',
                    text: displayText.trim() || href,
                    marks: [{ type: 'link', attrs: linkAttrs }]
                })
                .run();
        } else {
            editor
                .chain()
                .focus()
                .insertContent({
                    type: 'text',
                    text: displayText.trim() || href,
                    marks: [{ type: 'link', attrs: linkAttrs }]
                })
                .run();
        }

        this.manager.close();
    }
}
