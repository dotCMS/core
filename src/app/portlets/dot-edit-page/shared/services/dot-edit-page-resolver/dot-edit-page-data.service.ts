import { Injectable } from '@angular/core';
import { DotRenderedPageState } from '../../models/dot-rendered-page-state.model';

/**
 * Allow send data to DotEditPageResolver
 */
@Injectable()
export class DotEditPageDataService {
    private dotRenderedPageState: DotRenderedPageState;

    /**
     * Out dotRenderedPageState into cache to be used by DotEditPageResolver
     *
     * @param dotRenderedPageState
     */
    set(dotRenderedPageState: DotRenderedPageState) {
        this.dotRenderedPageState = dotRenderedPageState;
    }

    /**
     * Return the object in cache and clean
     *
     * @returns DotRenderedPageState
     * @memberof DotEditPageDataService
     */
    getAndClean(): DotRenderedPageState {
        const data = this.dotRenderedPageState;
        this.dotRenderedPageState = null;
        return data;
    }
}
