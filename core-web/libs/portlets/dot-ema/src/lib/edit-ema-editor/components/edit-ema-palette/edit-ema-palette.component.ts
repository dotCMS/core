import { Subject } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';
import { FormControl } from '@angular/forms';

import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { DotESContentService, DotPropertiesService } from '@dotcms/data-access';
import { DotContainerStructure, DotPageContainerStructure } from '@dotcms/dotcms-models';

import { EditEmaPaletteContentTypeComponent } from './components/edit-ema-palette-content-type/edit-ema-palette-content-type.component';
import { EditEmaPaletteContentletsComponent } from './components/edit-ema-palette-contentlets/edit-ema-palette-contentlets.component';
import { PALETTE_TYPES } from './shared/edit-ema-palette.enums';
import { DotPaletteStore } from './store/edit-ema-palette.store';

@Component({
    selector: 'dot-edit-ema-palette',
    standalone: true,
    templateUrl: './edit-ema-palette.component.html',
    styleUrls: ['./edit-ema-palette.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgIf,
        AsyncPipe,
        EditEmaPaletteContentTypeComponent,
        EditEmaPaletteContentletsComponent
    ],
    providers: [DotPaletteStore, DotESContentService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class EditEmaPaletteComponent implements OnInit, OnDestroy {
    @Input() languageId: number;
    @Input() containers: DotPageContainerStructure;

    @Output() dragStart = new EventEmitter();
    @Output() dragEnd = new EventEmitter();

    private readonly store = inject(DotPaletteStore);
    private readonly dotConfigurationService = inject(DotPropertiesService);
    private destroy$ = new Subject<void>();

    vm$ = this.store.vm$;
    searchContenttype = new FormControl('');
    searchContentlet = new FormControl('');

    allowedContentTypes: string[];
    currentContenttype = ''; //Search how avoid to use this, its neede in line 148.

    PALETTETYPE = PALETTE_TYPES;

    ngOnInit() {
        this.listenToSearchContents();

        this.dotConfigurationService
            .getKeyAsList('CONTENT_PALETTE_HIDDEN_CONTENT_TYPES')
            .subscribe((results) => {
                this.allowedContentTypes = this.filterAllowedContentTypes(results) || [];
                this.store.loadContentTypes({
                    filter: '',
                    allowedContent: this.allowedContentTypes
                });
            });
    }

    /**
     * Event handler for the drag start event.
     * @param event The drag event.
     */
    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    /**
     * Handles the drag end event.
     * @param event The drag event.
     */
    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

    /**
     * Shows contentlets from a specific content type.
     *
     * @param contentTypeName - The name of the content type.
     */
    showContentletsFromContentType(contentTypeName: string) {
        this.searchContentlet.setValue('', { emitEvent: false });

        this.currentContenttype = contentTypeName;
        this.store.loadContentlets({
            filter: '',
            languageId: this.languageId.toString(),
            contenttypeName: contentTypeName
        });

        this.store.changeView(PALETTE_TYPES.CONTENTLET);
    }

    /**
     * Shows the content types in the palette.
     */
    showContentTypes() {
        this.store.changeView(PALETTE_TYPES.CONTENTTYPE);
        this.store.resetContentlets();
    }

    /**
     * Filters the allowed content types based on a blacklist.
     *
     * @param blackList - An array of content types to be excluded from the allowed list.
     * @returns An array of allowed content types.
     */
    private filterAllowedContentTypes(blackList: string[] = []): string[] {
        const allowedContent = new Set();
        Object.values(this.containers).forEach((container) => {
            Object.values(container.containerStructures).forEach(
                (containerStructure: DotContainerStructure) => {
                    allowedContent.add(containerStructure.contentTypeVar.toLocaleLowerCase());
                }
            );
        });
        blackList.forEach((content) => allowedContent.delete(content.toLocaleLowerCase()));

        return [...allowedContent] as string[];
    }

    /**
     * Handles the pagination event for the edit-ema-palette component.
     * @param {Object} options - The pagination options.
     * @param {string} options.contentTypeVarName - The variable name of the content type.
     * @param {number} options.page - The page number to load.
     * @returns {void}
     */
    onPaginate({ contentTypeVarName, page }) {
        this.store.loadContentlets({
            filter: '',
            languageId: this.languageId.toString(),
            contenttypeName: contentTypeVarName,
            page: page
        });
    }

    /**
     * Listens to changes in the search input fields for content types and contentlets,
     * and loads the corresponding data based on the search criteria.
     */
    listenToSearchContents() {
        this.searchContenttype.valueChanges
            .pipe(takeUntil(this.destroy$), debounceTime(1000), distinctUntilChanged())
            .subscribe((res) => {
                this.store.loadContentTypes({
                    filter: res,
                    allowedContent: this.allowedContentTypes
                });
            });

        this.searchContentlet.valueChanges
            .pipe(takeUntil(this.destroy$), debounceTime(1000), distinctUntilChanged())
            .subscribe((res) => {
                this.store.loadContentlets({
                    filter: res,
                    contenttypeName: this.currentContenttype,
                    languageId: this.languageId.toString()
                });
            });
    }

    ngOnDestroy() {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
