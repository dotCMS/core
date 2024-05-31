import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { ColumnComponent } from './column.component';

import { DotPageAssetLayoutColumn } from '../../models';
import { PageResponseMock } from '../../utils/testing.utils';
import { ContainerComponent } from '../container/container.component';

describe('ColumnComponent', () => {
    let spectator: Spectator<ColumnComponent>;

    const createComponent = createComponentFactory({
        component: ColumnComponent,
        imports: [MockComponent(ContainerComponent)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                column: PageResponseMock.layout.body.rows[0].columns[0] as DotPageAssetLayoutColumn
            }
        });
    });

    it('should render one container', () => {
        expect(spectator.queryAll(ContainerComponent)?.length).toBe(1);
    });

    it('should set correct containerClasses', () => {
        expect(spectator.component.containerClasses).toBe('col-start-1 col-end-13');
    });
});
