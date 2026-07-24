import { matchesPattern, transformSpec } from './spec-transform';

/**
 * A compact raw OpenAPI doc exercising the transform's branches: prefix filtering, exclusion
 * patterns, deprecated-op dropping, targeted multipart replacement (Jersey vs curated), the
 * transitive `$ref` walk (incl. an `allOf` hop and a cycle), and dangling-ref handling.
 */
function makeRawSpec(): Record<string, unknown> {
    return {
        openapi: '3.0.1',
        info: { title: 'test', version: '1' },
        servers: [{ url: 'https://demo.dotcms.com' }],
        paths: {
            // allowed; references ContentType (which references Field via allOf)
            '/api/v1/contenttype': {
                get: {
                    summary: 'list',
                    responses: {
                        '200': {
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/ContentType' }
                                }
                            }
                        }
                    }
                },
                // deprecated op — should be dropped
                post: {
                    deprecated: true,
                    responses: {
                        '200': {
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/Dropped' }
                                }
                            }
                        }
                    }
                }
            },
            // allowed; Jersey multipart (must be replaced) + a dangling ref in responses
            '/api/v2/tags/import': {
                post: {
                    requestBody: {
                        content: {
                            'multipart/form-data': {
                                schema: { $ref: '#/components/schemas/FormDataMultiPart' }
                            }
                        }
                    },
                    responses: {
                        '200': {
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/Missing' }
                                }
                            }
                        }
                    }
                }
            },
            // allowed; curated multipart (must be kept)
            '/api/v1/workflow/actions/firemultipart': {
                put: {
                    requestBody: {
                        content: {
                            'multipart/form-data': {
                                schema: {
                                    $ref: '#/components/schemas/WorkflowActionMultipartSchema'
                                }
                            }
                        }
                    }
                }
            },
            // allowed prefix but excluded pattern — should be dropped entirely
            '/api/v1/containers/live': {
                get: { summary: 'excluded' }
            },
            // not in ALLOWED_PREFIXES — should be dropped entirely
            '/api/v1/secretstuff': {
                get: {
                    responses: {
                        '200': {
                            content: {
                                'application/json': {
                                    schema: { $ref: '#/components/schemas/NeverReferenced' }
                                }
                            }
                        }
                    }
                }
            }
        },
        components: {
            schemas: {
                ContentType: { type: 'object', allOf: [{ $ref: '#/components/schemas/Field' }] },
                Field: {
                    type: 'object',
                    properties: {
                        // self-referential cycle — the walk must terminate
                        child: { $ref: '#/components/schemas/Field' }
                    }
                },
                WorkflowActionMultipartSchema: {
                    type: 'object',
                    properties: { comments: { type: 'string' } }
                },
                FormDataMultiPart: {
                    type: 'object',
                    properties: {
                        bodyParts: { type: 'array' },
                        messageBodyWorkers: { type: 'object' }
                    }
                },
                Dropped: { type: 'object' },
                NeverReferenced: { type: 'object' }
            }
        }
    };
}

describe('matchesPattern', () => {
    it('matches {param} and * against a single segment', () => {
        expect(matchesPattern('/api/v1/site/abc', '/api/v1/site/{id}')).toBe(true);
        expect(matchesPattern('/api/v1/site/abc/def', '/api/v1/site/{id}')).toBe(false);
    });

    it('matches ** across multiple segments', () => {
        expect(matchesPattern('/api/v1/workflow/tasks/a/b', '/api/v1/workflow/tasks/**')).toBe(true);
    });

    it('is anchored at both ends', () => {
        expect(matchesPattern('/api/v1/site/switch/extra', '/api/v1/site/switch')).toBe(false);
    });
});

