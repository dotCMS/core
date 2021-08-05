import { ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';

import { map, take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';

import { SuggestionsService } from '../../services/suggestions.service';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { SuggestionListComponent } from '../suggestion-list/suggestion-list.component';

@Component({
    selector: 'dotcms-suggestions',
    templateUrl: './suggestions.component.html',
    styleUrls: ['./suggestions.component.scss']
})
export class SuggestionsComponent implements OnInit {
    @ViewChild('list', { static: true }) list: SuggestionListComponent;

    @Input() command: ({
        payload,
        type
    }: {
        payload?: DotCMSContentlet;
        type: { name: string; level?: number };
    }) => void;
    items: MenuItem[] = [];

    constructor(private suggestionsService: SuggestionsService, private cd: ChangeDetectorRef) {}

    onKeyDown(e: KeyboardEvent) {
        this.list.onKeydown(e);
    }

    ngOnInit(): void {
        const headings = [...Array(6).keys()].map((level) => {
            return {
                label: `Heading ${level + 1}`,
                icon: 'pi pi-circle-on',
                command: () => {
                    this.command({
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
                icon: 'pi pi-circle-on',
                command: () => {
                    this.command({
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
                icon: 'pi pi-circle-on',
                command: () => {
                    this.command({
                        type: {
                            name: 'listOrdered'
                        }
                    });
                }
            },
            {
                label: 'List Unordered',
                icon: 'pi pi-circle-on',
                command: () => {
                    this.command({
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
                icon: 'pi pi-plus',
                command: () => {
                    this.initContentletSelection();
                }
            },
            ...headings,
            ...paragraph,
            ...list
        ];
    }

    private initContentletSelection() {
        this.suggestionsService
            .getContentTypes()
            .pipe(
                map((items) => {
                    return items.map((item) => {
                        return {
                            label: item['name'],
                            icon: 'pi pi-fw pi-plus',
                            command: () => {
                                this.suggestionsService
                                    .getContentlets(item['variable'])
                                    .pipe(take(1))
                                    .subscribe((contentlets) => {
                                        this.items = contentlets.map((contentlet) => {
                                            return {
                                                label: contentlet['title'],
                                                icon: 'pi pi-fw pi-plus',
                                                command: () => {
                                                    this.command({
                                                        payload: contentlet,
                                                        type: {
                                                            name: 'dotContent'
                                                        }
                                                    });
                                                }
                                            };
                                        });

                                        this.cd.detectChanges();
                                    });
                            }
                        };
                    });
                }),
                take(1)
            )
            .subscribe((items) => {
                this.items = items;
                this.cd.detectChanges();
            });
    }
}
