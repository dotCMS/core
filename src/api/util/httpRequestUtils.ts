/**
 * Created by oswaldogallango on 9/27/16.
 */

export class HttpRequestUtils {

    /**
     * Get a map with the url querystring parameters
     * @returns {Map<string, string>}
     */
     getQueryParams(): Map<string, string> {
        let split: string[] = window.location.search.substring(1).split('&');
        let map: Map<string, string> = new Map();

        split.forEach(param => {
            let paramSplit: string[] = param.split('=');
            map.set(paramSplit[0], paramSplit[1]);
        });

        return map;
    }
}