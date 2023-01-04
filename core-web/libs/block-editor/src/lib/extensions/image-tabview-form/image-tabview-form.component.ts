import {
    Component,
    OnInit,
    Input,
    ViewChild,
    ElementRef,
    ChangeDetectionStrategy,
    ChangeDetectorRef
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { BehaviorSubject, merge } from 'rxjs';
import { debounceTime, map, throttleTime, mergeMap, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotLanguageService, Languages } from '../../shared';
import { DEFAULT_LANG_ID } from '@dotcms/block-editor';

import {
    ESOrderDirection,
    queryEsParams,
    SearchService
} from '../../shared/services/search/search.service';

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageTabviewFormComponent implements OnInit {
    @ViewChild('inputSearch') inputSearch!: ElementRef;

    @Input() languageId = DEFAULT_LANG_ID;
    @Input() loading = true;
    @Input() selectItemCallback: (props: DotCMSContentlet) => void;

    preventScroll = false;
    form: FormGroup;

    offset$ = new BehaviorSubject<number>(0);
    contentlets$: Observable<DotCMSContentlet[][]>;
    itemsLoaded: DotCMSContentlet[][] = [];

    private dotLangs: Languages;

    get searchValue() {
        return this.form.get('search').value;
    }

    constructor(
        private cd: ChangeDetectorRef,
        private searchService: SearchService,
        private dotLanguageService: DotLanguageService,
        private fb: FormBuilder
    ) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });

        this.contentlets$ = merge(
            // Needed when user filters images by search
            this.form.valueChanges.pipe(
                debounceTime(450),
                tap(() => this.setLoading(true)),
                mergeMap(() => this.searchContentlets(0))
            ),
            // Needed when the user scrolls to load the next batch of images
            this.offset$.pipe(
                throttleTime(450),
                mergeMap((offset) => this.searchContentlets(offset * 2))
            )
        );

        this.dotLanguageService.getLanguages().subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    setLoading(value: boolean) {
        this.loading = value;
        this.cd.markForCheck();
    }

    searchContentlets(offset: number = 0) {
        if (offset === 0) {
            this.itemsLoaded = [];
        }

        return this.searchService.get(this.params(offset)).pipe(
            map(({ jsonObjectView: { contentlets } }) => {
                const items = this.setContentletLanguage(contentlets);
                this.fillVirtualItems(items);
                this.setLoading(false);
                this.preventScroll = !contentlets?.length;

                return [...this.itemsLoaded];
            })
        );
    }

    /**
     * Reset Form Values
     *
     * @memberof ImageTabviewFormComponent
     */
    resetForm(): void {
        this.form.reset({ search: '' });
    }

    private params(offset: number): queryEsParams {
        return {
            query: ` +catchall:${this.searchValue}* title:'${this.searchValue}'^15 +languageId:${this.languageId} +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
            sortOrder: ESOrderDirection.ASC,
            limit: 20,
            offset
        };
    }

    /**
     * This method add the Language to the contentets based on their languageId
     *
     * @private
     * @param {DotCMSContentlet[]} contentlets
     * @return {*}
     * @memberof ImageTabviewFormComponent
     */
    private setContentletLanguage(contentlets: DotCMSContentlet[]) {
        return contentlets.map((data) => {
            return {
                ...data,
                language: this.getContentletLanguage(data.languageId)
            };
        });
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }

    /**
     * Create an array of type: [[DotCMSContentlet, DotCMSContentlet], ...]
     * Due PrimeNg virtual scroll allows only displaying one element at a time [https://primefaces.org/primeng/virtualscroller],
     * and figma's layout requires displaying two columns of contentlets [https://github.com/dotCMS/core/issues/23235]
     *
     * @private
     * @param {DotCMSContentlet[]} contentlets
     * @memberof ImageTabviewFormComponent
     */
    private fillVirtualItems(contentlets: DotCMSContentlet[]) {
        contentlets.forEach((contentlet) => {
            const i = this.itemsLoaded.length - 1;
            if (this.itemsLoaded[i]?.length < 2) {
                this.itemsLoaded[i].push(contentlet);
            } else {
                this.itemsLoaded.push([contentlet]);
            }
        });
    }
}
