import { describe, expect, it } from '@jest/globals';

import { parseFromDotObjectToGridStack, parseFromGridStackToDotObject } from './gridstack-utils';
import { MINIMAL_DATA_MOCK } from './mocks';

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

    it('should return an empty array when no rows are provided', () => {
        const result = parseFromDotObjectToGridStack(undefined);

        expect(result).toEqual([]);
    });
});

describe('parseFromGridStackToDotObject', () => {
    it('should parse the gridStack object to dot backend object', () => {
        const data = MINIMAL_DATA_MOCK;

        const gridstack = parseFromDotObjectToGridStack(data);
        const result = parseFromGridStackToDotObject(gridstack);

        expect(result).toEqual(data);
    });

    it('should return an empty DotBodyLayour when no rows are provided', () => {
        const result = parseFromGridStackToDotObject([]);

        expect(result).toEqual({ rows: [] });
    });
});
