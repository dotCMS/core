import { pagePlaceContentTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(pagePlaceContentTool);

export { schema, metadata };
export default handler;
