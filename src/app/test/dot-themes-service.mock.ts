import { of as observableOf, Observable } from 'rxjs';
import { DotTheme } from '@portlets/dot-edit-page/shared/models/dot-theme.model';
import { mockDotThemes } from './dot-themes.mock';

export class DotThemesServiceMock {
    get(_inode: string): Observable<DotTheme> {
        return observableOf(Object.assign({}, mockDotThemes[0]));
    }
}
