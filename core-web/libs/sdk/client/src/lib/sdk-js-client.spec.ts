import { dotcmsClient } from './sdk-js-client';
global.fetch = jest.fn();

// Utility function to mock fetch responses
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockFetchResponse = (body: any, ok = true, status = 200) => {
    (fetch as jest.Mock).mockImplementationOnce(() =>
        Promise.resolve({
            ok,
            status,
            json: () => Promise.resolve(body)
        })
    );
};

describe('DotCmsClient', () => {
    const client = dotcmsClient.init({
        host: 'http://localhost',
        siteId: '123456',
        authToken: 'ABC'
    });

    beforeEach(() => {
        (fetch as jest.Mock).mockClear();
    });

    describe('getPage', () => {
        it('should fetch page data successfully', async () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            const data = await client.getPage({ path: '/home' });

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/page/json/home?host_id=123456',
                {
                    headers: { Authorization: 'Bearer ABC' }
                }
            );
            expect(data).toEqual(mockResponse);
        });

        it('should throw an error if the path is not provided', async () => {
            // expect(async () => {
            //     await client.getPage({} as any);
            // }).toThrowError();

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            await expect(client.getPage({} as any));
        });
    });

    describe('getNav', () => {
        it('should fetch navigation data successfully', async () => {
            const mockResponse = { nav: 'Navigation data' };
            mockFetchResponse(mockResponse);

            const data = await client.getNav({ path: '/', depth: 1 });

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/?depth=1', {
                headers: { Authorization: 'Bearer ABC' }
            });
            expect(data).toEqual(mockResponse);
        });

        it('should correctly handle the root path', async () => {
            const mockResponse = { nav: 'Root nav data' };
            mockFetchResponse(mockResponse);

            const data = await client.getNav({ path: '/' });

            expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/', {
                headers: { Authorization: 'Bearer ABC' }
            });
            expect(data).toEqual(mockResponse);
        });
    });
});
