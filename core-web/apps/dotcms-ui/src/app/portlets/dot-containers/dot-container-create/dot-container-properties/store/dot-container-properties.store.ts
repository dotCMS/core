import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';

export interface DotContainerPropertiesState {
    showPrePostLoopInput: boolean;
    isContentTypeVisible: boolean;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    constructor() {
        super({
            showPrePostLoopInput: false,
            isContentTypeVisible: false
        });
    }

    readonly vm$ = this.select(
        ({ showPrePostLoopInput, isContentTypeVisible }: DotContainerPropertiesState) => {
            return {
                showPrePostLoopInput,
                isContentTypeVisible
            };
        }
    );

    readonly updatePrePostLoopInputVisibility = this.updater<boolean>(
        (state: DotContainerPropertiesState, showPrePostLoopInput: boolean) => {
            return {
                ...state,
                showPrePostLoopInput
            };
        }
    );

    readonly updateContentTypeVisibilty = this.updater<boolean>(
        (state: DotContainerPropertiesState, isContentTypeVisible: boolean) => {
            return {
                ...state,
                isContentTypeVisible
            };
        }
    );
}
