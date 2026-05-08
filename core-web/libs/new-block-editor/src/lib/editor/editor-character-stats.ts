import type { WritableSignal } from '@angular/core';

import type { Editor } from '@tiptap/core';

export interface CharacterStatsSignals {
    wordCount: WritableSignal<number>;
    charCount: WritableSignal<number>;
    readingTime: WritableSignal<number>;
}

/** Reads TipTap CharacterCount extension storage and updates UI signals. */
export function syncCharacterStatsFromEditor(editor: Editor, signals: CharacterStatsSignals): void {
    const storage = editor.storage as {
        characterCount?: { words: () => number; characters: () => number };
    };
    const cc = storage.characterCount;
    if (!cc) return;

    const words = cc.words();
    const chars = cc.characters();
    signals.wordCount.set(words);
    signals.charCount.set(chars);
    signals.readingTime.set(Math.max(1, Math.ceil(words / 200)));
}
