// import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Component, OnInit } from '@angular/core';
import {
    SearchService,
    queryEsParams,
    ESOrderDirection
} from '../../shared/services/search.service';
import { FormGroup, FormBuilder } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';

@Component({
    selector: 'dot-image-form',
    templateUrl: './image-form.component.html',
    styleUrls: ['./image-form.component.scss']
    // changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageFormComponent implements OnInit {
    public state = null;
    public form: FormGroup;
    public contentlets = [];

    constructor(private searchService: SearchService, private fb: FormBuilder) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            search: ['']
        });
        this.state = { live: true, working: true, deleted: false, hasLiveVersion: true };
        this.form.valueChanges.pipe(debounceTime(500)).subscribe(({ search }) => {
            this.searchContentlets(search);
        });
        this.searchContentlets();
    }

    private searchContentlets(search = '') {
        this.searchService.get(this.getParams(search)).subscribe(({ contentlets }) => {
            this.contentlets = contentlets;
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
}
