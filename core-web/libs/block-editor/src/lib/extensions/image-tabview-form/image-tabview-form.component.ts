import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    ElementRef
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { debounceTime, take, map, throttleTime, switchMap, filter } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotLanguageService, Languages } from '../../shared';
import {
    ESOrderDirection,
    queryEsParams,
    SearchService
} from '../../shared/services/search/search.service';

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss']
})
export class ImageTabviewFormComponent implements OnInit {
    @Input() languageId = 1;
    @Output() selectedContentlet: EventEmitter<DotCMSContentlet> = new EventEmitter();
    @ViewChild('inputSearch') inputSearch!: ElementRef;

    loading = true;
    done = false;
    search$ = new BehaviorSubject<number>(null);
    contentlets$: Observable<DotCMSContentlet[][]>;
    virtualItems: DotCMSContentlet[][] = [];
    form: FormGroup;

    private dotLangs: Languages;

    get currentSearch() {
        return this.form.get('search').value;
    }

    constructor(
        private searchService: SearchService,
        private dotLanguageService: DotLanguageService,
        private fb: FormBuilder
    ) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });

        this.contentlets$ = this.search$.pipe(
            filter((x) => x != null),
            throttleTime(500),
            // offset * 2 -> Because of the virtual scroll ([[DotCMSContentlet, DotCMSContentlet], ...])
            switchMap((offset) => this.searchContentlets(Math.ceil(offset * 2)))
        );

        this.form.valueChanges.pipe(debounceTime(450)).subscribe(() => {
            this.loading = true;
            this.search$.next(0);
        });

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => {
                this.dotLangs = dotLang;
                this.search$.next(0);
            });
    }

    searchContentlets(offset: number = 0) {
        if (offset == 0) {
            this.virtualItems = [];
        }

        return this.searchService.get(this.params(offset)).pipe(
            map(({ jsonObjectView: { contentlets } }) => {
                const items = this.setContentletLanguage(contentlets);
                this.fillVirtualItems(items);
                this.loading = false;
                this.done = !contentlets?.length;

                return this.virtualItems;
            })
        );
    }

    /**
     * Reset Form Values
     *
     * @memberof ImageTabviewFormComponent
     */
    resetForm(): void {
        this.virtualItems = [];
        this.form.reset({ search: '' }, { emitEvent: false });
    }

    private params(offset: number): queryEsParams {
        return {
            query: ` +catchall:${this.currentSearch}* title:'${this.currentSearch}'^15 +languageId:${this.languageId} +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
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
            const i = this.virtualItems.length - 1;
            if (!this.virtualItems[i] || this.virtualItems[i]?.length === 2) {
                this.virtualItems.push([contentlet]);
            } else {
                this.virtualItems[i].push(contentlet);
            }
        });
    }
}
