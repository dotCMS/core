import { DotAssetCreateOptions } from '@dotcms/dotcms-models';

import { DotAssetService } from './dot-asset.service';

// TODO: fix this test, it not work as espected
xdescribe('DotAssetService', () => {
    const fetchMock = jest.fn();
    window.fetch = fetchMock;
    const mockFallbackFun = jest.fn();

    const mockOptions: DotAssetCreateOptions = {
        files: [
            {
                fileName: 'landscape.jpg',
                folder: '',
                id: '123',
                image: true,
                length: 10,
                mimeType: '',
                referenceUrl: '',
                thumbnailUrl: 'string;'
            },
            {
                fileName: 'invoice.pdf',
                folder: '',
                id: '123',
                image: false,
                length: 10,
                mimeType: '',
                referenceUrl: '',
                thumbnailUrl: 'string;'
            }
        ],
        updateCallback: mockFallbackFun,
        url: '/test/url',
        folder: ''
    };

    const assetService = new DotAssetService();

    const mockSuccessResponse = {};
    const mockJsonPromise = Promise.resolve(mockSuccessResponse); // 2
    const mockFetchPromise = Promise.resolve({
        // 3
        json: () => mockJsonPromise,
        headers: null,
        ok: true,
        redirected: true,
        status: 200,
        statusText: '',
        trailer: null,
        type: null,
        url: '',
        clone: null,
        body: null,
        bodyUsed: true,
        arrayBuffer: null,
        blob: null,
        text: null,
        formData: null
    });
    jest.spyOn(window, 'fetch').mockImplementation(() => mockFetchPromise);

    beforeEach(() => {
        fetchMock.mockReset();
        fetchMock.mockRejectedValueOnce({});
    });

    it('test', () => {
        assetService
            .create(mockOptions)
            .then((x) => {
                console.log('fetchMock: ', fetchMock);

                // const params = fetchMock.mock.calls[0];
                //  expect(fetchMock.mock.calls.length).toBe(2);
                // expect(params[0]).toBe('/test/url');
                console.log('error: ', x);
            })
            .catch((x) => {
                expect(window.fetch).toHaveBeenCalledTimes(3);
                //expect(window.fetch).toBeCalledWith(2);

                console.log('error: ', x);
            });

        // process.nextTick(() => { // 6
        //     expect(wrapper.state()).toEqual({
        //         // ... assert the set state
        //     });
        //
        // expect(window.fetch).toHaveBeenCalledTimes(3);
    });
});
