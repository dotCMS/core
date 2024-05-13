import { buildTerm } from '..';

// The NotOperand can only return terms. It can't return fields or other operands.
export class NotOperand {
    #query = '';

    constructor(private query: string) {
        this.#query = this.query;
    }

    term(term: string) {
        return buildTerm(this.#query, term);
    }
}
