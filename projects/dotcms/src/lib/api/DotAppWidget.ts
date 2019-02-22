import { DotAppBase, DotAppConfigParams } from './DotAppBase';

export class DotAppWidget extends DotAppBase {
    constructor(config: DotAppConfigParams) {
        super(config);
    }

    getHtml(widgetId: string): Promise<string> {
        return this.request({
            url: `/api/widget/id/${widgetId}`
        }).then((response) => response.text());
    }
}
