import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostListener,
    Input,
    OnInit,
    ViewChild
} from '@angular/core';
import { SafeUrl } from '@angular/platform-browser';

import { MenuItem } from 'primeng/api';

import { map, take } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType, DotLanguage } from '@dotcms/dotcms-models';

import { DEFAULT_LANG_ID } from '../../../extensions';
import { SuggestionsService } from '../../services';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';

export interface SuggestionsCommandProps {
    payload?: DotCMSContentlet;
    type: { name: string; level?: number };
}

export interface DotMenuItem extends Omit<MenuItem, 'icon'> {
    icon?: string | SafeUrl;
    isActive?: () => boolean;
    attributes?: Record<string, unknown>;
    data?: Record<string, unknown>;
    commandKey?: string;
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
    @ViewChild('list', { static: false, read: ElementRef }) listElement: ElementRef;

    @Input() onSelectContentlet: (props: SuggestionsCommandProps) => void;
    @Input() items: DotMenuItem[] = [];
    @Input() title = 'Select a block';
    @Input() noResultsMessage = 'No Results';
    @Input() currentLanguage = DEFAULT_LANG_ID;
    @Input() allowedContentTypes = '';
    @Input() contentletIdentifier = '';

    private itemsLoaded: ItemsType;
    private selectedContentType: DotCMSContentType;
    private dotLangs: { [key: string]: DotLanguage } = {};
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
        private dotLanguagesService: DotLanguagesService,
        private cd: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.initialItems = this.items;
        this.itemsLoaded = ItemsType.BLOCK;
        this.dotLanguagesService
            .get()
            .pipe(take(1))
            .subscribe((dotLang) => {
                dotLang.forEach((lang) => (this.dotLangs[lang.id] = lang));
            });
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
                command: () => this.loadContentTypes()
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
    filterItems(filter = '') {
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
                currentLanguage: this.currentLanguage,
                contentletIdentifier: this.contentletIdentifier
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
                            this.onSelectContentlet({
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
