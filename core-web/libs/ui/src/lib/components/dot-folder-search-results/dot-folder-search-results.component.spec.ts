import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { LOAD_MORE_NODE_TYPE, TreeNodeItem } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFolderSearchResultsComponent } from './dot-folder-search-results.component';

/**
 * Flat list of folder-search results — the building block shared by the AssetPicker sidebar and the
 * Site/Folder field (spec FR-026).
 *
 * It is deliberately a list and not a mode of `DotFolderTreeComponent` (FR-029): the result set is
 * flat, so there is nothing to expand, and that component is shared with Content Drive and is being
 * edited concurrently by #37174.
 *
 * Everything below is presentation. The component filters nothing, pages nothing and owns no empty
 * state — those belong to each consumer (FR-028), which is what lets the Site/Folder field adopt it
 * with no visible change.
 */
const folder = (key: string, label: string, path: string): TreeNodeItem =>
    ({
        key,
        label,
        data: { type: 'folder', id: key, hostname: '//demo.dotcms.com', path }
    }) as TreeNodeItem;

const RESULTS: TreeNodeItem[] = [
    folder('f1', 'activities', '/activities/'),
    folder('f2', 'images', '/images/'),
    folder('f3', 'thumbnails', '/images/thumbnails/')
];

const LOAD_MORE: TreeNodeItem = {
    key: 'load-more:search',
    label: '',
    type: LOAD_MORE_NODE_TYPE,
    data: { type: LOAD_MORE_NODE_TYPE, id: 'load-more:search', nextPage: 2 }
} as TreeNodeItem;

