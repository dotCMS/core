import { describe, expect, it } from 'vitest';

import { columnCountFromTops, nextGridIndex } from './grid-navigation';

describe('grid navigation (#37930, US7)', () => {
    describe('columnCountFromTops', () => {
        it('should count the cards that share the first row', () => {
            expect(columnCountFromTops([0, 0, 0, 300, 300, 600])).toBe(3);
        });

        it('should count every card when they all fit in one row', () => {
            expect(columnCountFromTops([10, 10])).toBe(2);
        });

        it('should never answer less than one column', () => {
            expect(columnCountFromTops([])).toBe(1);
        });
    });

    describe('nextGridIndex', () => {
        // Four columns, ten cards: rows are 0-3, 4-7, 8-9.
        const move = (index: number, key: string, skip: number[] = []) =>
            nextGridIndex({
                index,
                key,
                columns: 4,
                count: 10,
                isSkipped: (i) => skip.includes(i)
            });

        it('should move one card right and left in reading order, wrapping rows (US7 #2)', () => {
            expect(move(0, 'ArrowRight')).toBe(1);
            expect(move(3, 'ArrowRight')).toBe(4);
            expect(move(4, 'ArrowLeft')).toBe(3);
        });

        it('should move a whole row up and down (US7 #3)', () => {
            expect(move(1, 'ArrowDown')).toBe(5);
            expect(move(5, 'ArrowUp')).toBe(1);
        });

        it('should land on the last card when the row below is shorter (US7 #3)', () => {
            expect(move(6, 'ArrowDown')).toBe(9);
        });

        it('should stay put at the edges of the page (US7 #4)', () => {
            expect(move(0, 'ArrowLeft')).toBe(0);
            expect(move(0, 'ArrowUp')).toBe(0);
            expect(move(9, 'ArrowRight')).toBe(9);
            expect(move(9, 'ArrowDown')).toBe(9);
            expect(move(8, 'ArrowDown')).toBe(8);
        });

        it('should go to the first and last card with Home and End', () => {
            expect(move(5, 'Home')).toBe(0);
            expect(move(5, 'End')).toBe(9);
        });

        it('should skip unselectable cards (US7 #7)', () => {
            expect(move(0, 'ArrowRight', [1, 2])).toBe(3);
            expect(move(1, 'ArrowDown', [5])).toBe(6);
            expect(move(0, 'Home', [0])).toBe(1);
            expect(move(0, 'End', [9])).toBe(8);
        });

        it('should stay put when every card that way is unselectable', () => {
            expect(move(8, 'ArrowRight', [9])).toBe(8);
        });

        it('should ignore keys it does not handle', () => {
            expect(move(5, 'Tab')).toBe(5);
        });
    });
});
