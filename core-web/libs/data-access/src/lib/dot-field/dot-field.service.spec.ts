import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotFieldService } from './dot-field.service';

describe('DotFieldService', () => {
    let spectator: SpectatorHttp<DotFieldService>;
    const createHttp = createHttpFactory(DotFieldService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('getFields', () => {
        const mockFields: DotCMSContentTypeField[] = [
            {
                fieldType: 'Text',
                name: 'title',
                required: true
            } as DotCMSContentTypeField
        ];

        it('should get all fields without filter', () => {
            spectator.service.getFields('Region').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields',
                HttpMethod.GET
            );
            expect(req.request.params.toString()).toEqual('');

            req.flush(mockFields);
        });

        it('should get fields with REQUIRED filter', () => {
            spectator.service.getFields('Region', 'REQUIRED').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields?filter=REQUIRED',
                HttpMethod.GET
            );
            expect(req.request.params.get('filter')).toBe('REQUIRED');

            req.flush(mockFields);
        });

        it('should get fields with SHOW_IN_LIST filter', () => {
            spectator.service.getFields('Region', 'SHOW_IN_LIST').subscribe();

            const req = spectator.expectOne(
                '/api/v3/contenttype/Region/fields/allfields?filter=SHOW_IN_LIST',
                HttpMethod.GET
            );
            expect(req.request.params.get('filter')).toBe('SHOW_IN_LIST');

            req.flush(mockFields);
        });

        it('should handle error response', () => {
            const errorResponse = { status: 404, statusText: 'Not Found' };

            spectator.service.getFields('InvalidType').subscribe({
                error: (error) => {
                    expect(error.status).toBe(404);
                }
            });

            const req = spectator.expectOne(
                '/api/v3/contenttype/InvalidType/fields/allfields',
                HttpMethod.GET
            );

            req.flush('Not Found', errorResponse);
        });
    });
});
