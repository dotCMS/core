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

    it('should send data to the binary file endpoint without max file size', () => {
        uploadService.uploadFile({} as File);
        const params = fetchMock.mock.calls[0];

        expect(fetchMock.mock.calls.length).toBe(1);
        expect(params[0]).toBe('/api/v1/temp');
    });

    it('should send data to the binary file endpoint with max file size', () => {
        uploadService.uploadFile({} as File, '1000');
        const params = fetchMock.mock.calls[0];

        expect(fetchMock.mock.calls.length).toBe(1);
        expect(params[0]).toBe('/api/v1/temp?maxFileLength=1000');
    });
});
