/**
 * Created by oswaldogallango on 9/27/16.
 */

export class HttpRequestUtils {

    /**
     * Get a map with the url querystring parameters
     * @returns {Map<string, string>}
     */
     // TODO: change the getQueryParams() with an NG2 method equivalent to QueryParams on NGRX.
     getQueryParams(): Map<string, string> {
        let split: string[] = window.location.search.substring(1).split('&');
        let map: Map<string, string> = new Map();

        split.forEach(param => {
            let paramSplit: string[] = param.split('=');
            map.set(paramSplit[0], paramSplit[1]);
        });

        return map;
    }

    /**
     * Get a single parameter form the query string, null if does not exits.
     * it is based on the window.location.href.
     * @returns {string}
     */
    getQueryStringParam(name: string): string {

        let value = null;
        var regex = new RegExp("[?&]" + name.replace(/[\[\]]/g, "\\$&") + "(=([^&#]*)|&|#|$)");
        let results = regex.exec(window.location.href);

        if (results && results[2]) {

            value = decodeURIComponent(results[2].replace(/\+/g, " "));
        }

        return value;
    } // getQueryStringParam.

}