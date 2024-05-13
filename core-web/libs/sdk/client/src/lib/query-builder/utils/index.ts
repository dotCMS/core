import { Field } from './lucene-syntax/Field';
import { Operand } from './lucene-syntax/Operand';
import { Term } from './lucene-syntax/Term';

export function santizeQuery(str: string): string {
    return str.replace(/\s{2,}/g, ' ').trim();
}

export function sanitizePhrases(term: string): string {
    return term.includes(' ') ? `"${term}"` : term;
}

export function buildTerm(query: string, term: string): Term {
    const newQuery = query + sanitizePhrases(term);

    return new Term(newQuery);
}

export function buildField(query: string, field: string, exclude = false): Field {
    const newQuery = query + (exclude ? ` -${field}:` : ` +${field}:`);

    return new Field(newQuery);
}

export function buildOperand(query: string, operand: string): Operand {
    const newQuery = query + ` ${operand} `;

    return new Operand(newQuery);
}
