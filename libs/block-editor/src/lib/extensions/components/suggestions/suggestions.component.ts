import { ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';

import { map, take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';

import { SuggestionsService } from '../../services/suggestions.service';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { headerIcons, pIcon, ulIcon, olIcon } from './suggestion-icons';

export interface SuggestionsCommandProps {
    payload?: DotCMSContentlet;
    type: { name: string; level?: number };
}

export interface DotMenuItem  extends Omit<MenuItem, 'icon'> {
    icon: string | SafeUrl;
}

@Component({
    selector: 'dotcms-suggestions',
    templateUrl: './suggestions.component.html',
    styleUrls: ['./suggestions.component.scss']
})
export class SuggestionsComponent implements OnInit {
    @ViewChild('list', { static: true }) list: SuggestionListComponent;

    @Input() onSelection: (props: SuggestionsCommandProps) => void;
    items: DotMenuItem[] = [];

    title = 'Select a block';

    constructor(
        private suggestionsService: SuggestionsService,
        private cd: ChangeDetectorRef,
        private domSanitizer: DomSanitizer
    ) {}

    ngOnInit(): void {
        const headings = [...Array(3).keys()].map((level) => {
            const size = level + 1;
            return {
                label: `Heading ${size}`,
                icon: this.sanitizeUrl(headerIcons[level]),
                command: () => {
                    this.onSelection({
                        type: {
                            name: 'heading',
                            level: level + 1
                        }
                    });
                }
            };
        });

        const paragraph = [
            {
                label: 'Paragraph',
                icon: this.sanitizeUrl(pIcon),
                command: () => {
                    this.onSelection({
                        type: {
                            name: 'paragraph'
                        }
                    });
                }
            }
        ];

        const list = [
            {
                label: 'List Ordered',
                icon: this.sanitizeUrl(olIcon),
                command: () => {
                    this.onSelection({
                        type: {
                            name: 'listOrdered'
                        }
                    });
                }
            },
            {
                label: 'List Unordered',
                icon: this.sanitizeUrl(ulIcon),
                command: () => {
                    this.onSelection({
                        type: {
                            name: 'listUnordered'
                        }
                    });
                }
            }
        ];
        this.items = [
            {
                label: 'Contentlets',
                icon: 'receipt',
                command: () => {
                    this.initContentletSelection();
                }
            },
            ...headings,
            ...paragraph,
            ...list
        ];
    }

    /**
     * Execute the item command
     *
     * @memberof SuggestionsComponent
     */
    execCommand() {
        this.list.execCommand();
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
     * Set the first item active
     *
     * @memberof SuggestionsComponent
     */
    setFirstItemActive() {
        this.list.setFirstItemActive();
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
     * Handle the active item on menu events
     *
     * @param {MouseEvent} e
     * @memberof SuggestionsComponent
     */
    onMouseEnter(e: MouseEvent) {
        e.preventDefault();
        const index = Number((e.target as HTMLElement).dataset.index);
        this.list.updateActiveItem(index);
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
                                            return {
                                                label: contentlet.title,
                                                icon: 'image',
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

                                        this.title = 'Select a contentlet';
                                        this.cd.detectChanges();
                                        this.resetKeyManager();
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

    private sanitizeUrl( url: string ): SafeUrl {
        return this.domSanitizer.bypassSecurityTrustUrl(url);
    }
}
