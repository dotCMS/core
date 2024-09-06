import { of, Observable, BehaviorSubject } from 'rxjs';

import { Injectable } from '@angular/core';

import {
    DotDevice,
    DotPageRenderOptions,
    DotPageRenderState,
    DotPersona
} from '@dotcms/dotcms-models';

import { mockDotRenderedPageState } from './dot-rendered-page-state.mock';

@Injectable()
export class DotPageStateServiceMock {
    haveContent$ = new BehaviorSubject<boolean>(mockDotRenderedPageState.numberContents > 0);

    get(_url: string): Observable<DotPageRenderState> {
        return of(mockDotRenderedPageState);
    }

    setLock(_options: DotPageRenderOptions, _lock: boolean = null): void {
        /* */
    }

    setDevice(_device: DotDevice): void {
        /* */
    }

    setLanguage(_language: number): void {
        /* */
    }

    setPersona(_persona: DotPersona): void {
        /* */
    }

    setSeoMedia(_seoMedia: string): void {
        /* */
    }
}
