import { createComponentFactory, Spectator } from '@openng/spectator';

import { BlockEditorNode } from '@dotcms/types';

// grid-block and the item component import each other. Load the item first, as the renderer
// does at runtime; loading grid-block first leaves DotGridBlock undefined in the item's
// imports and Angular fails with NG0919.
import '../item/dotcms-block-editor-item.component';
import { DotGridBlock } from './grid-block.component';

const gridNode = (columns: unknown, columnCount: number): BlockEditorNode => ({
    type: 'gridBlock',
    attrs: { columns },
    content: Array.from({ length: columnCount }, () => ({ type: 'gridColumn', content: [] }))
});

describe('DotGridBlock', () => {
    let spectator: Spectator<DotGridBlock>;

    const createComponent = createComponentFactory({
        component: DotGridBlock,
        detectChanges: false
    });

    const columnSpans = () =>
        (spectator.queryAll('[data-type="gridColumn"]') as HTMLElement[]).map(
            (column) => column.style.gridColumn
        );

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render one column per gridColumn, spanning the widths in attrs.columns', () => {
        spectator.setInput('node', gridNode([4, 8], 2));
        spectator.detectChanges();

        expect(columnSpans()).toEqual(['span 4', 'span 8']);
    });

    it('should give a column past the two in attrs.columns the default span of 6', () => {
        spectator.setInput('node', gridNode([4, 8], 3));
        spectator.detectChanges();

        expect(columnSpans()).toEqual(['span 4', 'span 8', 'span 6']);
    });

    it('should fall back to 6/6 when attrs.columns is not two finite numbers', () => {
        spectator.setInput('node', gridNode([4], 2));
        spectator.detectChanges();

        expect(columnSpans()).toEqual(['span 6', 'span 6']);
    });
});
