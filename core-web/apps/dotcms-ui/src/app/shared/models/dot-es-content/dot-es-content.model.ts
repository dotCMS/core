import { DotCMSContentlet } from "@dotcms/dotcms-models";

export interface ESContent {
    contentTook: number;
    jsonObjectView: { contentlets: DotCMSContentlet[] };
    queryTook: number;
    resultsSize: number;
}
