import { describe, expect, it } from '@jest/globals';

import {
    getRemainingSpaceForBox,
    parseFromDotObjectToGridStack,
    parseFromGridStackToDotObject,
    willBoxFitInRow,
    EMPTY_ROWS_VALUE
} from './gridstack-utils';
import { FULL_DATA_MOCK_UNSORTED, MINIMAL_DATA_MOCK, ROWS_MOCK } from './mocks';

global.structuredClone = jest.fn((val) => {
    return JSON.parse(JSON.stringify(val));
});

describe('parseFromDotObjectToGridStack', () => {
    it('should parse the backend object to gridStack', () => {
        const data = MINIMAL_DATA_MOCK;

        const result = parseFromDotObjectToGridStack(data);

        expect(result).toHaveLength(data.rows.length);

        // Test the properties of each gridStack widget
        result.forEach((widget, i) => {
            expect(widget.w).toBe(12);
            expect(widget.h).toBe(1);
            expect(widget.x).toBe(0);
            expect(widget.y).toBe(i);
            expect(widget.id).toBeDefined();
            expect(Array.isArray(widget.styleClass)).toBe(true);

            expect(widget.subGridOpts?.children).toHaveLength(data.rows[i].columns.length);

            // Test the properties of each sub-grid widget
            widget.subGridOpts?.children.forEach((subWidget, j) => {
                const column = data.rows[i].columns[j];
                expect(subWidget.w).toBe(column.width);
                expect(subWidget.h).toBe(1);
                expect(subWidget.y).toBe(0);
                expect(subWidget.x).toBe(column.leftOffset - 1);
                expect(subWidget.id).toBeDefined();
                expect(Array.isArray(subWidget.styleClass)).toBe(true);
                expect(subWidget.containers).toEqual(column.containers);
            });
        });
    });

    it('should return a row with one container when body is undefined', () => {
        const result = parseFromDotObjectToGridStack(undefined);

        expect(result).toEqual(EMPTY_ROWS_VALUE);
    });

    it('should return a row with one container when rows is empty', () => {
        const result = parseFromDotObjectToGridStack({ rows: [] });

        expect(result).toEqual(EMPTY_ROWS_VALUE);
    });
});

describe('parseFromGridStackToDotObject', () => {
    it('should parse the gridStack object to dot backend object', () => {
        const data = MINIMAL_DATA_MOCK;

        const gridstack = parseFromDotObjectToGridStack(data);
        const result = parseFromGridStackToDotObject(gridstack);

        expect(result).toEqual(data);
    });

    it('should sort all colums by leftOffset', () => {
        const data = FULL_DATA_MOCK_UNSORTED;

        const gridstack = parseFromDotObjectToGridStack(data);
        const result = parseFromGridStackToDotObject(gridstack);

        let sorted = true;

        result.rows.forEach(({ columns }) => {
            if (columns.length <= 1) return;

            columns.reduce((prev, curr) => {
                if (prev.leftOffset > curr.leftOffset) {
                    sorted = false;
                }

                return curr;
            });
        });

        expect(sorted).toBeTruthy();
    });

    it('should return an empty DotBodyLayour when no rows are provided', () => {
        const result = parseFromGridStackToDotObject([]);

        expect(result).toEqual({ rows: [] });
    });
});

describe('willBoxFitInRow', () => {
    it('should fit when theres only a box with width 7', () => {
        expect(willBoxFitInRow(ROWS_MOCK[0].subGridOpts.children)).toBe(true);
    });

    it('should fit with 2 boxes and a gap of 2 between them', () => {
        expect(willBoxFitInRow(ROWS_MOCK[1].subGridOpts.children)).toBe(true);
    });

    it('should not fit when the row is full', () => {
        expect(willBoxFitInRow(ROWS_MOCK[2].subGridOpts.children)).toBe(false);
    });

    it('should fit with multiple boxes and a gap of 1 between them', () => {
        expect(willBoxFitInRow(ROWS_MOCK[3].subGridOpts.children)).toBe(true);
    });

    it('should fit when empty', () => {
        expect(willBoxFitInRow(ROWS_MOCK[4].subGridOpts.children)).toBe(true);
    });
});
describe('getRemainingSpaceForBox', () => {
    it('should return 2 empty spaces when theres only a box with width 7', () => {
        expect(
            getRemainingSpaceForBox(ROWS_MOCK[0].subGridOpts.children, {
                w: 1,
                x: 7
            })
        ).toBe(2);
    });

    it('should return 1 empty spaces with 2 boxes and a gap of 2 between them', () => {
        expect(
            getRemainingSpaceForBox(ROWS_MOCK[1].subGridOpts.children, {
                w: 1,
                x: 7
            })
        ).toBe(1);
    });
    it('should return 0 empty spaces when theres no more space', () => {
        expect(
            getRemainingSpaceForBox(ROWS_MOCK[3].subGridOpts.children, {
                w: 1,
                x: 7
            })
        ).toBe(0);
    });
});
