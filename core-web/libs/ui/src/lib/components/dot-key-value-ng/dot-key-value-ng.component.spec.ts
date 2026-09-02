import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/jest';

import { Table } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotKeyValue, DotKeyValueComponent } from './dot-key-value-ng.component';

export const mockKeyValue: DotKeyValue[] = [
    {
        key: 'name',
        hidden: false,
        value: 'John'
    },
    {
        key: 'password',
        hidden: true,
        value: '123456'
    }
];

const messageServiceMock = new MockDotMessageService({
    'keyValue.actions_header.label': 'Actions',
    'keyValue.value_header.label': 'Value',
    'keyValue.key_header.label': 'Key',
    'keyValue.value_no_rows.label': 'No Rows',
    'keyValue.hidden_header.label': 'Hidden',
    'keyValue.key_input.placeholder': 'Enter Key',
    'keyValue.value_input.placeholder': 'Enter Value',
    'keyValue.key_input.required': 'This field is required',
    'keyValue.value_input.required': 'This field is required',
    'keyValue.key_input.duplicated': 'This key already exists',
    'keyValue.value_hidden': 'Value hidden',
    'keyValue.action.load_more': 'Load more',
    Delete: 'Delete',
    Reorder: 'Reorder',
    add: 'Add'
});

/**
 * The container owns the list and the table frame. Row-level behaviour lives in
 * the two row components and is covered by their own specs; what is tested here
 * is the public contract and what the container does with the list.
 */
