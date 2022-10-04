import { ComponentStore } from '@ngrx/component-store';
import { StructureTypeView } from '@models/contentlet';
import { take } from 'rxjs/operators';
import { DotContentTypeService } from '@dotcms/app/api/services/dot-content-type';
import { Injectable } from '@angular/core';

export interface DotContentEditorState {
    activeTabIndex: number;
    contentTypes: StructureTypeView[];
}

@Injectable()
export class DotContentEditorStore extends ComponentStore<DotContentEditorState> {
    constructor(private dotContentTypeService: DotContentTypeService) {
        super({
            activeTabIndex: 1,
            contentTypes: []
        });

        this.dotContentTypeService
            .getAllContentTypes()
            .pipe(take(1))
            .subscribe((contentTypes) => {
                this.setState({
                    activeTabIndex: 1,
                    contentTypes: contentTypes
                });
            });
    }

    readonly vm$ = this.select((state: DotContentEditorState) => state);
}
