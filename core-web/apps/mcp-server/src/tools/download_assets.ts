import { downloadAssetsTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(downloadAssetsTool);

export { schema, metadata };
export default handler;