describe('DotKeyValueComponent', () => {
    let spectator: Spectator<DotKeyValueComponent>;

    const createComponent = createComponentFactory({
        component: DotKeyValueComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const create = (props: Partial<Record<string, unknown>> = {}) => {
        spectator = createComponent({
            props: { variables: [...mockKeyValue], ...props } as unknown
        });
        spectator.detectChanges();

        return spectator;
    };

    beforeEach(() => create({ showHiddenField: false }));

    /**
     * Characterization tests pinning the component's PUBLIC surface.
     *
     * Three separate features consume this component through `@dotcms/ui`
     * (Edit Content, Field Variables and Apps custom properties), so its inputs
     * and outputs are a contract, not implementation detail. If one of these
     * starts failing, a consumer is about to break.
     *
     * Source of truth: specs/37191-key-value-field-redesign/contracts/dot-key-value-ng.component.md
     */
    describe('public contract', () => {
        it('should default every capability off so consumers opt in explicitly', () => {
            const bare = createComponent();

            expect(bare.component.$variables()).toEqual([]);
            expect(bare.component.$showHiddenField()).toBe(false);
        });

        it('should expose the documented inputs', () => {
            spectator.setInput('showHiddenField', true);
            spectator.detectChanges();

            expect(spectator.component.$variables()).toEqual(mockKeyValue);
            expect(spectator.component.$showHiddenField()).toBe(true);
        });

        it('should expose the documented outputs', () => {
            // `updatedList` is consumed by Edit Content and Apps (whole array);
            // `save` / `update` / `delete` are consumed by Field Variables to
            // persist per row. Both channels must survive any refactor.
            expect(spectator.component.updatedList).toBeDefined();
            expect(spectator.component.save).toBeDefined();
            expect(spectator.component.update).toBeDefined();
            expect(spectator.component.delete).toBeDefined();
        });

        it('should re-seed the working list when the consumer supplies a new one', () => {
            spectator.setInput('variables', [{ key: 'other', value: 'v' }]);
            spectator.detectChanges();

            expect(spectator.component.$variableList()).toEqual([{ key: 'other', value: 'v' }]);
        });
    });

    describe('rendering', () => {
        it('should render one row per pair', () => {
            expect(spectator.queryAll(byTestId('dot-key-value-key')).length).toBe(2);
            expect(spectator.query(Table).value).toEqual(mockKeyValue);
        });

        it('should collect existing keys so the entry row can reject duplicates', () => {
            expect(spectator.component.$forbiddenkeys()).toEqual({ name: true, password: true });
        });
    });

    describe('empty state (FR-014)', () => {
        beforeEach(() => {
            spectator.setInput('variables', []);
            spectator.detectChanges();
        });

        it('should show the key glyph above the message', () => {
            const icon = spectator.query(byTestId('dot-key-value-empty-icon'));

            expect(icon.className).toContain('material-symbols-outlined');
            expect(icon.textContent.trim()).toBe('key');
        });

        it('should use the existing message key rather than a new one', () => {
            expect(spectator.query(byTestId('no-rows')).textContent).toContain('No Rows');
        });

        it('should keep the entry row usable so the empty state is escapable', () => {
            // An empty state that hides the only control for leaving it is a trap.
            expect(spectator.query(byTestId('key-input'))).toBeTruthy();
            expect(spectator.query(byTestId('value-input'))).toBeTruthy();
            expect(spectator.query(byTestId('save-button'))).toBeTruthy();
        });

        it('should span every column', () => {
            expect(spectator.component.colspan).toBe(4);
        });
    });

    describe('list operations', () => {
        it('should prepend a new pair and report it', () => {
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');
            const newVariable = { key: 'newKey', value: 'newValue', hidden: false };

            spectator.component.saveVariable(newVariable);
            spectator.detectChanges();

            expect(saveSpy).toHaveBeenCalledWith(newVariable);
            expect(listSpy).toHaveBeenCalledWith([newVariable, ...mockKeyValue]);
        });

        it('should replace a pair in place and report both versions', () => {
            const updateSpy = jest.spyOn(spectator.component.update, 'emit');
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');
            const updated = { ...mockKeyValue[0], value: 'changed' };

            spectator.component.updateKeyValue(updated, 0);
            spectator.detectChanges();

            expect(updateSpy).toHaveBeenCalledWith({
                variable: updated,
                oldVariable: mockKeyValue[0]
            });
            expect(listSpy).toHaveBeenCalledWith([updated, mockKeyValue[1]]);
        });

        it('should remove a pair and report it', () => {
            const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');

            spectator.component.deleteVariable(0);
            spectator.detectChanges();

            expect(deleteSpy).toHaveBeenCalledWith(mockKeyValue[0]);
            expect(listSpy).toHaveBeenCalledWith([mockKeyValue[1]]);
        });

        it('should wire the rows so a row-level delete removes that row', () => {
            const deleteSpy = jest.spyOn(spectator.component.delete, 'emit');

            spectator.click(spectator.queryAll(byTestId('dot-key-value-delete-button'))[0]);
            spectator.detectChanges();

            expect(deleteSpy).toHaveBeenCalledWith(mockKeyValue[0]);
        });
    });

    describe('reordering (FR-027, FR-028)', () => {
        beforeEach(() => create());

        /**
         * Reproduces what PrimeNG does before it notifies: `onRowDrop` calls
         * `reorderArray` on the array bound to `[value]` — ours — and only then
         * emits. Simulating the emit alone would test a situation that never
         * happens.
         */
        const primengDropsRow = (from: number, to: number) => {
            const table = spectator.query(Table);
            const [moved] = table.value.splice(from, 1);
            table.value.splice(to, 0, moved);
            table.onRowReorder.emit({ dragIndex: from, dropIndex: to });
            spectator.detectChanges();
        };

        it('should publish the list in the order PrimeNG left it', () => {
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');

            primengDropsRow(1, 0);

            const reordered = [mockKeyValue[1], mockKeyValue[0]];
            expect(spectator.component.$variableList()).toEqual(reordered);
            expect(listSpy).toHaveBeenCalledWith(reordered);
        });

        it('should not apply the move a second time', () => {
            // The bug this guards: recomputing the move from dragIndex/dropIndex
            // on an already-reordered array moves the row twice.
            create({ variables: [...mockKeyValue, { key: 'third', value: '3' }] });
            const third = spectator.component.$variableList()[2];

            primengDropsRow(2, 0);

            expect(spectator.component.$variableList()[0]).toEqual(third);
        });

        it('should produce a new array so the signal actually notifies', () => {
            // An in-place mutation leaves the signal comparing equal to itself.
            const before = spectator.component.$variableList();

            primengDropsRow(1, 0);

            expect(spectator.component.$variableList()).not.toBe(before);
        });
    });

    describe('adding a pasted block', () => {
        it('should keep the block in the order it was pasted', () => {
            // Adding them one at a time would reverse the block, since each new pair
            // goes on top.
            create({ variables: [] });

            spectator.component.saveVariables([
                { key: 'SOME', value: 'TEST' },
                { key: 'JEJE', value: 'JEJE' },
                { key: 'FOO', value: 'BAR' }
            ]);
            spectator.detectChanges();

            expect(
                spectator.queryAll(byTestId('dot-key-value-key')).map((el) => el.textContent.trim())
            ).toEqual(['SOME', 'JEJE', 'FOO']);
        });

        it('should put the block above what was already there', () => {
            create({ variables: [{ key: 'existing', value: 'v' }] });

            spectator.component.saveVariables([{ key: 'pasted', value: 'p' }]);

            expect(spectator.component.$variableList().map(({ key }) => key)).toEqual([
                'pasted',
                'existing'
            ]);
        });

        it('should report each pair through save and the list once', () => {
            // Field Variables persists row by row through `save`, so every pair has to
            // reach it; the other two consumers take the whole array from `updatedList`.
            create({ variables: [] });
            const saveSpy = jest.spyOn(spectator.component.save, 'emit');
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');

            spectator.component.saveVariables([
                { key: 'A', value: '1' },
                { key: 'B', value: '2' }
            ]);

            expect(saveSpy).toHaveBeenCalledTimes(2);
            expect(listSpy).toHaveBeenCalledTimes(1);
            expect(listSpy).toHaveBeenCalledWith([
                { key: 'A', value: '1' },
                { key: 'B', value: '2' }
            ]);
        });

        it('should do nothing for an empty block', () => {
            create({ variables: [{ key: 'a', value: '1' }] });
            const listSpy = jest.spyOn(spectator.component.updatedList, 'emit');

            spectator.component.saveVariables([]);

            expect(listSpy).not.toHaveBeenCalled();
            expect(spectator.component.$variableList()).toHaveLength(1);
        });
    });

    describe('paging long lists', () => {
        const manyPairs = (count: number): DotKeyValue[] =>
            Array.from({ length: count }, (_, i) => ({
                key: `key-${String(i).padStart(3, '0')}`,
                value: `value-${i}`
            }));

        const renderedKeys = () =>
            spectator.queryAll(byTestId('dot-key-value-key')).map((el) => el.textContent.trim());

        it('should render every row when the list fits in one page', () => {
            create({ variables: manyPairs(40) });

            expect(renderedKeys()).toHaveLength(40);
            expect(spectator.query(byTestId('dot-key-value-load-more-row'))).toBeFalsy();
        });

        it('should render only the first page of a longer list', () => {
            create({ variables: manyPairs(95) });

            expect(renderedKeys()).toHaveLength(40);
            expect(renderedKeys()[0]).toBe('key-000');
            expect(spectator.query(byTestId('dot-key-value-load-more')).textContent).toContain(
                'Load more'
            );
        });

        it('should reveal the next page on each click, in order', () => {
            create({ variables: manyPairs(95) });

            spectator.click(byTestId('dot-key-value-load-more'));
            spectator.detectChanges();
            expect(renderedKeys()).toHaveLength(80);
            expect(renderedKeys()[79]).toBe('key-079');

            spectator.click(byTestId('dot-key-value-load-more'));
            spectator.detectChanges();
            expect(renderedKeys()).toHaveLength(95);
        });

        it('should drop the control once nothing is left to reveal', () => {
            create({ variables: manyPairs(50) });

            spectator.click(byTestId('dot-key-value-load-more'));
            spectator.detectChanges();

            expect(renderedKeys()).toHaveLength(50);
            expect(spectator.query(byTestId('dot-key-value-load-more-row'))).toBeFalsy();
        });

        it('should keep the same label whatever is left to reveal', () => {
            // Matches the site/folder selector, which never states a count.
            create({ variables: manyPairs(50) });

            expect(spectator.query(byTestId('dot-key-value-load-more')).textContent).toContain(
                'Load more'
            );
        });

        it('should keep the whole list bound to the table, not just the rendered part', () => {
            // Withheld rows stay in the data: PrimeNG reorders the array it is given,
            // so a shortened one would silently drop a drag.
            create({ variables: manyPairs(95) });

            expect(spectator.component.$variableList()).toHaveLength(95);
        });

        it('should still emit the whole list when a visible row is removed', () => {
            create({ variables: manyPairs(95) });
            const spy = jest.spyOn(spectator.component.updatedList, 'emit');

            spectator.component.deleteVariable(0);

            expect(spy).toHaveBeenCalledWith(expect.any(Array));
            expect(spy.mock.calls[0][0]).toHaveLength(94);
        });

        it('should start again at the first page when the consumer supplies a new list', () => {
            create({ variables: manyPairs(95) });
            spectator.click(byTestId('dot-key-value-load-more'));
            spectator.detectChanges();
            expect(renderedKeys()).toHaveLength(80);

            spectator.setInput('variables', manyPairs(95));
            spectator.detectChanges();

            expect(renderedKeys()).toHaveLength(40);
        });
    });

    describe('hidden values are the only per-consumer capability', () => {
        it('should always render a drag handle per row, in every consumer', () => {
            expect(spectator.queryAll(byTestId('dot-key-value-drag-handle')).length).toBe(
                mockKeyValue.length
            );
        });

        it('should render no visibility affordance when hidden values are off', () => {
            expect(spectator.query(byTestId('dot-key-value-visibility-toggle'))).toBeFalsy();
            expect(spectator.query(byTestId('dot-key-value-new-visibility-toggle'))).toBeFalsy();
        });

        it('should offer the visibility choice only where a pair is created', () => {
            create({ showHiddenField: true });

            // The entry row decides it once; existing rows never revisit it, because
            // the server sends a mask rather than the secret it stands for.
            expect(spectator.query(byTestId('dot-key-value-new-visibility-toggle'))).toBeTruthy();
            expect(spectator.query(byTestId('dot-key-value-visibility-toggle'))).toBeFalsy();
        });
    });
});
