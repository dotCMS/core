import { Observable, Subject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import {
    Component,
    inject,
    input,
    signal,
    OnDestroy,
    ViewChild,
    ElementRef,
    computed,
    HostListener
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { SkeletonModule } from 'primeng/skeleton';

import { debounceTime, distinctUntilChanged, takeUntil, pluck } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { EditorModalDirective } from '../../../../directive/editor-modal.directive';

interface SearchResultItem {
    displayName: string;
    url: string;
    hasTitleImage?: boolean;
    inode?: string;
}

/**
 * A popover component for creating and editing links in the DotCMS block editor.
 * This component provides functionality to:
 * - Search for internal content to link to
 * - Create links to external URLs
 * - Edit existing link properties (URL, target attribute)
 * - Remove links from selected text or images
 */
@Component({
    selector: 'dot-link-editor-popover',
    templateUrl: './dot-link-editor-popover.component.html',
    styleUrls: ['./dot-link-editor-popover.component.scss'],
    standalone: true,
    imports: [
        FormsModule,
        ListboxModule,
        AutoCompleteModule,
        InputTextModule,
        SkeletonModule,
        ButtonModule,
        EditorModalDirective
    ]
})
export class DotLinkEditorPopoverComponent implements OnDestroy {
    @ViewChild('popover', { read: EditorModalDirective }) popover: EditorModalDirective;
    @ViewChild('input', { read: ElementRef }) searchInput?: ElementRef<HTMLInputElement>;
    @ViewChild('resultListbox') searchResultsListbox?: Listbox;

    protected readonly editor = input.required<Editor>();
    protected readonly searchQuery = signal<string>('');
    protected readonly isSearching = signal<boolean>(false);
    protected readonly searchResults = signal<SearchResultItem[]>([]);

    protected readonly isImageNode = computed(() => {
        return this.editor().isActive('dotImage');
    });

    // Current link state for editing existing links
    protected readonly existingLinkUrl = signal<string | null>(null);
    protected readonly linkTargetAttribute = signal<string>('_blank');

    protected readonly isExternalURL = computed(() => {
        const validURLRegex = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/;

        return validURLRegex.test(this.searchQuery());
    });

    protected readonly showLinkDetails = computed(
        () => this.existingLinkUrl() && this.searchQuery() === this.existingLinkUrl()
    );

    protected readonly showSearchResults = computed(() => {
        const isDifferentLink = this.searchQuery() !== this.existingLinkUrl();
        const hasResults = this.searchResults().length > 0;
        const isSearching = this.isSearching();
        const isExternalLink = this.isExternalURL();

        return (hasResults || isDifferentLink) && !isSearching && !isExternalLink;
    });

    private readonly httpClient = inject(HttpClient);
    private readonly componentDestroy$ = new Subject<void>();
    private readonly searchQuerySubject = new Subject<string>();

    readonly tippyModalOptions = {
        onShow: this.initializeExistingLinkData.bind(this),
        onShown: this.focusSearchInput.bind(this),
        onHide: this.clearEditorHighlight.bind(this)
    };

    /**
     * Handles the Escape key press to close the popover.
     * This provides a consistent way for users to cancel link editing.
     */
    @HostListener('document:keydown.escape', ['$event'])
    protected onEscapeKey(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            this.popover.hide();
        }
    }

    constructor() {
        this.searchQuerySubject
            .pipe(debounceTime(250), distinctUntilChanged(), takeUntil(this.componentDestroy$))
            .subscribe((searchTerm) => {
                this.executeContentSearch(searchTerm);
                this.searchQuery.set(searchTerm);
            });
    }

    /**
     * Cleanup method called when the component is destroyed.
     * Ensures all subscriptions are properly closed to prevent memory leaks.
     */
    ngOnDestroy(): void {
        this.componentDestroy$.next();
        this.componentDestroy$.complete();
    }

    /**
     * Toggles the visibility of the link editor popover.
     * Can be called from parent components to show/hide the link editor.
     */
    toggle() {
        this.popover?.toggle();
    }

    /**
     * Handles keyboard navigation within the search input field.
     * Allows users to navigate search results using arrow keys for better accessibility.
     *
     * @param event - The keyboard event containing the pressed key
     */
    protected handleSearchInputKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            this.searchResultsListbox?.onListKeyDown(event);
        }
    }

    /**
     * Processes changes to the search input and triggers content search.
     * Skips search execution for external URLs to avoid unnecessary API calls.
     *
     * @param searchValue - The current value from the search input field
     */
    protected onSearchQueryChange(searchValue: string) {
        if (this.isExternalURL()) {
            return;
        }

        this.isSearching.set(true);
        this.searchQuerySubject.next(searchValue);
    }

    /**
     * Applies the selected link URL to the currently selected text or image in the editor.
     * Creates a new link with the specified URL and target attribute, then closes the popover.
     *
     * @param linkUrl - The URL to be applied as the link href attribute
     */
    protected addLinkToNode(linkUrl: string) {
        if (this.isImageNode()) {
            this.editor()
                .chain()
                .focus()
                .setImageLink({ href: linkUrl, target: this.linkTargetAttribute() })
                .run();
        } else {
            this.editor()
                .chain()
                .focus()
                .setLink({ href: linkUrl, target: this.linkTargetAttribute() })
                .run();
        }

        this.popover.hide();
    }

    /**
     * Initializes the popover with data from an existing link when editing.
     * Extracts link attributes from the current selection and populates the form fields.
     * Handles both text links and image links by checking the appropriate node type.
     */
    private initializeExistingLinkData() {
        const isActiveTextLink = this.editor().isActive('link');
        const linkAttributes = this.editor().getAttributes(isActiveTextLink ? 'link' : 'dotImage');
        const { href: existingUrl = '', target: existingTarget = '_blank' } = linkAttributes;
        this.searchQuery.set(existingUrl);
        this.existingLinkUrl.set(existingUrl);
        this.linkTargetAttribute.set(existingTarget);
        this.searchResults.set([]);
    }

    /**
     * Copies the current link URL to the system clipboard.
     * Provides a quick way for users to share or reuse existing link URLs.
     */
    protected copyExistingLinkToClipboard() {
        navigator.clipboard.writeText(this.existingLinkUrl() || '');
        this.popover.hide();
    }

    /**
     * Removes the link from the currently selected text or image in the editor.
     * The text content remains but the link formatting is removed, then closes the popover.
     */
    protected removeLinkFromEditor() {
        this.editor().chain().unsetLink().run();
        this.popover.hide();
    }

    /**
     * Updates the target attribute of an existing link based on user preference.
     * Allows users to control whether links open in the same window or a new tab.
     *
     * @param event - The change event from the checkbox input
     */
    protected updateLinkTargetAttribute(event: Event) {
        const shouldOpenInNewWindow = (event.target as HTMLInputElement).checked;
        const newTargetValue = shouldOpenInNewWindow ? '_blank' : '_self';
        this.editor()
            .chain()
            .setLink({ href: this.existingLinkUrl(), target: newTargetValue })
            .run();
    }

    /**
     * Executes a search for DotCMS content based on the provided search term.
     * Transforms the search results into a format suitable for display in the UI.
     * Handles both successful responses and error cases gracefully.
     *
     * @param searchTerm - The search query entered by the user
     */
    private executeContentSearch(searchTerm: string) {
        this.searchForContentletsByQuery(searchTerm).subscribe({
            next: (foundContentlets) => {
                this.searchResults.set(
                    foundContentlets.map((contentlet) => ({
                        hasTitleImage: contentlet.hasTitleImage,
                        inode: contentlet.inode,
                        displayName: contentlet.title,
                        url: contentlet.path || contentlet.urlMap
                    }))
                );
                this.isSearching.set(false);
            },
            error: () => {
                this.searchResults.set([]);
                this.isSearching.set(false);
            }
        });
    }

    /**
     * Performs an HTTP request to search for DotCMS contentlets matching the search term.
     * Constructs a Lucene query to find published content with matching titles, paths, or URL maps.
     * Results are limited to 5 items and sorted by modification date.
     *
     * @param searchTerm - The search query to match against content
     * @returns Observable of DotCMS contentlets matching the search criteria
     */
    private searchForContentletsByQuery(searchTerm: string): Observable<DotCMSContentlet[]> {
        const languageId = this.editor().storage.dotConfig.lang;

        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:${languageId} +deleted:false +working:true  +(urlmap:* OR basetype:5)  +deleted:false +(title:${searchTerm}* OR path:*${searchTerm}* OR urlmap:*${searchTerm}*)`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }

    /**
     * Sets focus to the search input field and highlights the current selection in the editor.
     * Called when the popover is shown to provide immediate user interaction feedback.
     */
    private focusSearchInput() {
        this.editor().commands.setHighlight();
        this.searchInput?.nativeElement.focus();
    }

    /**
     * Removes the highlight from the editor selection and restores focus to the editor.
     * Called when the popover is hidden to clean up the editor state.
     */
    private clearEditorHighlight() {
        this.editor().chain().unsetHighlight().focus().run();
    }
}
