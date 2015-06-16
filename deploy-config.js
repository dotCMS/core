'use strict';

var fs = require('fs')
var localPath = './deploy-config.local.js';

var default_config = "'use strict';\n\n"
    + "module.exports = {\n"
    + "  artifactory: {\n"
    + "    release: {\n"
    + "     url: 'http://repo.dotcms.com/artifactory/libs-release-local',\n"
    + "      username: '',\n"
    + "     password: ''\n"
    + "    },\n"
    + "    snapshot: {\n"
    + "     url: 'http://repo.dotcms.com/artifactory/libs-snapshot-local',\n"
    + "      username: '',\n"
    + "     password: ''\n"
    + "    }\n"
    + "\n"
    + "  }\n"
    + "};\n"

var out = {artifactory: "Please configure your deploy-config.local.js file with the appropriate credentials."}

var exists = fs.existsSync(localPath)
if (!exists) {
  console.error("WARNING: ./deploy-config.local.js does not exist. Creating it now.")
  fs.writeFile(localPath, default_config, function (err) {
    if (err) throw err;
    console.log("WARNING: ./deploy-config.local.js created. You will need to edit the file to provide appropriate credentials.")
  })
} else {
  out.artifactory = require(localPath).artifactory
}

module.exports = out;


