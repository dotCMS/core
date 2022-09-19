import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { LoadingState } from '@portlets/shared/models/shared-models';

export interface DotExperimentsState {
    pageId: string;
    state: LoadingState;
}

const initialState: DotExperimentsState = {
    pageId: '',
    state: LoadingState.INIT
};

@Injectable()
export class DotExperimentsStore extends ComponentStore<DotExperimentsState> {
    constructor() {
        super(initialState);
    }
    readonly getState$ = this.select((state) => state.state);
}
