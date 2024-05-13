import { OPERAND, buildField, buildNotOperand, buildOperand, santizeQuery } from '..';

// After a Term we can start building another Field or concat an operand
export class Term {
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

    or() {
        return buildOperand(this.#query, OPERAND.OR);
    }

    and() {
        return buildOperand(this.#query, OPERAND.AND);
    }

    not() {
        return buildNotOperand(this.#query);
    }

    build() {
        return santizeQuery(this.#query);
    }
}
