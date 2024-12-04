import { MOCK_WORKFLOW_DATA } from './edit-content.mock';
import { parseWorkflows } from './workflows.utils';

describe('Workflow Utils', () => {
    describe('parseWorkflows', () => {
        it('should return empty object when input is not an array', () => {
            expect(parseWorkflows(null)).toEqual({});
            expect(parseWorkflows(undefined)).toEqual({});
        });

        it('should parse real workflow data correctly', () => {
            const expected = {
                [MOCK_WORKFLOW_DATA[0].scheme.id]: {
                    actions: [{ ...MOCK_WORKFLOW_DATA[0].action }],
                    firstStep: { ...MOCK_WORKFLOW_DATA[0].firstStep },
                    scheme: { ...MOCK_WORKFLOW_DATA[0].scheme }
                },
                [MOCK_WORKFLOW_DATA[1].scheme.id]: {
                    actions: [{ ...MOCK_WORKFLOW_DATA[1].action }],
                    firstStep: { ...MOCK_WORKFLOW_DATA[1].firstStep },
                    scheme: { ...MOCK_WORKFLOW_DATA[1].scheme }
                }
            };

            expect(parseWorkflows(MOCK_WORKFLOW_DATA)).toEqual(expected);
        });
        it('should handle multiple schemes correctly', () => {
            const result = parseWorkflows(MOCK_WORKFLOW_DATA);

            expect(Object.keys(result)).toHaveLength(2);
            expect(result[MOCK_WORKFLOW_DATA[0].scheme.id].scheme.name).toBe('System Workflow');
            expect(result[MOCK_WORKFLOW_DATA[1].scheme.id].scheme.name).toBe('Blogs');
        });

        it('should handle empty array input', () => {
            expect(parseWorkflows([])).toEqual({});
        });
    });

    describe('parseWorkflows', () => {
        it('should return empty object when input is not an array', () => {
            expect(parseWorkflows(null)).toEqual({});
            expect(parseWorkflows(undefined)).toEqual({});
        });

        it('should correctly parse workflow data', () => {
            const result = parseWorkflows(MOCK_WORKFLOW_DATA);

            // Check structure for System Workflow
            expect(result['d61a59e1-a49c-46f2-a929-db2b4bfa88b2']).toBeDefined();
            expect(result['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'].scheme.name).toBe(
                'System Workflow'
            );
            expect(result['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'].actions).toHaveLength(1);
            expect(result['d61a59e1-a49c-46f2-a929-db2b4bfa88b2'].firstStep.name).toBe('New');

            // Check structure for Blogs workflow
            expect(result['2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd']).toBeDefined();
            expect(result['2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'].scheme.name).toBe('Blogs');
            expect(result['2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'].actions).toHaveLength(1);
            expect(result['2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'].firstStep.name).toBe('Edit');
        });
    });
});
