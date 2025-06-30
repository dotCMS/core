/* eslint-disable @typescript-eslint/no-explicit-any */
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
    computed
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
    @ViewChild('linkEditorModal', { read: EditorModalDirective })
    linkEditorModal: EditorModalDirective;
    @ViewChild('input', { read: ElementRef }) searchInput?: ElementRef<HTMLInputElement>;

    protected readonly editor = input.required<Editor>();
    protected readonly searchQuery = signal<string>('');
    protected readonly isSearching = signal<boolean>(false);
    protected readonly searchResults = signal<SearchResultItem[]>([]);

    // Current link state for editing existing links
    protected readonly existingLinkUrl = signal<string | null>(null);
    protected readonly linkTargetAttribute = signal<string>('_blank');

    protected readonly showLinkDetails = computed(
        () => this.existingLinkUrl() && this.searchQuery() === this.existingLinkUrl()
    );
    protected readonly showSearchResults = computed(
        () =>
            this.searchResults() &&
            this.searchQuery() !== this.existingLinkUrl() &&
            !this.isSearching()
    );

    private readonly httpClient = inject(HttpClient);
    private readonly componentDestroy$ = new Subject<void>();
    private readonly searchQuerySubject = new Subject<string>();

    @ViewChild('resultListbox') searchResultsListbox?: Listbox;

    readonly tippyModalOptions = {
        onShow: this.initializeExistingLinkData.bind(this),
        onShown: this.focusSearchInput.bind(this),
        onHide: this.clearEditorHighlight.bind(this)
    };

    constructor() {
        this.searchQuerySubject
            .pipe(debounceTime(250), distinctUntilChanged(), takeUntil(this.componentDestroy$))
            .subscribe((searchTerm) => {
                this.executeContentSearch(searchTerm);
                this.searchQuery.set(searchTerm);
            });
    }

    ngOnDestroy(): void {
        this.componentDestroy$.next();
        this.componentDestroy$.complete();
    }

    toggle() {
        this.linkEditorModal?.toggle();
    }

    protected handleSearchInputKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            this.searchResultsListbox?.onListKeyDown(event);
        }
    }

    protected onSearchQueryChange(searchValue: string) {
        this.isSearching.set(true);
        this.searchQuerySubject.next(searchValue);
    }

    protected applyLinkToEditor(linkUrl: string) {
        this.editor()
            .chain()
            .focus()
            .setLink({ href: linkUrl, target: this.linkTargetAttribute() })
            .run();
        this.linkEditorModal.hide();
    }

    private initializeExistingLinkData() {
        const isActiveTextLink = this.editor().isActive('link');
        const linkAttributes = this.editor().getAttributes(isActiveTextLink ? 'link' : 'dotImage');
        const { href: existingUrl = '', target: existingTarget = '_blank' } = linkAttributes;
        this.searchQuery.set(existingUrl);
        this.existingLinkUrl.set(existingUrl);
        this.linkTargetAttribute.set(existingTarget);
        this.searchResults.set([]);
    }

    /* LINK MANAGEMENT ACTIONS */
    protected copyExistingLinkToClipboard() {
        navigator.clipboard.writeText(this.existingLinkUrl() || '');
    }

    protected removeLinkFromEditor() {
        this.editor().chain().unsetLink().run();
        this.linkEditorModal.hide();
    }

    protected updateLinkTargetAttribute(event: Event) {
        const shouldOpenInNewWindow = (event.target as HTMLInputElement).checked;
        const newTargetValue = shouldOpenInNewWindow ? '_blank' : '_self';
        this.editor()
            .chain()
            .setLink({ href: this.existingLinkUrl(), target: newTargetValue })
            .run();
    }

    /* CONTENT SEARCH FUNCTIONALITY */
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

    private searchForContentletsByQuery(searchTerm: string): Observable<DotCMSContentlet[]> {
        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:1 +deleted:false +working:true  +(urlmap:* OR basetype:5)  +deleted:false +(title:${searchTerm}* OR path:*${searchTerm}* OR urlmap:*${searchTerm}*)`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }

    /* EDITOR INTERACTION HELPERS */
    private focusSearchInput() {
        this.editor().commands.setHighlight();
        this.searchInput?.nativeElement.focus();
    }

    private clearEditorHighlight() {
        this.editor().chain().unsetHighlight().focus().run();
    }
}
