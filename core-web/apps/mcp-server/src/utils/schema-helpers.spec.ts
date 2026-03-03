import { z } from 'zod';

import { asMcpSchema } from './schema-helpers';

describe('schema-helpers', () => {
    describe('asMcpSchema', () => {
        it('should return the same schema object reference', () => {
            const schema = z.object({ name: z.string() });
            const result = asMcpSchema(schema);

            // Runtime: same object reference
            expect(result).toBe(schema);
        });

        it('should work with object schemas', () => {
            const schema = z.object({
                id: z.string(),
                count: z.number().optional()
            });
            expect(asMcpSchema(schema)).toBeDefined();
        });

        it('should work with string schemas', () => {
            const schema = z.string();
            expect(asMcpSchema(schema)).toBeDefined();
        });

        it('should work with number schemas', () => {
            const schema = z.number();
            expect(asMcpSchema(schema)).toBeDefined();
        });

        it('should work with enum schemas', () => {
            const schema = z.enum(['a', 'b', 'c']);
            expect(asMcpSchema(schema)).toBeDefined();
        });

        it('should work with array schemas', () => {
            const schema = z.array(z.string());
            expect(asMcpSchema(schema)).toBeDefined();
        });

        it('should work with nested object schemas', () => {
            const innerSchema = z.object({ value: z.string() });
            const outerSchema = z.object({ inner: innerSchema });
            expect(asMcpSchema(outerSchema)).toBeDefined();
        });

        it('should preserve schema validation behavior', () => {
            const schema = z.object({
                name: z.string(),
                age: z.number().optional()
            });
            const mcpSchema = asMcpSchema(schema);

            // The returned value should still be the same schema
            // and validation should work
            const validResult = schema.safeParse({ name: 'test' });
            expect(validResult.success).toBe(true);

            const invalidResult = schema.safeParse({ name: 123 });
            expect(invalidResult.success).toBe(false);

            // Verify identity
            expect(mcpSchema).toBe(schema);
        });

        it('should work with schemas that have refinements', () => {
            const schema = z
                .object({
                    password: z.string(),
                    confirmPassword: z.string()
                })
                .refine((data) => data.password === data.confirmPassword, {
                    message: 'Passwords do not match'
                });

            const mcpSchema = asMcpSchema(schema);
            expect(mcpSchema).toBe(schema);
        });

        it('should work with schemas that have defaults', () => {
            const schema = z.object({
                limit: z.number().default(10),
                offset: z.number().default(0)
            });

            const mcpSchema = asMcpSchema(schema);
            expect(mcpSchema).toBe(schema);
        });
    });
});
