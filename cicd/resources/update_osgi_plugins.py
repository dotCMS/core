import sys
import re

plugin_file = sys.argv[1]
replacing_text = sys.argv[2]

with open(plugin_file, 'r') as fr:
  content = fr.read()
  content_new = re.sub('com.dotcms:dotcms:\d{2}\.\d{2}((\.\d{1,2})*)', 'com.dotcms:dotcms:' + replacing_text, content, flags = re.M)
  content_new = re.sub('name: \'dotcms\', version: \'\d{2}\.\d{2}((\.\d{1,2})*)\'', 'name: \'dotcms\', version: \'' + replacing_text + '\'', content_new, flags = re.M)
  fr.close()

with open(plugin_file, 'w') as fw:
  fw.write(content_new)
  fw.close()
