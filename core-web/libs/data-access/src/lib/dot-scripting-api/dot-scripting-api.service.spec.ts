import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DotScriptingApiService } from './dot-scripting-api.service';

const baseUrl = '/api/vtl';

describe('DotScriptingApiService', () => {
    let spectator: SpectatorHttp<DotScriptingApiService>;
    const createHttp = createHttpFactory(DotScriptingApiService);

    beforeEach(() => (spectator = createHttp()));

    it('should retrieve data from script API given a `path`', (done) => {
        const path = `some/path`;
        const data = { data: 'some data' };
        spectator.service.get(path).subscribe((res) => {
            expect(res).toEqual(data);
            done();
        });

        const req = spectator.expectOne(`${baseUrl}/${path}`, HttpMethod.GET);
        req.flush(data);
    });
});
