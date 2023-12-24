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
import { DotPaletteStore } from './store/edit-ema-palette.store';

import { PALETTE_TYPES } from '../../../shared/enums';
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

    onDragStart(event: DragEvent) {
        this.dragStart.emit(event);
    }

    onDragEnd(event: DragEvent) {
        this.dragEnd.emit(event);
    }

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

    showContentTypes() {
        this.store.changeView(PALETTE_TYPES.CONTENTTYPE);
    }

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

    onPaginate({ contentTypeVarName, page }) {
        this.store.loadContentlets({
            filter: '',
            languageId: this.languageId.toString(),
            contenttypeName: contentTypeVarName,
            page: page
        });
    }

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
