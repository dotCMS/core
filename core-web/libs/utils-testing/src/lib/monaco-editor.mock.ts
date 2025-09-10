export const monacoMock = {
    editor: {
        create: () => ({
            setModel: () => {},
            dispose: () => {},
            onDidChangeModelContent: (listener: () => void) => ({
                dispose: () => {}
            }),
            getValue: () => '',
            setValue: (value: string) => {
                //
            },
            getModel: () => ({
                uri: {
                    path: '/some/path'
                }
            }),
            updateOptions: (options: object) => {
                //
            },
            onDidChangeModelDecorations: (callback: () => void) => {
                callback();

                return {
                    dispose: () => {
                        //
                    }
                };
            },
            // Nuevas funciones agregadas
            onDidBlurEditorText: (listener: () => void) => ({
                dispose: () => {}
            }),
            onDidFocusEditorText: (listener: () => void) => ({
                dispose: () => {}
            }),
            layout: (dimension?: { width: number; height: number }) => {
                //
            },
            getPosition: () => ({
                lineNumber: 1,
                column: 1
            }),
            setPosition: (position: { lineNumber: number; column: number }) => {
                //
            },
            revealLine: (lineNumber: number) => {
                //
            }
        }),
        setModelLanguage: () => {},
        createModel: () => ({
            dispose: () => {}
        }),
        setTheme: () => {},
        getModelMarkers: (model: object) => {
            //

            return [
                {
                    severity: 1,
                    message: 'Simulated error',
                    startLineNumber: 1,
                    startColumn: 1,
                    endLineNumber: 1,
                    endColumn: 5
                }
            ];
        }
    },
    languages: {
        register: () => {},
        registerCompletionItemProvider: () => {},
        registerDefinitionProvider: () => {},
        setMonarchTokensProvider: () => {}
    },
    Uri: {
        parse: () => ({}),
        file: () => ({})
    }
};
