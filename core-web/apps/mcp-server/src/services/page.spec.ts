import { PageService } from './page';

import { mockFetch } from '../test-setup';

describe('PageService', () => {
    let service: PageService;

    beforeEach(() => {
        service = new PageService();
    });

    describe('renderHtml', () => {
        it('should render HTML successfully for URI with leading slash', async () => {
            const mockResponse = {
                text: jest.fn().mockResolvedValue('<html><body>About</body></html>'),
                headers: {
                    get: jest.fn().mockReturnValue('text/html; charset=UTF-8')
                }
            };
            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.renderHtml({ uri: '/about-us' });

            expect(mockFetch).toHaveBeenCalledWith(
                '/api/v1/page/renderHTML/about-us',
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        Accept: expect.stringContaining('text/html')
                    })
                })
            );
            expect(result.html).toContain('<body>About</body>');
            expect(result.contentType).toContain('text/html');
        });

        it('should render HTML successfully for URI without leading slash', async () => {
            const mockResponse = {
                text: jest.fn().mockResolvedValue('<html><body>Home</body></html>'),
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                }
            };
            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.renderHtml({ uri: 'index' });

            expect(mockFetch).toHaveBeenCalledWith(
                '/api/v1/page/renderHTML/index',
                expect.objectContaining({
                    method: 'GET'
                })
            );
            expect(result.html).toContain('Home');
            expect(result.contentType).toBe('text/html');
        });

        it('should encode URI path segments while preserving slashes', async () => {
            const mockResponse = {
                text: jest.fn().mockResolvedValue('<html><body>Encoded</body></html>'),
                headers: {
                    get: jest.fn().mockReturnValue('text/html')
                }
            };
            mockFetch.mockResolvedValue(mockResponse);

            await service.renderHtml({ uri: 'folder/page with space' });

            expect(mockFetch).toHaveBeenCalledWith(
                '/api/v1/page/renderHTML/folder/page%20with%20space',
                expect.any(Object)
            );
        });

        it('should default contentType to text/html if header missing', async () => {
            const mockResponse = {
                text: jest.fn().mockResolvedValue('<html><body>NoCT</body></html>'),
                headers: {
                    get: jest.fn().mockReturnValue(null)
                }
            };
            mockFetch.mockResolvedValue(mockResponse);

            const result = await service.renderHtml({ uri: '/no-ct' });
            expect(result.contentType).toBe('text/html');
        });

        it('should validate parameters and throw on invalid input', async () => {
            await expect(service.renderHtml({ uri: '' })).rejects.toThrow(
                'Invalid page render parameters'
            );
        });

        it('should propagate fetch errors', async () => {
            const error = new Error('Network error');
            mockFetch.mockRejectedValue(error);
            await expect(service.renderHtml({ uri: '/error' })).rejects.toThrow('Network error');
        });
    });
});


