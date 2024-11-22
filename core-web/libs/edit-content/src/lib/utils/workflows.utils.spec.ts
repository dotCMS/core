import { MOCK_CONTENTLET_1_TAB, MOCK_WORKFLOW_DATA } from './edit-content.mock';
import { getWorkflowActions, parseWorkflows, shouldShowWorkflowWarning } from './workflows.utils';

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

    describe('shouldShowWorkflowWarning', () => {
        const mockSchemes = parseWorkflows(MOCK_WORKFLOW_DATA);

        it('should return true when content is new, has multiple schemes and no scheme selected', () => {
            const result = shouldShowWorkflowWarning({
                schemes: mockSchemes,
                contentlet: null,
                currentSchemeId: null
            });
            expect(result).toBe(true);
        });

        it('should return false when content exists', () => {
            const result = shouldShowWorkflowWarning({
                schemes: mockSchemes,
                contentlet: MOCK_CONTENTLET_1_TAB,
                currentSchemeId: null
            });
            expect(result).toBe(false);
        });

        it('should return false when only one scheme exists', () => {
            const singleScheme = {
                'd61a59e1-a49c-46f2-a929-db2b4bfa88b2':
                    mockSchemes['d61a59e1-a49c-46f2-a929-db2b4bfa88b2']
            };
            const result = shouldShowWorkflowWarning({
                schemes: singleScheme,
                contentlet: null,
                currentSchemeId: null
            });
            expect(result).toBe(false);
        });

        it('should return false when scheme is selected', () => {
            const result = shouldShowWorkflowWarning({
                schemes: mockSchemes,
                contentlet: null,
                currentSchemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
            });
            expect(result).toBe(false);
        });
    });

    describe('getWorkflowActions', () => {
        const mockSchemes = parseWorkflows(MOCK_WORKFLOW_DATA);

        it('should return empty array when no scheme is selected', () => {
            const result = getWorkflowActions({
                schemes: mockSchemes,
                contentlet: null,
                currentSchemeId: null,
                currentContentActions: []
            });
            expect(result).toEqual([]);
        });

        it('should return empty array when selected scheme does not exist', () => {
            const result = getWorkflowActions({
                schemes: mockSchemes,
                contentlet: null,
                currentSchemeId: 'non-existent-scheme',
                currentContentActions: []
            });
            expect(result).toEqual([]);
        });

        it('should return current content actions for existing content', () => {
            const currentActions = [MOCK_WORKFLOW_DATA[0].action];
            const result = getWorkflowActions({
                schemes: mockSchemes,
                contentlet: MOCK_CONTENTLET_1_TAB,
                currentSchemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                currentContentActions: currentActions
            });
            expect(result).toEqual(currentActions);
        });

        it('should return sorted scheme actions for new content with Save first', () => {
            const result = getWorkflowActions({
                schemes: mockSchemes,
                contentlet: null,
                currentSchemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                currentContentActions: []
            });

            expect(result.length).toBeGreaterThan(0);
            expect(result[0].name).toBe('Save');
        });

        it('should return scheme actions when content exists but no current actions', () => {
            const result = getWorkflowActions({
                schemes: mockSchemes,
                contentlet: MOCK_CONTENTLET_1_TAB,
                currentSchemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                currentContentActions: []
            });

            expect(result.length).toBeGreaterThan(0);
            expect(result[0].name).toBe('Save');
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
