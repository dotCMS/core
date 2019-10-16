import { Injectable } from '@angular/core';
import { of, Observable, BehaviorSubject } from 'rxjs';

import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockDotRenderedPageState } from './dot-rendered-page-state.mock';
import { DotPageRenderOptions } from '@services/dot-page-render/dot-page-render.service';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { DotPersona } from '@models/dot-persona/dot-persona.model';

@Injectable()
export class DotPageStateServiceMock {
    haveContent$ = new BehaviorSubject<boolean>(mockDotRenderedPageState.numberContents > 0);

    get(_url: string): Observable<DotPageRenderState> {
        return of(mockDotRenderedPageState);
    }

    setLock(_options: DotPageRenderOptions, _lock: boolean = null): void {}

    setDevice(_device: DotDevice): void {}

    setLanguage(_language: number): void {}

    setPersona(_persona: DotPersona): void {}
}
