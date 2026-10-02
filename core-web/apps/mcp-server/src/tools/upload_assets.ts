import { uploadAssetsTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(uploadAssetsTool);

export { schema, metadata };
export default handler;
