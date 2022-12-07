// import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { debounceTime, take } from 'rxjs/operators';

import {
    SearchService,
    queryEsParams,
    ESOrderDirection
} from '../../shared/services/search.service';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotLanguageService } from '../../shared';
import { Languages } from '@dotcms/block-editor/services';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
    selector: 'dot-image-form',
    templateUrl: './image-form.component.html',
    styleUrls: ['./image-form.component.scss']
})
export class ImageFormComponent implements OnInit {
    @Input() languageId = 1;
    @Output() selectedContentlet: EventEmitter<DotCMSContentlet> = new EventEmitter();

    public contentlets: DotCMSContentlet[] = [];
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

        this.form.valueChanges.pipe(debounceTime(500)).subscribe(({ search }) => {
            this.searchContentlets(search);
        });

        this.dotLanguageService
            .getLanguages()
            .pipe(take(1))
            .subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    searchContentlets(search = '') {
        this.searchService.get(this.getParams(search)).subscribe(({ contentlets }) => {
            this.contentlets = contentlets.map((contentlet) => {
                return {
                    ...contentlet,
                    language: this.getContentletLanguage(contentlet.languageId)
                };
            });
        });
    }

    resetForm() {
        this.form.reset();
    }

    private getParams(search = ''): queryEsParams {
        return {
            query: `title:${search}* +contentType:(image OR fileAsset) title:'${search}'^15 +languageId:${this.languageId} +deleted:false +working:true`,
            sortOrder: ESOrderDirection.ASC,
            limit: 20,
            offset: '0'
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
