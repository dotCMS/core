import { DotTheme } from '../portlets/dot-edit-page/shared/models/dot-theme.model';
import { mockDotThemes } from './dot-themes.mock';
import { Observable } from 'rxjs/Observable';

export class DotThemesServiceMock {
    get(_inode: string): Observable<DotTheme> {
        return Observable.of(Object.assign({}, mockDotThemes[0]));
    }
}