describe('DotFolderSearchResultsComponent', () => {
    let spectator: Spectator<DotFolderSearchResultsComponent>;

    const createComponent = createComponentFactory({
        component: DotFolderSearchResultsComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }],
        detectChanges: false
    });

    const rows = () => spectator.queryAll(byTestId('folder-search-result'));

    describe('rendering rows', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { results: RESULTS } });
            spectator.detectChanges();
        });

        it('renders one row per result', () => {
            expect(rows()).toHaveLength(3);
        });

        it('renders the folder name on the first line', () => {
            const names = spectator
                .queryAll(byTestId('folder-search-result-name'))
                .map((el) => el.textContent?.trim());

            expect(names).toEqual(['activities', 'images', 'thumbnails']);
        });

        it('renders the full path on the second line', () => {
            const paths = spectator
                .queryAll(byTestId('folder-search-result-path'))
                .map((el) => el.textContent?.trim());

            expect(paths).toEqual([
                'demo.dotcms.com / activities',
                'demo.dotcms.com / images',
                'demo.dotcms.com / images / thumbnails'
            ]);
        });

        it('truncates both lines rather than wrapping, so a long path cannot widen the sidebar', () => {
            // SC-008. Asserted on the classes because core-web has no visual-regression harness.
            expect(spectator.query(byTestId('folder-search-result-name'))).toHaveClass('truncate');
            expect(spectator.query(byTestId('folder-search-result-path'))).toHaveClass('truncate');
        });

        it('renders each row as a real button so it is keyboard reachable', () => {
            expect(rows()[0].tagName.toLowerCase()).toBe('button');
            expect(rows()[0].getAttribute('type')).toBe('button');
        });

        it('renders a folder icon on every row', () => {
            expect(spectator.queryAll(byTestId('folder-search-result-icon'))).toHaveLength(3);
        });
    });

    describe('selection', () => {
        it('marks exactly the row whose key matches selectedKey', () => {
            spectator = createComponent({ props: { results: RESULTS, selectedKey: 'f2' } });
            spectator.detectChanges();

            const selected = rows().filter((row) => row.getAttribute('aria-current') === 'true');

            expect(selected).toHaveLength(1);
            expect(selected[0].textContent).toContain('images');
        });

        it('marks nothing when selectedKey is null', () => {
            spectator = createComponent({ props: { results: RESULTS, selectedKey: null } });
            spectator.detectChanges();

            expect(rows().filter((r) => r.getAttribute('aria-current') === 'true')).toHaveLength(0);
        });

        it('selects by key, not by object reference, so a re-published clone keeps the highlight', () => {
            spectator = createComponent({ props: { results: RESULTS, selectedKey: 'f2' } });
            spectator.detectChanges();

            // A consumer re-publishing its results hands over a fresh object graph.
            spectator.setInput('results', structuredClone(RESULTS));
            spectator.detectChanges();

            expect(rows().filter((r) => r.getAttribute('aria-current') === 'true')).toHaveLength(1);
        });
    });

    describe('emitting selections', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { results: RESULTS } });
            spectator.detectChanges();
        });

        it('emits resultSelect with the clicked node', () => {
            const onSelect = jest.fn();
            spectator.output('resultSelect').subscribe(onSelect);

            spectator.click(rows()[1]);

            expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ key: 'f2' }));
        });

        it('emits resultSelect on keyboard activation', () => {
            const onSelect = jest.fn();
            spectator.output('resultSelect').subscribe(onSelect);

            // A <button> activates on Enter/Space by dispatching a click — asserting the click
            // handler is on the button (not a div) is what makes keyboard support real.
            (rows()[0] as HTMLButtonElement).click();

            expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ key: 'f1' }));
        });
    });

    describe('load-more row (consumer-owned paging, FR-028)', () => {
        it('renders no load-more row when loadMoreLabelKey is empty — the picker caps at one page', () => {
            spectator = createComponent({ props: { results: RESULTS } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('folder-search-load-more'))).toBeNull();
        });

        it('renders the load-more row when a consumer supplies a label key', () => {
            spectator = createComponent({
                props: {
                    results: [...RESULTS, LOAD_MORE],
                    loadMoreLabelKey: 'dot.file.field.host.folder.action.load.more'
                }
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('folder-search-load-more'))).not.toBeNull();
        });

        it('does not count the load-more sentinel as a result row', () => {
            spectator = createComponent({
                props: {
                    results: [...RESULTS, LOAD_MORE],
                    loadMoreLabelKey: 'dot.file.field.host.folder.action.load.more'
                }
            });
            spectator.detectChanges();

            expect(rows()).toHaveLength(3);
        });

        it('emits loadMore — not resultSelect — when the sentinel is activated', () => {
            spectator = createComponent({
                props: {
                    results: [...RESULTS, LOAD_MORE],
                    loadMoreLabelKey: 'dot.file.field.host.folder.action.load.more'
                }
            });
            spectator.detectChanges();

            const onLoadMore = jest.fn();
            const onSelect = jest.fn();
            spectator.output('loadMore').subscribe(onLoadMore);
            spectator.output('resultSelect').subscribe(onSelect);

            spectator.click(byTestId('folder-search-load-more'));

            expect(onLoadMore).toHaveBeenCalledWith(
                expect.objectContaining({ key: 'load-more:search' })
            );
            expect(onSelect).not.toHaveBeenCalled();
        });
    });

    describe('empty and loading', () => {
        it('renders nothing at all for an empty result set — the empty state belongs to the consumer', () => {
            spectator = createComponent({ props: { results: [] } });
            spectator.detectChanges();

            expect(rows()).toHaveLength(0);
            expect(spectator.query(byTestId('folder-search-results-empty'))).toBeNull();
        });

        it('shows the loading affordance without blanking the rows already on screen', () => {
            spectator = createComponent({ props: { results: RESULTS, loading: true } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('folder-search-results-loading'))).not.toBeNull();
            expect(rows()).toHaveLength(3);
        });
    });

    describe('test ids are overridable, because consumers render more than one list per screen', () => {
        it('applies the supplied listTestId and rowTestId', () => {
            spectator = createComponent({
                props: { results: RESULTS, listTestId: 'my-list', rowTestId: 'my-row' }
            });
            spectator.detectChanges();

            expect(spectator.query(byTestId('my-list'))).not.toBeNull();
            expect(spectator.queryAll(byTestId('my-row'))).toHaveLength(3);
        });
    });
});
