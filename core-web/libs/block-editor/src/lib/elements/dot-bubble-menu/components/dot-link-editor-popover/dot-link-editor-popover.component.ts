/* eslint-disable @typescript-eslint/no-explicit-any */
import { Observable, Subject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Component, inject, input, signal, OnDestroy, ViewChild, ElementRef } from '@angular/core';
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

interface Item {
    name: string;
    value: string;
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
    @ViewChild('linkModal', { read: EditorModalDirective }) editorModal: EditorModalDirective;
    @ViewChild('input', { read: ElementRef }) input?: ElementRef<HTMLInputElement>;

    protected readonly editor = input.required<Editor>();
    protected readonly searchTerm = signal<string>('');
    protected readonly loading = signal<boolean>(false);
    protected readonly items = signal<Item[]>([]);
    protected readonly selectedItem = signal<Item | null>(null);

    private readonly httpClient = inject(HttpClient);
    private readonly destroy$ = new Subject<void>();
    private readonly searchSubject = new Subject<string>();

    @ViewChild('resultListbox') resultListbox?: Listbox;

    readonly tippyOptions = {
        onShow: this.setInitialLink.bind(this),
        onShown: this.focusInput.bind(this),
        onHide: this.unsetHighlight.bind(this)
    };

    constructor() {
        this.searchSubject
            .pipe(debounceTime(1000), distinctUntilChanged(), takeUntil(this.destroy$))
            .subscribe((query) => {
                this.performSearch(query);
                this.searchTerm.set(query);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    toggle() {
        this.editorModal?.toggle();
    }

    protected onKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            this.resultListbox?.onListKeyDown(event);
        }
    }

    protected onSearchInput(value: string) {
        this.loading.set(true);
        this.searchSubject.next(value);
    }

    protected addLink(value: string) {
        this.editor().chain().focus().setLink({ href: value, target: '_blank' }).run();
        this.editorModal.hide();
    }

    private performSearch(query: string) {
        this.getContentletsByLink(query).subscribe({
            next: (contentlets) => {
                this.items.set(
                    contentlets.map((contentlet) => ({
                        hasTitleImage: contentlet.hasTitleImage,
                        inode: contentlet.inode,
                        name: contentlet.title,
                        value: contentlet.path || contentlet.urlMap
                    }))
                );
                this.loading.set(false);
            },
            error: () => {
                this.items.set([]);
                this.loading.set(false);
            }
        });
    }

    private getContentletsByLink(query: string): Observable<DotCMSContentlet[]> {
        return this.httpClient
            .post('/api/content/_search', {
                query: `+languageId:1 +deleted:false +working:true  +(urlmap:* OR basetype:5)  +deleted:false +(title:${query}* OR path:*${query}* OR urlmap:*${query}*)`,
                sort: 'modDate desc',
                offset: 0,
                limit: 5
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }

    private setInitialLink() {
        const isTextLink = this.editor().isActive('link');
        const node = this.editor().getAttributes(isTextLink ? 'link' : 'dotImage');
        const { href: link = '' } = node;
        this.searchTerm.set(link);
        this.items.set([]);
    }

    private focusInput() {
        this.editor().commands.setHighlight();
        this.input?.nativeElement.focus();
    }

    private unsetHighlight() {
        this.editor().chain().unsetHighlight().focus().run();
    }
}
