import { of, Observable } from 'rxjs';

import { DotTheme } from '@models/dot-edit-layout-designer';
import { mockDotThemes } from './dot-themes.mock';

export class DotThemesServiceMock {
    get(_inode: string): Observable<DotTheme> {
        return of(Object.assign({}, mockDotThemes[0]));
    }
}
