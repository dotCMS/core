import { renderHook } from '@testing-library/react-hooks';

import { StyleEditorFormSchema, registerStyleEditorSchemas } from '@dotcms/uve';

import { useStyleEditorSchemas } from '../../hooks/useStyleEditorSchemas';

jest.mock('@dotcms/uve', () => ({
    registerStyleEditorSchemas: jest.fn(),
    StyleEditorFormSchema: {}
}));

describe('useStyleEditorSchemas', () => {
    const registerStyleEditorSchemasMock = registerStyleEditorSchemas as jest.Mock;

    const mockForm1: StyleEditorFormSchema = {
        contentType: 'BlogPost',
        sections: [
            {
                title: 'Typography',
                fields: [
                    {
                        type: 'input',
                        label: 'Font Size',
                        config: {
                            inputType: 'number',
                            defaultValue: 1
                        }
                    }
                ]
            }
        ]
    };

    const mockForm2: StyleEditorFormSchema = {
        contentType: 'Banner',
        sections: [
            {
                title: 'Colors',
                fields: [
                    {
                        type: 'input',
                        label: 'Background Color',
                        config: {
                            inputType: 'text',
                            defaultValue: '#FFFFFF'
                        }
                    }
                ]
            }
        ]
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should call registerStyleEditorSchemas with provided forms on mount', () => {
        const forms = [mockForm1];

        renderHook(() => useStyleEditorSchemas(forms));

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledWith(forms);
    });

    test('should call registerStyleEditorSchemas with multiple forms', () => {
        const forms = [mockForm1, mockForm2];

        renderHook(() => useStyleEditorSchemas(forms));

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledWith(forms);
    });

    test('should call registerStyleEditorSchemas with empty array', () => {
        const forms: StyleEditorFormSchema[] = [];

        renderHook(() => useStyleEditorSchemas(forms));

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledWith(forms);
    });

    test('should re-register forms when forms array changes', () => {
        const initialForms = [mockForm1];
        const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
            initialProps: { forms: initialForms }
        });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledWith(initialForms);

        const updatedForms = [mockForm1, mockForm2];
        rerender({ forms: updatedForms });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(2);
        expect(registerStyleEditorSchemasMock).toHaveBeenLastCalledWith(updatedForms);
    });

    test('should re-register forms when forms array reference changes even with same content', () => {
        const forms1 = [mockForm1];
        const forms2 = [mockForm1]; // Same content, different reference

        const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
            initialProps: { forms: forms1 }
        });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

        rerender({ forms: forms2 });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(2);
        expect(registerStyleEditorSchemasMock).toHaveBeenNthCalledWith(1, forms1);
        expect(registerStyleEditorSchemasMock).toHaveBeenNthCalledWith(2, forms2);
    });

    test('should not re-register when forms array reference does not change', () => {
        const forms = [mockForm1];

        const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
            initialProps: { forms }
        });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

        // Rerender with same forms reference
        rerender({ forms });

        // Should not call again because forms reference hasn't changed
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
    });

    test('should handle forms array being replaced with empty array', () => {
        const initialForms = [mockForm1, mockForm2];
        const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
            initialProps: { forms: initialForms }
        });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledWith(initialForms);

        const emptyForms: StyleEditorFormSchema[] = [];
        rerender({ forms: emptyForms });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(2);
        expect(registerStyleEditorSchemasMock).toHaveBeenLastCalledWith(emptyForms);
    });

    test('should handle forms array being replaced with different forms', () => {
        const initialForms = [mockForm1];
        const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
            initialProps: { forms: initialForms }
        });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

        const differentForms = [mockForm2];
        rerender({ forms: differentForms });

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(2);
        expect(registerStyleEditorSchemasMock).toHaveBeenLastCalledWith(differentForms);
    });

    test('should cleanup and not call registerStyleEditorSchemas after unmount', () => {
        const forms = [mockForm1];
        const { unmount } = renderHook(() => useStyleEditorSchemas(forms));

        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

        unmount();

        // Should not call again after unmount
        expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
    });

    describe('infinite loop prevention', () => {
        test('should not cause infinite loop when inline object is passed on each render', () => {
            // Simulate a component that passes inline forms on each render
            // This creates a new reference each time, which could cause infinite loops
            const { rerender } = renderHook(
                () =>
                    useStyleEditorSchemas([
                        {
                            contentType: 'InlineForm',
                            sections: [
                                {
                                    title: 'Inline Section',
                                    fields: [
                                        {
                                            type: 'input',
                                            label: 'Inline Field',
                                            config: { inputType: 'text' }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]),
                {}
            );

            // First render
            expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

            // Simulate multiple re-renders (as would happen in a real component)
            rerender();
            rerender();
            rerender();

            // With current implementation, each rerender with inline object creates new reference
            // This is expected behavior - the hook re-registers on each new reference
            // The key is that it doesn't cause an INFINITE loop (exponential calls)
            // It should be called once per render, not exponentially
            const callCount = registerStyleEditorSchemasMock.mock.calls.length;

            // Should be exactly 4 calls (1 initial + 3 rerenders), not exponentially growing
            expect(callCount).toBe(4);

            // Additional rerenders should add linearly, not exponentially
            rerender();
            rerender();

            expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(6);
        });

        test('should demonstrate stable reference prevents unnecessary re-registrations', () => {
            // This test shows the recommended pattern: memoize forms to prevent re-registration
            const stableForms = [mockForm1];

            const { rerender } = renderHook(({ forms }) => useStyleEditorSchemas(forms), {
                initialProps: { forms: stableForms }
            });

            expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);

            // Multiple rerenders with same reference should NOT re-register
            rerender({ forms: stableForms });
            rerender({ forms: stableForms });
            rerender({ forms: stableForms });

            // Should still be 1 - stable reference prevents re-registration
            expect(registerStyleEditorSchemasMock).toHaveBeenCalledTimes(1);
        });
    });
});
