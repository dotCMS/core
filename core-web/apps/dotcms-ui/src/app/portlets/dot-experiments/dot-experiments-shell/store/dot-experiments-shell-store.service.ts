import { Injectable } from '@angular/core';
import { ComponentStore, OnStoreInit } from '@ngrx/component-store';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { ActivatedRoute } from '@angular/router';

export interface DotExperimentsShellState {
    pageId: string;
    pageTitle: string;
}

const initialState: DotExperimentsShellState = {
    pageId: '',
    pageTitle: ''
};

@Injectable()
export class DotExperimentsShellStore
    extends ComponentStore<DotExperimentsShellState>
    implements OnStoreInit
{
    //Updater
    readonly setPageDetails = this.updater(
        (state, page: { pageId: string; pageTitle: string }) => ({
            ...state,
            pageId: page.pageId,
            pageTitle: page.pageTitle
        })
    );

    constructor(private readonly route: ActivatedRoute) {
        super(initialState);
    }

    ngrxOnStoreInit() {
        this.setPageDetails(this._getResolverExperimentsData());
    }

    private _getResolverExperimentsData(): { pageId: string; pageTitle: string } {
        const { page } = this.route.parent?.parent.snapshot.data?.content as DotPageRenderState;

        return {
            pageId: page.identifier,
            pageTitle: page.title
        };
    }
}
