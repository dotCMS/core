import { buildField } from './utils';

export class QueryBuilder {
    #query = '';

    field(field: string) {
        return buildField(this.#query, field);
    }

    excludeField(field: string) {
        return buildField(this.#query, field, true);
    }
}
