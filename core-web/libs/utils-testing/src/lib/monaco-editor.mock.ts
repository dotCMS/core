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
            },
            getSelection: () => ({
                startLineNumber: 1,
                startColumn: 1,
                endLineNumber: 1,
                endColumn: 1
            }),
            executeEdits: () => {},
            focus: () => {}
        }),
        setModelLanguage: () => {},
        createModel: () => ({
            uri: { path: '/some/path' },
            dispose: () => {}
        }),
        // The two remaining members @materia-ui/ngx-monaco-editor reaches for. Its
        // `initEditor` / diff paths call these, and a missing one surfaces only as
        // `ReferenceError: monaco is not defined` from a lifecycle hook — an error rxjs
        // reports asynchronously, so it stayed invisible under Jest.
        getModels: () => [],
        createDiffEditor: () => ({
            setModel: () => {},
            dispose: () => {},
            onDidChangeModelContent: () => ({ dispose: () => {} }),
            onDidChangeModelDecorations: () => ({ dispose: () => {} }),
            onDidBlurEditorText: () => ({ dispose: () => {} }),
            getValue: () => '',
            setValue: () => {},
            getModel: () => null,
            updateOptions: () => {},
            layout: () => {}
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
    },
    Range: class {
        startLineNumber: number;
        startColumn: number;
        endLineNumber: number;
        endColumn: number;

        constructor(
            startLineNumber: number,
            startColumn: number,
            endLineNumber: number,
            endColumn: number
        ) {
            this.startLineNumber = startLineNumber;
            this.startColumn = startColumn;
            this.endLineNumber = endLineNumber;
            this.endColumn = endColumn;
        }
    }
};
