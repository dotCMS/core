import { Observable, Subject } from 'rxjs';
import { Instance, Props } from 'tippy.js';

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

import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { InputText } from 'primeng/inputtext';
import { Listbox } from 'primeng/listbox';
import { Skeleton } from 'primeng/skeleton';

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
    imports: [FormsModule, Listbox, InputText, Skeleton, Button, Checkbox, EditorModalDirective]
})
export class DotLinkEditorPopoverComponent implements OnDestroy {
    @ViewChild('popover', { read: EditorModalDirective }) private popover: EditorModalDirective;
    @ViewChild('input', { read: ElementRef }) private searchInput?: ElementRef<HTMLInputElement>;
    @ViewChild('resultListbox') private searchResultsListbox?: Listbox;

    readonly editor = input.required<Editor>();
    private readonly httpClient = inject(HttpClient);

    protected readonly searchQuery = signal<string>('');
    protected readonly searchResults = signal<SearchResultItem[]>([]);
    protected readonly isSearching = signal<boolean>(false);
    protected readonly existingLinkUrl = signal<string | null>(null);
    protected readonly linkTargetAttribute = signal<string>('_blank');

    protected readonly showLoading = computed(
        () => this.isSearching() && !this.showLinkDetails() && !this.isFullURL()
    );
    protected readonly showLinkDetails = computed(
        () => !!this.existingLinkUrl() && this.searchQuery() === this.existingLinkUrl()
    );
    protected readonly showSearchResults = computed(
        () =>
            this.searchQuery() &&
            !this.isSearching() &&
            !this.showLinkDetails() &&
            !this.isFullURL()
    );

    private readonly componentDestroy$ = new Subject<void>();
    private readonly searchInputSubject = new Subject<string>();

    protected readonly isFullURL = computed(() => {
        try {
            return new URL(this.searchQuery());
        } catch {
            return false;
        }
    });

    readonly tippyModalOptions: Partial<Props> = {
        onShow: this.initializeExistingLinkData.bind(this),
        onShown: this.focusSearchInput.bind(this),
        onHide: this.clearEditorHighlight.bind(this),
        placement: 'bottom',
        onClickOutside: this.onClickOutside.bind(this)
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

    /**
     * The native element of the Tippy instance.
     */
    get tippyElement() {
        return this.popover?.nativeElement;
    }

    constructor() {
        this.searchInputSubject
            .pipe(debounceTime(250), distinctUntilChanged(), takeUntil(this.componentDestroy$))
            .subscribe((searchTerm) => this.executeContentSearch(searchTerm));
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
     * Toggles the visibility of the image editor popover.
     */
    toggle() {
        this.popover?.toggle();
    }

    /**
     * Shows the image editor popover.
     */
    show() {
        this.popover?.show();
    }

    /**
     * Hides the image editor popover.
     */
    hide() {
        this.popover?.hide();
    }

    /**
     * Handles keyboard navigation within the search input field.
     * Allows users to navigate search results using arrow keys for better accessibility.
     */
    protected handleSearchInputKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            this.searchResultsListbox?.onListKeyDown(event);
        }
    }

    /**
     * Processes changes to the search input and triggers content search.
     * Skips search execution for external URLs to avoid unnecessary API calls.
     */
    protected onSearchQueryChange(searchValue: string) {
        if (this.isFullURL()) {
            return;
        }

        this.isSearching.set(true);
        this.searchInputSubject.next(searchValue);
    }

    /**
     * Applies the selected link URL to the currently selected text or image in the editor.
     * Creates a new link with the specified URL and target attribute, then closes the popover.
     */
    protected addLinkToNode(linkUrl: string) {
        const target = this.linkTargetAttribute();
        const isImageNode = this.editor().isActive('dotImage');

        const searchResultIndex = this.searchResultsListbox?.focusedOptionIndex() ?? -1;
        const searchResult = this.searchResults()[searchResultIndex];

        const linkToSave = searchResult?.url || linkUrl;

        if (isImageNode) {
            this.editor().chain().focus().setImageLink({ href: linkToSave, target }).run();
        } else {
            this.editor().chain().focus().setLink({ href: linkToSave, target }).run();
        }

        this.popover.hide();
    }

