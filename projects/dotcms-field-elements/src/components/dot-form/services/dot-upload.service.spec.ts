import { DotUploadService } from './dot-upload.service';

describe('DotUploadService', () => {
    function FormDataMock() {
        this.append = jest.fn();
    }

    const globalAny: any = global;
    globalAny.FormData = FormDataMock;

    const fetchMock = jest.fn();
    window.fetch = fetchMock;

    const uploadService = new DotUploadService();

    beforeEach(() => {
        fetchMock.mockReset();
        fetchMock.mockRejectedValueOnce({});
    });

    it('should send data to the URL endpoint with the correct information', () => {
        uploadService.uploadFile('test');
        const params = fetchMock.mock.calls[0];

        expect(fetchMock.mock.calls.length).toBe(1);
        expect(params[0]).toBe('/api/v1/temp/byUrl');
    });

    it('should send data to the binary file endpoint with the correct information', () => {
        uploadService.uploadFile({} as File);
        const params = fetchMock.mock.calls[0];

        expect(fetchMock.mock.calls.length).toBe(1);
        expect(params[0]).toBe('/api/v1/temp');
    });
});
