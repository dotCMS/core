export const HTML_BASE_TOKENIZER_ROOT = [
    [/<!DOCTYPE/, 'metatag', '@doctype'],
    [/<!--/, 'comment', '@comment'],
    [/(<)((?:[\w-]+:)?[\w-]+)(\s*)(\/>)/, ['delimiter', 'tag', '', 'delimiter']],
    [/(<)(script)/, ['delimiter', { token: 'tag', next: '@script' }]],
    [/(<)(style)/, ['delimiter', { token: 'tag', next: '@style' }]],
    [/(<)((?:[\w-]+:)?[\w-]+)/, ['delimiter', { token: 'tag', next: '@otherTag' }]],
    [/(<\/)((?:[\w-]+:)?[\w-]+)/, ['delimiter', { token: 'tag', next: '@otherTag' }]],
    [/</, 'delimiter'],
    [/[^<#$]+/, ''] // text
] as monaco.languages.IMonarchLanguageRule[];

export const HTML_BASE_TOKENIZER_STATES = {
    doctype: [
        [/[^>]+/, 'metatag.content'],
        [/>/, 'metatag', '@pop']
    ],

    comment: [
        [/-->/, 'comment', '@pop'],
        [/[^-]+/, 'comment.content'],
        [/./, 'comment.content']
    ],

    otherTag: [
        [/\/?>/, 'delimiter', '@pop'],
        [/"([^"]*)"/, 'attribute.value'],
        [/'([^']*)'/, 'attribute.value'],
        [/[\w-]+/, 'attribute.name'],
        [/=/, 'delimiter'],
        [/[ \t\r\n]+/, ''] // whitespace
    ],

    script: [
        [/type/, 'attribute.name', '@scriptAfterType'],
        [/"([^"]*)"/, 'attribute.value'],
        [/'([^']*)'/, 'attribute.value'],
        [/[\w-]+/, 'attribute.name'],
        [/=/, 'delimiter'],
        [
            />/,
            {
                token: 'delimiter',
                next: '@scriptEmbedded',
                nextEmbedded: 'text/javascript'
            }
        ],
        [/[ \t\r\n]+/], // whitespace
        [/(<\/)(script\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]]
    ],

    scriptAfterType: [
        [/=/, 'delimiter', '@scriptAfterTypeEquals'],
        [
            />/,
            {
                token: 'delimiter',
                next: '@scriptEmbedded',
                nextEmbedded: 'text/javascript'
            }
        ],
        [/[ \t\r\n]+/], // whitespace
        [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    scriptAfterTypeEquals: [
        [/"([^"]*)"/, { token: 'attribute.value', switchTo: '@scriptWithCustomType.$1' }],
        [/'([^']*)'/, { token: 'attribute.value', switchTo: '@scriptWithCustomType.$1' }],
        [
            />/,
            {
                token: 'delimiter',
                next: '@scriptEmbedded',
                nextEmbedded: 'text/javascript'
            }
        ],
        [/[ \t\r\n]+/], // whitespace
        [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    scriptWithCustomType: [
        [/>/, { token: 'delimiter', next: '@scriptEmbedded.$S2', nextEmbedded: '$S2' }],
        [/"([^"]*)"/, 'attribute.value'],
        [/'([^']*)'/, 'attribute.value'],
        [/[\w-]+/, 'attribute.name'],
        [/=/, 'delimiter'],
        [/[ \t\r\n]+/], // whitespace
        [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    scriptEmbedded: [
        [/<\/script/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
        [/[^<]+/, '']
    ],

    style: [
        [/type/, 'attribute.name', '@styleAfterType'],
        [/"([^"]*)"/, 'attribute.value'],
        [/'([^']*)'/, 'attribute.value'],
        [/[\w-]+/, 'attribute.name'],
        [/=/, 'delimiter'],
        [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
        [/[ \t\r\n]+/], // whitespace
        [/(<\/)(style\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]]
    ],

    styleAfterType: [
        [/=/, 'delimiter', '@styleAfterTypeEquals'],
        [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
        [/[ \t\r\n]+/], // whitespace
        [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    styleAfterTypeEquals: [
        [/"([^"]*)"/, { token: 'attribute.value', switchTo: '@styleWithCustomType.$1' }],
        [/'([^']*)'/, { token: 'attribute.value', switchTo: '@styleWithCustomType.$1' }],
        [
            />/,
            {
                token: 'delimiter',
                next: '@styleEmbedded',
                nextEmbedded: 'text/css'
            }
        ],
        [/[ \t\r\n]+/], // whitespace
        [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    styleWithCustomType: [
        [
            />/,
            {
                token: 'delimiter',
                next: '@styleEmbedded.$S2',
                nextEmbedded: '$S2'
            }
        ],
        [/"([^"]*)"/, 'attribute.value'],
        [/'([^']*)'/, 'attribute.value'],
        [/[\w-]+/, 'attribute.name'],
        [/=/, 'delimiter'],
        [/[ \t\r\n]+/], // whitespace
        [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
    ],

    styleEmbedded: [
        [/<\/style/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
        [/[^<]+/, '']
    ]
};
