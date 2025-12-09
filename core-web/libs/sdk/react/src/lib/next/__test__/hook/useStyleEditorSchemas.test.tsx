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
                columns: 1,
                fields: [
                    [
                        {
                            type: 'input',
                            label: 'Font Size',
                            config: {
                                inputType: 'number',
                                defaultValue: 16
                            }
                        }
                    ]
                ]
            }
        ]
    };

    const mockForm2: StyleEditorFormSchema = {
        contentType: 'Banner',
        sections: [
            {
                title: 'Colors',
                columns: 1,
                fields: [
                    [
                        {
                            type: 'input',
                            label: 'Background Color',
                            config: {
                                inputType: 'text',
                                defaultValue: '#FFFFFF'
                            }
                        }
                    ]
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
});
