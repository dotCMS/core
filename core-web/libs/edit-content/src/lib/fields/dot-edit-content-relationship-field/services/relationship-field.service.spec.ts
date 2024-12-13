import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotFieldService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { RelationshipFieldService } from './relationship-field.service';

import { MANDATORY_FIRST_COLUMNS, MANDATORY_LAST_COLUMNS } from '../dot-edit-content-relationship-field.constants';

describe('RelationshipFieldService', () => {
    let spectator: SpectatorService<RelationshipFieldService>;
    let dotFieldService: SpyObject<DotFieldService>;

    const createService = createServiceFactory({
        service: RelationshipFieldService,
        providers: [
            mockProvider(DotFieldService),
            {
                useValue: {
                    getFields: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotFieldService = spectator.inject(DotFieldService);
    });

    describe('getColumns', () => {
        it('should map fields to columns', (done) => {
            const mockFields = [
                {
                    variable: 'title',
                    name: 'Title',
                    sortOrder: 1
                },
                {
                    variable: 'description',
                    name: 'Description',
                    sortOrder: 2
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                ...mockFields.map((field) => ({ field: field.variable, header: field.name })),
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });

        it('should return empty array when no fields are returned', (done) => {
            dotFieldService.getFields.mockReturnValue(of([]));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });

        it('should handle fields with missing properties', (done) => {
            const mockFields = [
                {
                    variable: 'title',
                    name: 'Title'
                },
                {
                    variable: 'description' // missing name
                }
            ] as DotCMSContentTypeField[];

            dotFieldService.getFields.mockReturnValue(of(mockFields));

            const contentTypeId = '123';

            const expectedColumns = [
                ...MANDATORY_FIRST_COLUMNS.map((field) => ({ field, header: field })),
                ...mockFields.map((field) => ({ field: field.variable, header: field.name })),
                ...MANDATORY_LAST_COLUMNS.map((field) => ({ field, header: field }))
            ];

            spectator.service.getColumns(contentTypeId).subscribe((columns) => {
                expect(dotFieldService.getFields).toHaveBeenCalledWith(
                    contentTypeId,
                    'SHOW_IN_LIST'
                );
                expect(columns).toEqual(expectedColumns);
                done();
            });
        });
    });
});
