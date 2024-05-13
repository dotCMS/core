import { buildTerm } from '..';

export class Field {
    #query = '';
    constructor(private query: string) {
        this.#query = this.query;
    }

    term(term: string) {
        return buildTerm(this.#query, term);
    }
}