describe('transformSpec', () => {
    it('keeps allowed paths and drops excluded / non-allowed ones', () => {
        const { spec, stats } = transformSpec(makeRawSpec());
        const paths = spec.paths as Record<string, unknown>;
        expect(Object.keys(paths).sort()).toEqual([
            '/api/v1/contenttype',
            '/api/v1/workflow/actions/firemultipart',
            '/api/v2/tags/import'
        ]);
        expect(stats.pathCount).toBe(3);
    });

    it('keeps templates/{id}/working but still excludes templates/{id}/live', () => {
        // Authoring needs the working-layout endpoint; the live variant stays out (edit working,
        // publish separately). Guards the C1 inclusion decision against a regression.
        const raw = {
            openapi: '3.0.1',
            info: { title: 't', version: '1' },
            paths: {
                '/api/v1/templates/{templateId}/working': { get: { summary: 'working' } },
                '/api/v1/templates/{templateId}/live': { get: { summary: 'live' } }
            },
            components: { schemas: {} }
        };
        const { spec } = transformSpec(raw);
        const paths = spec.paths as Record<string, unknown>;
        expect(paths['/api/v1/templates/{templateId}/working']).toBeDefined();
        expect(paths['/api/v1/templates/{templateId}/live']).toBeUndefined();
    });

    it('drops deprecated operations', () => {
        const { spec } = transformSpec(makeRawSpec());
        const contentType = (spec.paths as Record<string, Record<string, unknown>>)[
            '/api/v1/contenttype'
        ];
        expect(contentType.get).toBeDefined();
        expect(contentType.post).toBeUndefined();
    });

    it('replaces Jersey multipart with the placeholder but keeps curated multipart', () => {
        const { spec } = transformSpec(makeRawSpec());
        const paths = spec.paths as Record<string, Record<string, Record<string, unknown>>>;

        const jersey = (
            (paths['/api/v2/tags/import'].post.requestBody as Record<string, unknown>)
                .content as Record<string, Record<string, unknown>>
        )['multipart/form-data'].schema as Record<string, unknown>;
        expect(jersey.$ref).toBeUndefined();
        expect((jersey.properties as Record<string, unknown>).file).toBeDefined();

        const curated = (
            (paths['/api/v1/workflow/actions/firemultipart'].put.requestBody as Record<
                string,
                unknown
            >).content as Record<string, Record<string, unknown>>
        )['multipart/form-data'].schema as Record<string, unknown>;
        expect(curated.$ref).toBe('#/components/schemas/WorkflowActionMultipartSchema');
    });

    it('prunes components to transitively-referenced schemas only, sorted', () => {
        const { spec, stats } = transformSpec(makeRawSpec());
        const schemas = (spec.components as Record<string, Record<string, unknown>>).schemas;
        const names = Object.keys(schemas);

        // ContentType → Field (allOf hop, self-cycle) and the curated multipart schema.
        expect(names).toEqual(['ContentType', 'Field', 'WorkflowActionMultipartSchema']);
        // Jersey noise, the deprecated op's schema, and unreferenced schemas are excluded.
        expect(schemas.FormDataMultiPart).toBeUndefined();
        expect(schemas.Dropped).toBeUndefined();
        expect(schemas.NeverReferenced).toBeUndefined();
        // sorted for deterministic output
        expect(names).toEqual([...names].sort());
        expect(stats.schemaCount).toBe(3);
    });

    it('reports dangling refs and leaves them in place', () => {
        const { spec, stats } = transformSpec(makeRawSpec());
        expect(stats.danglingRefs).toEqual(['Missing']);
        const paths = spec.paths as Record<string, Record<string, Record<string, unknown>>>;
        const respSchema = (
            (
                (paths['/api/v2/tags/import'].post.responses as Record<string, Record<string, unknown>>)
                    ['200'].content as Record<string, Record<string, unknown>>
            )['application/json'].schema
        ) as Record<string, unknown>;
        expect(respSchema.$ref).toBe('#/components/schemas/Missing');
    });

    it('preserves top-level metadata (openapi, info, servers)', () => {
        const { spec } = transformSpec(makeRawSpec());
        expect(spec.openapi).toBe('3.0.1');
        expect(spec.info).toEqual({ title: 'test', version: '1' });
        expect(spec.servers).toEqual([{ url: 'https://demo.dotcms.com' }]);
    });
});
