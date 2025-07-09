import { WorkflowService } from './workflow';

import { mockFetch } from '../test-setup';

describe('WorkflowService', () => {
    let service: WorkflowService;

    beforeEach(() => {
        service = new WorkflowService();
    });

    describe('saveContent', () => {
        it('should save content successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        identifier: 'content123',
                        inode: 'inode123',
                        title: 'Test Content',
                        contentType: 'Blog',
                        languageId: '1'
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    pagination: null,
                    permissions: []
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                contentType: 'Blog',
                languageId: '1',
                title: 'Test Content'
            };

            const result = await service.saveContent(params, 'Test comment');

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/workflow/actions/default/fire/NEW', {
                method: 'PUT',
                body: expect.stringContaining('Test comment')
            });
            expect(result.entity.identifier).toBe('content123');
        });

        it('should handle invalid content parameters', async () => {
            const invalidParams = {
                // Missing required contentType and languageId
                title: 'Test'
            };

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            await expect(service.saveContent(invalidParams as any)).rejects.toThrow(
                'Invalid content parameters'
            );
        });
    });

    describe('performContentAction', () => {
        it('should publish content successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: {
                        identifier: 'content123',
                        inode: 'inode123',
                        contentType: 'Blog',
                        languageId: '1'
                    },
                    errors: [],
                    messages: [],
                    i18nMessagesMap: {},
                    pagination: null,
                    permissions: []
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const params = {
                identifier: 'content123',
                variantName: 'DEFAULT',
                action: 'PUBLISH' as const
            };

            const result = await service.performContentAction(params);

            expect(mockFetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/workflow/actions/default/fire/PUBLISH?identifier=content123&variantName=DEFAULT',
                {
                    method: 'PUT',
                    body: expect.stringContaining('Publishing content via API')
                }
            );
            expect(result.entity.identifier).toBe('content123');
        });

        it('should handle invalid action parameters', async () => {
            const invalidParams = {
                // Missing required identifier
                action: 'PUBLISH' as const
            };

            await expect(service.performContentAction(invalidParams as never)).rejects.toThrow(
                'Invalid content action parameters'
            );
        });
    });

    describe('getWorkflowSchemes', () => {
        it('should fetch workflow schemes successfully', async () => {
            const mockResponse = {
                json: jest.fn().mockResolvedValue({
                    entity: [
                        {
                            archived: false,
                            creationDate: 123456789,
                            defaultScheme: true,
                            description: 'Default workflow',
                            entryActionId: null,
                            id: 'scheme123',
                            mandatory: false,
                            modDate: 123456789,
                            name: 'System Workflow',
                            system: true,
                            variableName: 'systemWorkflow'
                        }
                    ],
                    errors: [],
                    i18nMessagesMap: {},
                    messages: [],
                    pagination: null,
                    permissions: []
                })
            };

            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.getWorkflowSchemes();

            expect(mockFetch).toHaveBeenCalledWith('/api/v1/workflow/schemes?showArchived=true', {
                method: 'GET'
            });
            expect(result.entity).toHaveLength(1);
            expect(result.entity[0].name).toBe('System Workflow');
        });

        it('should handle fetch errors', async () => {
            const error = new Error('Network error');
            mockFetch.mockRejectedValue(error);

            await expect(service.getWorkflowSchemes()).rejects.toThrow('Network error');
        });
    });
});