    /**
     * Selects a link from the search results and adds it to the editor.
     * @param item - The selected link item.
     */
    protected selectLink(item: SearchResultItem) {
        this.addLinkToNode(item.url);
    }

    /**
     * Initializes the popover with data from an existing link when editing.
     * Extracts link attributes from the current selection and populates the form fields.
     */
    private initializeExistingLinkData() {
        const isTextLink = this.editor().isActive('link');
        const linkAttrs = this.editor().getAttributes(isTextLink ? 'link' : 'dotImage');
        const { href: existingUrl = '', target: existingTarget = '_blank' } = linkAttrs;

        this.searchQuery.set(existingUrl);
        this.existingLinkUrl.set(existingUrl);
        this.linkTargetAttribute.set(existingTarget);
        this.searchResults.set([]);
    }

    /** Copies the current link URL to the system clipboard. */
    protected copyExistingLinkToClipboard() {
        navigator.clipboard.writeText(this.existingLinkUrl() || '');
        this.popover.hide();
    }

    /**
     * Removes the link from the currently selected text or image in the editor.
     * The text content remains but the link formatting is removed.
     */
    protected removeLinkFromEditor() {
        const isImageNode = this.editor().isActive('dotImage');

        if (isImageNode) {
            this.editor().chain().focus().unsetImageLink().run();
        } else {
            this.editor().chain().focus().unsetLink().run();
        }

        this.popover.hide();
    }

    /**
     * Updates the target attribute of an existing link based on user preference.
     * Allows users to control whether links open in the same window or a new tab.
     */
    protected updateLinkTargetAttribute(event: { checked: boolean }) {
        const shouldOpenInNewWindow = event.checked;
        const newTargetValue = shouldOpenInNewWindow ? '_blank' : '_self';

        this.editor()
            .chain()
            .setLink({ href: this.existingLinkUrl(), target: newTargetValue })
            .run();

        this.linkTargetAttribute.set(newTargetValue);
        this.popover.hide();
    }

    /**
     * Executes a search for DotCMS content based on the provided search term.
     * Transforms the results into a format suitable for display.
     */
    private executeContentSearch(searchTerm: string) {
        this.searchContentletsByQuery(searchTerm).subscribe({
            next: (results) => {
                this.searchResults.set(
                    (results || []).map((c) => ({
                        hasTitleImage: c.hasTitleImage,
                        inode: c.inode,
                        displayName: c.title,
                        url: c.path || c.urlMap
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
     * Sets focus to the search input field and highlights the editor selection.
     */
    private focusSearchInput() {
        this.editor().commands.setHighlight();
        this.searchInput?.nativeElement.focus();
    }

    /**
     * Removes the highlight from the editor selection and restores focus.
     */
    private clearEditorHighlight() {
        this.editor().chain().unsetHighlight().focus().run();
    }

    /**
     * Sends a POST request to fetch contentlets matching the search term.
     */
    private searchContentletsByQuery(searchTerm: string): Observable<DotCMSContentlet[]> {
        const languageId = this.editor().storage.dotConfig.lang;

        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:${languageId || 1} +deleted:false +working:true +(urlmap:* OR basetype:5) +deleted:false +(title:${searchTerm}* OR path:*${searchTerm}* OR urlmap:*${searchTerm}*)`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }

    /**
     * Handles clicks outside the link editor popover.
     * If the click is on a link option, it does not hide the popover.
     * @param instance - The Tippy instance.
     * @param event - The mouse event.
     */
    private onClickOutside(instance: Instance, event: MouseEvent) {
        const target = event.target as HTMLElement;
        const clickedOnBubbleMenu = target?.closest('[tiptapbubblemenu]');
        if (clickedOnBubbleMenu) {
            return;
        }

        instance.hide();
    }
}
