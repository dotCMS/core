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
                console.log(`Editor value set to: ${value}`);
            },
            getModel: () => ({
                uri: {
                    path: '/some/path'
                }
            }),
            updateOptions: (options: object) => {
                console.log('Editor options updated:', options);
            },
            onDidChangeModelDecorations: (callback: () => void) => {
                console.log('Model decorations changed');
                callback();

                return {
                    dispose: () => {
                        console.log('Listener disposed');
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
                console.log('Editor layout updated:', dimension);
            },
            getPosition: () => ({
                lineNumber: 1,
                column: 1
            }),
            setPosition: (position: { lineNumber: number; column: number }) => {
                console.log('Editor position set to:', position);
            },
            revealLine: (lineNumber: number) => {
                console.log('Revealing line:', lineNumber);
            }
        }),
        setModelLanguage: () => {},
        createModel: () => ({
            dispose: () => {}
        }),
        setTheme: () => {},
        getModelMarkers: (model: object) => {
            console.log('Getting model markers for model:', model);

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
        registerDefinitionProvider: () => {}
    },
    Uri: {
        parse: () => ({}),
        file: () => ({})
    }
};
