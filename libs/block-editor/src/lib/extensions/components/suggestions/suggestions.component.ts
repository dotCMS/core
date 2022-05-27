import {
    AfterViewInit,
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

import { DotLanguageService, Languages } from '../../services/dot-language/dot-language.service';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';
import { SuggestionsService } from '../../services/suggestions/suggestions.service';
import { DEFAULT_LANG_ID, suggestionOptions } from '@dotcms/block-editor';

export interface SuggestionsCommandProps {
    payload?: DotCMSContentlet;
    type: { name: string; level?: number };
}

export interface DotMenuItem extends Omit<MenuItem, 'icon'> {
    icon: string | SafeUrl;
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
    selector: 'dotcms-suggestions',
    templateUrl: './suggestions.component.html',
    styleUrls: ['./suggestions.component.scss']
})
export class SuggestionsComponent implements OnInit, AfterViewInit {
    @ViewChild('list', { static: false }) list: SuggestionListComponent;

    @Input() onSelection: (props: SuggestionsCommandProps) => void;
    @Input() items: DotMenuItem[] = [];
    @Input() title = 'Select a block';
    @Input() noResultsMessage = 'No Results';
    @Input() isOpen = false;
    @Input() currentLanguage = DEFAULT_LANG_ID;
    @Input() allowedContentTypes = '';

    @Output() clearFilter: EventEmitter<string> = new EventEmitter<string>();

    private itemsLoaded: ItemsType;
    private selectedContentType: DotCMSContentType;
    private mouseMove = true;
    private dotLangs: Languages;
    private initialItems: DotMenuItem[];

    isFilterActive = false;

    @HostListener('mousemove', ['$event'])
    onMousemove() {
        this.mouseMove = true;
    }

    constructor(
        private suggestionsService: SuggestionsService,
        private dotLanguageService: DotLanguageService,
        private cd: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        if (this.items?.length === 0) {
            // assign the default suggestions options.
            this.items = suggestionOptions;
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
        }
        this.initialItems = this.items;
        this.itemsLoaded = ItemsType.BLOCK;
        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    ngAfterViewInit() {
        this.setFirstItemActive();
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
            this.handleBackButton(new MouseEvent('click'));
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
        this.mouseMove = false;
    }

    /**
     * Set the first item active
     *
     * @memberof SuggestionsComponent
     */
    setFirstItemActive() {
        this.list?.setFirstItemActive();
    }

    /**
     * Update the active Index
     * @param {number} index
     * @memberof SuggestionsComponent
     */
    updateActiveItem(index: number): void {
        this.list.updateActiveItem(index);
    }

    /**
     * Reset the key manager after we add new elements to the list
     *
     * @memberof SuggestionsComponent
     */
    resetKeyManager() {
        this.list?.resetKeyManager();
    }

    /**
     * Avoid closing the suggestions on manual scroll
     *
     * @param {MouseEvent} e
     * @memberof SuggestionsComponent
     */
    onMouseDownHandler(e: MouseEvent) {
        e.preventDefault();
    }

    /**
     * Handle the active item on menu events
     *
     * @param {MouseEvent} e
     * @memberof SuggestionsComponent
     */
    onMouseEnter(e: MouseEvent) {
        // If mouse does not move then leave the function.
        if (!this.mouseMove) {
            return;
        }
        e.preventDefault();
        const index = Number((e.target as HTMLElement).dataset.index);
        this.updateActiveItem(index);
    }

    /**
     * Execute the item command on mouse down
     *
     * @param {MouseEvent} e
     * @param {MenuItem} item
     * @memberof SuggestionsComponent
     */
    onMouseDown(e: MouseEvent, item: MenuItem) {
        e.preventDefault();
        item.command();
    }

    /**
     * Go back to contentlet selection
     *
     * @param {MouseEvent} event
     * @memberof SuggestionsComponent
     */
    handleBackButton(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        // Set the previous load Time to make the right search.
        this.itemsLoaded =
            this.itemsLoaded === ItemsType.CONTENT ? ItemsType.CONTENTTYPE : ItemsType.BLOCK;
        this.filterItems();
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
        this.setFirstItemActive();
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
                if (this.items.length) {
                    this.title = 'Select a content type';
                    this.cd.detectChanges();
                    this.resetKeyManager();
                } else {
                    this.title = `No results`;
                    this.cd.detectChanges();
                }

                this.cd.detectChanges();
                this.resetKeyManager();
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
                if (this.items.length) {
                    this.title = 'Select a contentlet';
                    this.cd.detectChanges();
                    this.resetKeyManager();
                } else {
                    this.title = `No results for <b>${contentType.name}</b>`;
                    this.cd.detectChanges();
                }
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
