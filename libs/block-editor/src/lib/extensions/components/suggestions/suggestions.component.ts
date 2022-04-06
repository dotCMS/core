import {
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
    ViewChild,
    HostListener,
    AfterViewInit
} from '@angular/core';

import { SafeUrl } from '@angular/platform-browser';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { map, take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';

import { Languages, DotLanguageService } from '../../services/dot-language/dot-language.service';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';
import { SuggestionsService } from '../../services/suggestions/suggestions.service';
import { suggestionOptions } from '@dotcms/block-editor';

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

    mouseMove = true;

    private dotLang: Languages;

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
                        this.initContentletSelection();
                    }
                },
                ...this.items
            ];
        }

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLang = dotLang));
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
        this.list.setFirstItemActive();
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
        this.list.resetKeyManager();
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
        this.initContentletSelection();
    }

    private initContentletSelection() {
        this.suggestionsService
            .getContentTypes()
            .pipe(
                map((items) => {
                    return items.map((item) => {
                        return {
                            label: item.name,
                            icon: item.icon,
                            command: () => {
                                this.suggestionsService
                                    .getContentlets(item.variable)
                                    .pipe(take(1))
                                    .subscribe((contentlets) => {
                                        this.items = contentlets.map((contentlet) => {
                                            const { languageId } = contentlet;
                                            contentlet.language =
                                                this.getContentletLanguage(languageId);
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
                                            this.title = `No results for <b>${item.name}</b>`;
                                            this.cd.detectChanges();
                                        }
                                    });
                            }
                        };
                    });
                }),
                take(1)
            )
            .subscribe((items) => {
                this.title = 'Select a content type';
                this.items = items;
                this.cd.detectChanges();
                this.resetKeyManager();
            });
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLang[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
