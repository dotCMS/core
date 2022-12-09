// import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { debounceTime, map, take } from 'rxjs/operators';

import {
    SearchService,
    queryEsParams,
    ESOrderDirection
} from '../../shared/services/search.service';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotLanguageService } from '../../shared';
import { Languages } from '@dotcms/block-editor/services';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs';

@Component({
    selector: 'dot-image-form',
    templateUrl: './image-form.component.html',
    styleUrls: ['./image-form.component.scss']
})
export class ImageFormComponent implements OnInit {
    @Input() languageId = 1;
    @Output() selectedContentlet: EventEmitter<DotCMSContentlet> = new EventEmitter();

    public contentlets$ = new Subject<DotCMSContentlet[][]>();
    public items: DotCMSContentlet[][] = [];
    public form: FormGroup;
    private dotLangs: Languages;

    constructor(
        private searchService: SearchService,
        private dotLanguageService: DotLanguageService,
        private fb: FormBuilder
    ) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });

        this.form.valueChanges.pipe(debounceTime(500)).subscribe((search) => {
            this.items = [];
            this.searchContentlets(search);
        });

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    searchContentlets(search = '', offset = '0') {
        this.searchService
            .get(this.getParams(search, offset))
            .pipe(
                map(({ jsonObjectView }) => {
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

    resetForm() {
        this.items = [];
        this.form.reset({ search: '' }, { emitEvent: false });
    }

    private getParams(search = '', offset = '0'): queryEsParams {
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
