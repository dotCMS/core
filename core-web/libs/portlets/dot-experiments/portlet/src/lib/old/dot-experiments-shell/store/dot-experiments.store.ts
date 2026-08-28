import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

export interface DotExperimentsState {
    pageId: string;
    pageTitle: string;
}

const initialState: DotExperimentsState = {
    pageId: '',
    pageTitle: ''
};

@Injectable()
export class DotExperimentsStore extends ComponentStore<DotExperimentsState> {
    readonly getPageId$: Observable<string> = this.select((state) => state.pageId);
    readonly getPageTitle$: Observable<string> = this.select((state) => state.pageTitle);

    constructor() {
        const route = inject(ActivatedRoute);
        const { pageId } = route.snapshot.params;
        const pageTitle = route.snapshot.parent?.parent?.data?.['content']?.page?.title;

        super({ ...initialState, pageId, pageTitle });
    }
}
