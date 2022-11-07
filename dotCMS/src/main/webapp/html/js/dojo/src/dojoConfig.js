// JUST RUN: rm -rf ./dist; node dojo/dojo.js load=build --profile ./dojoConfig.js --release

function timestamp(){
    // this function isn't really necessary...
    // just using it to show you can call a function to get a profile property value
    var d = new Date();
    return d.getFullYear() + '-' + (d.getMonth()+1) + "-" + d.getDate() + "-" +
        d.getHours() + ':' + d.getMinutes() + ":" + d.getSeconds();
}

var profile = {
    basePath: ".",
    buildTimestamp: timestamp(),
    releaseDir: "/Users/morera/dev/dotcms/core/dotCMS/src/main/webapp/html/js/dojo/custom-build",
    packages: [
      "dojo",
      "build",
      "dijit",
      "dojox"
    ],
    layers: {
      "dojo/dojo": {
        customBase: false,
        boot: true,
        include: [
          "dojo/dojo"
        ],
      },

      "build/build": {
        include: [
          "build/build"
        ]
      },
    }
};