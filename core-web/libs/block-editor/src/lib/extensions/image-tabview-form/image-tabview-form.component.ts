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
import { debounceTime, take, map } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotLanguageService, Languages } from '../../shared';
import {
    ESOrderDirection,
    queryEsParams,
    SearchService
} from '../../shared/services/search.service';

export enum TAB_STATE {
    OPEN = 'OPEN',
    CLOSE = 'CLOSE'
}

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss']
})
export class ImageTabviewFormComponent implements OnInit {
    @Input() languageId = 1;
    @Output() selectedContentlet: EventEmitter<DotCMSContentlet> = new EventEmitter();
    @ViewChild('inputSearch') search!: ElementRef;

    form: FormGroup;
    items: DotCMSContentlet[][] = [];
    resultsSize = 0;
    contentlets$ = new BehaviorSubject<DotCMSContentlet[][]>([]);
    taviewState: TAB_STATE = TAB_STATE.CLOSE;
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

        this.form.valueChanges.pipe(debounceTime(450)).subscribe(({ search }) => {
            this.items = [];
            this.searchContentlets({ search });
        });

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => {
                this.dotLangs = dotLang;
                this.searchContentlets({});
            });
    }

    searchContentlets({ search = '', offset = '0' }): void {
        this.searchService
            .get(this.getParams({ search, offset }))
            .pipe(
                map(({ jsonObjectView, resultsSize }) => {
                    this.resultsSize = resultsSize;
                    const { contentlets } = jsonObjectView;
                    contentlets.forEach((data) => {
                        const i = this.items.length - 1;
                        const contentlet = {
                            ...data,
                            language: this.getContentletLanguage(data.languageId)
                        };

                        if (!this.items[i] || this.items[i]?.length === 2) {
                            this.items.push([contentlet]);
                        } else {
                            this.items[i].push(contentlet);
                        }
                    });

                    return this.items;
                })
            )
            .subscribe((items) => this.contentlets$.next(items));
    }

    resetForm(): void {
        this.items = [];
        this.form.reset({ search: '' }, { emitEvent: false });
    }

    private getParams({ search = '', offset = '0' }): queryEsParams {
        return {
            query: ` catchall:${search}* +baseType:(4 OR 9) +metadata.contenttype:image/* title:'${search}'^15 +languageId:${this.languageId} +deleted:false +working:true`,
            sortOrder: ESOrderDirection.ASC,
            limit: 20,
            offset
        };
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
