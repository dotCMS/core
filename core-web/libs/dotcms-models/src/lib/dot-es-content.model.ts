import { DotCMSContentlet } from './dot-contentlet.model';

export interface ESContent {
    contentTook: number;
    jsonObjectView: { contentlets: DotCMSContentlet[] };
    queryTook: number;
    resultsSize: number;
}
