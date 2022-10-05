import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';

export interface DotContainerPropertiesState {
    showPrePostLoopInput: boolean;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    constructor() {
        super({
            showPrePostLoopInput: false
        });
    }

    readonly vm$ = this.select(({ showPrePostLoopInput }: DotContainerPropertiesState) => {
        return {
            showPrePostLoopInput
        };
    });

    readonly updatePrePostLoopInputVisibility = this.updater<boolean>(
        (state: DotContainerPropertiesState, showPrePostLoopInput: boolean) => {
            return {
                ...state,
                showPrePostLoopInput
            };
        }
    );
}
