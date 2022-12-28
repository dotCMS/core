import { Injectable } from '@angular/core';
import { ComponentStore, OnStoreInit } from '@ngrx/component-store';
import { ActivatedRoute } from '@angular/router';
import { DotPageRenderState } from '@dotcms/dotcms-models';

export interface DotExperimentsShellState {
    pageTitle: string;
}

const initialState: DotExperimentsShellState = {
    pageTitle: ''
};

@Injectable()
export class DotExperimentsShellStore
    extends ComponentStore<DotExperimentsShellState>
    implements OnStoreInit
{
    //Updater
    readonly setPageDetails = this.updater((state, page: { pageTitle: string }) => ({
        ...state,
        pageTitle: page.pageTitle
    }));

    constructor(private readonly route: ActivatedRoute) {
        super(initialState);
    }

    ngrxOnStoreInit() {
        this.setPageDetails(this._getResolverExperimentsData());
    }

    private _getResolverExperimentsData(): { pageTitle: string } {
        const { page } = this.route.parent?.parent.snapshot.data?.content as DotPageRenderState;

        return {
            pageTitle: page.title
        };
    }
}
