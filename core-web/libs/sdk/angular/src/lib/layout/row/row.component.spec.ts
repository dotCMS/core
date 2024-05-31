import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { RowComponent } from './row.component';

import { DotPageAssetLayoutRow } from '../../models';
import { PageResponseMock } from '../../utils/testing.utils';
import { ColumnComponent } from '../column/column.component';

describe('RowComponent', () => {
    let spectator: Spectator<RowComponent>;
    const createComponent = createComponentFactory({
        component: RowComponent,
        imports: [MockComponent(ColumnComponent)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                row: PageResponseMock.layout.body.rows[1] as DotPageAssetLayoutRow
            }
        });
    });

    it('should render two columns', () => {
        expect(spectator.queryAll(ColumnComponent)?.length).toBe(4);
    });
});
