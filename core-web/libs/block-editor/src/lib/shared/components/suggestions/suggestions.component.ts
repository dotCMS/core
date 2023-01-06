import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { SafeUrl } from '@angular/platform-browser';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { map, take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';

import { DotLanguageService, SuggestionsService, Languages } from '@dotcms/block-editor/services';
import { DEFAULT_LANG_ID, suggestionOptions, SuggestionListComponent } from '@dotcms/block-editor';

export interface SuggestionsCommandProps {
    payload?: DotCMSContentlet;
    type: { name: string; level?: number };
}

export interface DotMenuItem extends Omit<MenuItem, 'icon'> {
    icon?: string | SafeUrl;
    isActive?: () => boolean;
    attributes?: Record<string, unknown>;
    data?: Record<string, unknown>;
}

export enum ItemsType {
    BLOCK = 'block',
    CONTENTTYPE = 'contentType',
    CONTENT = 'content'
}

@Component({
    selector: 'dot-suggestions',
    templateUrl: './suggestions.component.html',
    styleUrls: ['./suggestions.component.scss']
})
export class SuggestionsComponent implements OnInit {
    @ViewChild('list', { static: false }) list: SuggestionListComponent;

    @Input() onSelection: (props: SuggestionsCommandProps) => void;
    @Input() items: DotMenuItem[] = [];
    @Input() title = 'Select a block';
    @Input() noResultsMessage = 'No Results';
    @Input() currentLanguage = DEFAULT_LANG_ID;
    @Input() allowedContentTypes = '';
    @Input() allowedBlocks = [];

    @Output() clearFilter: EventEmitter<string> = new EventEmitter<string>();

    private itemsLoaded: ItemsType;
    private selectedContentType: DotCMSContentType;
    private dotLangs: Languages;
    private initialItems: DotMenuItem[];

    isFilterActive = false;

    /**
     * Avoid loosing the `focus` target.
     *
     * @param {MouseEvent} e
     * @memberof SuggestionListComponent
     */
    @HostListener('mousedown', ['$event'])
    onMouseDownHandler(e: MouseEvent) {
        e.preventDefault();
    }

    constructor(
        private suggestionsService: SuggestionsService,
        private dotLanguageService: DotLanguageService,
        private cd: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        if (this.items?.length === 0) {
            // assign the default suggestions options.
            this.items = this.allowedBlocks.length
                ? suggestionOptions.filter((item) => this.allowedBlocks.includes(item.id))
                : suggestionOptions;
            // Extra this to an function
            this.items.forEach((item) => {
                item.command = () => {
                    this.clearFilter.emit(ItemsType.BLOCK);
                    item.id.includes('heading')
                        ? this.onSelection({
                              type: { name: 'heading', ...item.attributes }
                          })
                        : this.onSelection({ type: { name: item.id } });
                };
            });
        }

        this.initialItems = this.items;
        this.itemsLoaded = ItemsType.BLOCK;
        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    /**
     * Add the Contentlets item to the suggestions that is not present by default.
     *
     * @memberof SuggestionsComponent
     */
    addContentletItem() {
        this.items = [
            {
                label: 'Contentlets',
                icon: 'receipt',
                command: () => {
                    this.clearFilter.emit(ItemsType.CONTENTTYPE);
                    this.loadContentTypes();
                }
            },
            ...this.items
        ];
        this.initialItems = this.items;
    }

    /**
     * Execute the item command
     *
     * @memberof SuggestionsComponent
     */
    execCommand() {
        if (this.items.length) {
            this.list.execCommand();
        } else {
            this.handleBackButton();
        }
    }

    /**
     * Update the current item selected
     *
     * @param {KeyboardEvent} e
     * @memberof SuggestionsComponent
     */
    updateSelection(e: KeyboardEvent) {
        this.list.updateSelection(e);
    }

    /**
     * Go back to contentlet selection
     *
     * @memberof SuggestionsComponent
     */
    handleBackButton(): boolean {
        // Set the previous load Time to make the right search.
        this.itemsLoaded =
            this.itemsLoaded === ItemsType.CONTENT ? ItemsType.CONTENTTYPE : ItemsType.BLOCK;
        this.filterItems();

        return false;
    }

    /**
     * Set items visible based on filter
     *
     * @param {string} filter
     * @memberof SuggestionsComponent
     */
    filterItems(filter: string = '') {
        switch (this.itemsLoaded) {
            case ItemsType.BLOCK:
                this.items = this.initialItems.filter((item) =>
                    item.label.toLowerCase().includes(filter.trim().toLowerCase())
                );
                break;

            case ItemsType.CONTENTTYPE:
                this.loadContentTypes(filter);
                break;

            case ItemsType.CONTENT:
                this.loadContentlets(this.selectedContentType, filter);
        }

        this.isFilterActive = !!filter.length;
    }

    private loadContentTypes(filter = '') {
        this.suggestionsService
            .getContentTypes(filter, this.allowedContentTypes)
            .pipe(
                map((items) => {
                    return items.map((item) => {
                        return {
                            label: item.name,
                            icon: item.icon,
                            command: () => {
                                this.selectedContentType = item;
                                this.itemsLoaded = ItemsType.CONTENT;
                                this.loadContentlets(item);
                                this.clearFilter.emit(ItemsType.CONTENT);
                            }
                        };
                    });
                }),
                take(1)
            )
            .subscribe((items) => {
                this.items = items;
                this.itemsLoaded = ItemsType.CONTENTTYPE;

                this.items.length
                    ? (this.title = 'Select a content type')
                    : (this.noResultsMessage = `No results`);

                this.cd.detectChanges();
            });
    }

    private loadContentlets(contentType: DotCMSContentType, filter = '') {
        this.suggestionsService
            .getContentlets({
                contentType: contentType.variable,
                filter,
                currentLanguage: this.currentLanguage
            })
            .pipe(take(1))
            .subscribe((contentlets) => {
                this.items = contentlets.map((contentlet) => {
                    const { languageId } = contentlet;
                    contentlet.language = this.getContentletLanguage(languageId);

                    return {
                        label: contentlet.title,
                        icon: 'contentlet/image',
                        data: {
                            contentlet: contentlet
                        },
                        command: () => {
                            this.onSelection({
                                payload: contentlet,
                                type: {
                                    name: 'dotContent'
                                }
                            });
                        }
                    };
                });

                this.items.length
                    ? (this.title = 'Select a contentlet')
                    : (this.noResultsMessage = `No results for <b>${contentType.name}</b>`);

                this.cd.detectChanges();
            });
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
