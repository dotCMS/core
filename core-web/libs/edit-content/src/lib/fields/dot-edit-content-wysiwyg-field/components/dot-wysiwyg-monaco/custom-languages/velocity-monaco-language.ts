// Velocity language definition for Monaco Editor
export const dotVelocityLanguageDefinition: monaco.languages.IMonarchLanguage = {
    defaultToken: '',
    tokenPostfix: '.vtl',
    ignoreCase: true,

    brackets: [
        { open: '{', close: '}', token: 'delimiter.curly' },
        { open: '[', close: ']', token: 'delimiter.square' },
        { open: '(', close: ')', token: 'delimiter.parenthesis' },
        { open: '<', close: '>', token: 'delimiter.angle' }
    ],

    keywords: [
        'foreach',
        'if',
        'else',
        'elseif',
        'end',
        'set',
        'parse',
        'include',
        'macro',
        'stop',
        'dotParse'
    ],

    tokenizer: {
        root: [
            // HTML Comments
            [/<!--/, 'comment.html', '@htmlComment'],

            // Velocity Directives

            [/#(foreach|if|else|elseif|end|set|parse|include|macro|stop)\b/, 'keyword.velocity'],

            [/#(dotParse)\b/, 'keyword.dotparse.velocity'],

            // Velocity Variables (más inclusivo)
            [
                /(\$!?)(\{?)([a-zA-Z_][\w-]*(?:\.[a-zA-Z_][\w-]*)*)/,
                {
                    cases: {
                        '$2=={': [
                            'variable.velocity.delimiter',
                            'variable.velocity.delimiter',
                            { token: 'variable.velocity', next: '@velocityVariable' }
                        ],
                        '$1==$': ['variable.velocity.delimiter', '', 'variable.velocity'],
                        '@default': ['', '', 'variable.velocity']
                    }
                }
            ],

            // Variables simples que comienzan con $
            [/\$[a-zA-Z][\w-]*/, 'variable.velocity'],

            // HTML Tags
            [/<\/?[\w\-:.]+/, 'tag.html'],

            // HTML Attributes
            [/[a-zA-Z][a-zA-Z0-9_-]*(?=\s*=)/, 'attribute.name.html'],
            [/"[^"]*"|'[^']*'/, 'attribute.value.html'],

            // Velocity Comments
            [/##[^\n]*/, 'comment.velocity'],
            [/#\*(?!\*)/, 'comment.velocity', '@velocityComment'],

            // Strings
            [/"/, 'string.velocity', '@string_double'],
            [/'/, 'string.velocity', '@string_single'],

            // Other syntax
            [/[{}()[\]]/, 'delimiter.velocity'],
            [/[<>]/, 'delimiter.angle.velocity'],
            [/[;,.]/, 'delimiter.velocity']
        ],

        htmlComment: [
            [/[^-]+/, 'comment.html'],
            [/-->/, 'comment.html', '@pop'],
            [/-/, 'comment.html']
        ],

        htmlAttributeValue: [
            [/[^"]+/, 'string.html'],
            [
                /(\$!?\{?)([a-zA-Z][\w-]*(?:\.[a-zA-Z][\w-]*)*(?:\([^)]*\))?)(\})?/,
                ['variable.velocity', 'variable.velocity', 'variable.velocity']
            ],
            [/\$[a-zA-Z][\w-]*/, 'variable.velocity'],
            [/"/, { token: 'string.html', next: '@pop' }]
        ],

        string_double: [
            [/[^\\"$]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [
                /(\$!?\{?)([a-zA-Z][\w-]*(?:\.[a-zA-Z][\w-]*)*(?:\([^)]*\))?)(\})?/,
                ['variable.velocity', 'variable.velocity', 'variable.velocity']
            ],
            [/\$[a-zA-Z][\w-]*/, 'variable.velocity'],
            [/"/, 'string.velocity', '@pop']
        ],

        string_single: [
            [/[^\\'$]+/, 'string.velocity'],
            [/\\./, 'string.escape.velocity'],
            [
                /(\$!?\{?)([a-zA-Z][\w-]*(?:\.[a-zA-Z][\w-]*)*(?:\([^)]*\))?)(\})?/,
                ['variable.velocity', 'variable.velocity', 'variable.velocity']
            ],
            [/\$[a-zA-Z][\w-]*/, 'variable.velocity'],
            [/'/, 'string.velocity', '@pop']
        ],

        velocityComment: [
            [/[^*#]+/, 'comment.velocity'],
            [/#\*/, 'comment.velocity', '@push'],
            [/\*#/, 'comment.velocity', '@pop'],
            [/[*#]/, 'comment.velocity']
        ],

        velocityVariable: [
            [/\}/, 'variable.velocity.delimiter', '@pop'],
            [/\(/, 'delimiter.parenthesis', '@velocityMethod'],
            [/[^}()]/, 'variable.velocity']
        ],

        velocityMethod: [
            [/\)/, 'delimiter.parenthesis', '@pop'],
            [/\(/, 'delimiter.parenthesis', '@push'],
            [/[^()]/, 'variable.velocity']
        ]
    }
};
