// import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { take } from 'rxjs/operators';

import {
    SearchService,
    queryEsParams,
    ESOrderDirection
} from '../../shared/services/search.service';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotLanguageService } from '../../shared';
import { Languages } from '@dotcms/block-editor/services';

@Component({
    selector: 'dot-image-form',
    templateUrl: './image-form.component.html',
    styleUrls: ['./image-form.component.scss']
})
export class ImageFormComponent implements OnInit {
    @Output() selectedContentlet: EventEmitter<DotCMSContentlet> = new EventEmitter();
    public contentlets: DotCMSContentlet[] = [];
    private dotLangs: Languages;

    constructor(
        private searchService: SearchService,
        private dotLanguageService: DotLanguageService
    ) {}

    ngOnInit(): void {
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

    private getParams(search = ''): queryEsParams {
        return {
            query: `title:${search}* +contentType:(image OR fileAsset) title:'${search}'^15 +languageId:1 +deleted:false +working:true`,
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
