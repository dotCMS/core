import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/vitest';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';

import { DotContentTypeService, DotFieldService } from '@dotcms/data-access';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { RelationshipFieldService } from './relationship-field.service';

/**
 * `relationships` is required on the relationship arm but arrives as raw HTTP JSON, so this
 * service is the edge that establishes it. Everything downstream — the store, the dialog — reads
 * the descriptor published here instead of the field, which is what lets those reads drop the
 * optional chaining they used to carry.
 */
describe('RelationshipFieldService', () => {
    let spectator: SpectatorService<RelationshipFieldService>;
    let service: RelationshipFieldService;

    const contentlet = createFakeContentlet({
        identifier: 'parent-1',
        relationship_field: []
    });

    const createService = createServiceFactory({
        service: RelationshipFieldService,
        providers: [
            mockProvider(DotContentTypeService, {
                getContentType: () => of({ id: 'child-ct', name: 'Child', metadata: {} })
            }),
            mockProvider(DotFieldService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    describe('prepareField', () => {
        it('publishes the cardinality and parent role the field carries', async () => {
            const field = createFakeRelationshipField({
                variable: 'relationship_field',
                relationships: {
                    cardinality: 0,
                    isParentField: false,
                    velocityVar: 'child-ct.relationship_field'
                }
            });

            const result = await firstValueFrom(service.prepareField({ field, contentlet }));

            expect(result.relationships).toEqual({ cardinality: 0, isParentField: false });
        });

        it('defaults isParentField to true when the server omits it', async () => {
            const field = createFakeRelationshipField({
                variable: 'relationship_field',
                relationships: {
                    cardinality: 1,
                    velocityVar: 'child-ct.relationship_field'
                } as never
            });

            const result = await firstValueFrom(service.prepareField({ field, contentlet }));

            // The parent side is what the editor assumed before the descriptor existed, so a
            // payload without the flag keeps behaving exactly as it always has.
            expect(result.relationships).toEqual({ cardinality: 1, isParentField: true });
        });

        it('rejects a field whose relationships the server left out', async () => {
            const field = createFakeRelationshipField({
                variable: 'relationship_field',
                relationships: undefined
            });

            // The store surfaces this as an error status, so such a field never reaches a loaded
            // state and the dialog that reads the descriptor cannot open for it.
            await expect(
                firstValueFrom(service.prepareField({ field, contentlet }))
            ).rejects.toThrow('Invalid field: missing cardinality');
        });

        it('derives the selection mode from the descriptor', async () => {
            // Cardinality 1 is MANY_TO_MANY, the only relationship a child side may multi-select.
            const field = createFakeRelationshipField({
                variable: 'relationship_field',
                relationships: {
                    cardinality: 1,
                    isParentField: false,
                    velocityVar: 'child-ct.relationship_field'
                }
            });

            const result = await firstValueFrom(service.prepareField({ field, contentlet }));

            expect(result.selectionMode).toBe('multiple');
        });

        it('keeps the child side single for anything other than many-to-many', async () => {
            // Cardinality 0 is ONE_TO_MANY: one parent, many children, so a child picks one.
            const field = createFakeRelationshipField({
                variable: 'relationship_field',
                relationships: {
                    cardinality: 0,
                    isParentField: false,
                    velocityVar: 'child-ct.relationship_field'
                }
            });

            const result = await firstValueFrom(service.prepareField({ field, contentlet }));

            expect(result.selectionMode).toBe('single');
        });
    });
});
