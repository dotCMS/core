import { z } from 'zod';

import {
    type AdapterContext,
    defineAdapter,
    describeAdapterForLLM,
    isDefinedAdapter
} from './define-adapter';
import { ValidationError } from './errors';

const ctx: AdapterContext = {
    request: async (opts) => ({ echoed: opts })
};

describe('defineAdapter', () => {
    const a11y = defineAdapter({
        name: 'a11y',
        methods: {
            scan: {
                description: 'Scan a page URL',
                input: z.object({ url: z.url() }),
                output: z.object({ echoed: z.object({}).loose() }).loose(),
                handler: ({ url }, { request }) =>
                    request({
                        method: 'POST',
                        path: '/api/v1/page-scanner/a11y/check',
                        body: { url }
                    })
            }
        }
    });

    it('produces a plain Adapter usable by the executor', () => {
        expect(isDefinedAdapter(a11y)).toBe(true);
        expect(a11y.name).toBe('a11y');
        expect(a11y.methods.has('scan')).toBe(true);
    });

    it('validates input before the handler runs', async () => {
        const bound = a11y.withContext(ctx);
        const scan = bound.methods.get('scan');
        await expect(scan?.execute({ url: 'not-a-url' })).rejects.toBeInstanceOf(ValidationError);
    });

    it('passes validated input to the handler and validates output', async () => {
        const bound = a11y.withContext(ctx);
        const scan = bound.methods.get('scan');
        const result = await scan?.execute({ url: 'https://demo.dotcms.com/' });
        expect(result).toEqual({
            echoed: {
                method: 'POST',
                path: '/api/v1/page-scanner/a11y/check',
                body: { url: 'https://demo.dotcms.com/' }
            }
        });
    });

    it('throws if used without a bound context', async () => {
        const scan = a11y.methods.get('scan');
        await expect(scan?.execute({ url: 'https://demo.dotcms.com/' })).rejects.toBeInstanceOf(
            ValidationError
        );
    });

    it('marks an adapter with output schemas as model-exposable', () => {
        expect(a11y.modelExposable).toBe(true);
    });

    it('marks an adapter missing an output schema as not model-exposable', () => {
        const internal = defineAdapter({
            name: 'internal',
            methods: {
                plumb: {
                    description: 'internal only',
                    input: z.object({ x: z.number() }),
                    handler: ({ x }) => x * 2
                }
            }
        });
        expect(internal.modelExposable).toBe(false);
        // ...and the LLM description withholds the output-less method.
        expect(describeAdapterForLLM(internal)).toEqual([]);
    });

    it('describes model-facing methods as tool definitions', () => {
        const tools = describeAdapterForLLM(a11y);
        expect(tools).toHaveLength(1);
        expect(tools[0].name).toBe('a11y.scan');
        expect(tools[0].description).toBe('Scan a page URL');
        expect(tools[0].inputSchema).toBeDefined();
        expect(tools[0].outputSchema).toBeDefined();
    });
});
