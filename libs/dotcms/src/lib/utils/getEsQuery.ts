import { DotCMSElasticSearchParams } from '../models';

// tslint:disable-next-line:cyclomatic-complexity
export function getEsQuery(params: DotCMSElasticSearchParams): string {
    const {
        contentType,
        queryParams: {
            languageId,
            sortResultsBy,
            sortOrder1,
            offset,
            pagination,
            itemsPerPage,
            numberOfResults,
            detailedSearchQuery
        }
    } = params;
    const paginationQuery = `,
        "from": OFFSETVALUE,
        "size": SIZEPERPAGE`;

    let query = `{
        "query": {
            "bool": {
                "must": {
                    "query_string" : {
                        "query" : "+contentType:CONTENTTYPE +languageId:LANGUAGEIDVALUE ${
                            detailedSearchQuery || ''
                        }"
                    }
                }
            }
        },
        "sort" : [
            { "SORTBYVALUE" : {"order" : "SORTTYPEVALUE"}}
        ]
        ${pagination || numberOfResults ? paginationQuery : ''}
    }`;

    const esParams = {
        CONTENTTYPE: contentType,
        LANGUAGEIDVALUE: languageId || '1',
        SORTBYVALUE: sortResultsBy || 'title_dotraw',
        SORTTYPEVALUE: sortOrder1 || 'asc',
        OFFSETVALUE: offset || '0',
        SIZEPERPAGE: pagination ? itemsPerPage || 20 : numberOfResults || 40
    };

    Object.keys(esParams).forEach((key) => {
        query = query.replace(key, esParams[key]);
    });

    return query;
}
