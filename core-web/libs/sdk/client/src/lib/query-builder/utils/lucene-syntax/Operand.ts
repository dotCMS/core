import { buildField, buildTerm } from '..';

// The operands can only return fields or terms. They can't return other operands.
export class Operand {
    #query = '';

    constructor(private query: string) {
        this.#query = this.query;
    }

    excludeField(field: string) {
        return buildField(this.#query, field, true);
    }

    field(field: string) {
        return buildField(this.#query, field);
    }

    term(term: string) {
        return buildTerm(this.#query, term);
    }
}
