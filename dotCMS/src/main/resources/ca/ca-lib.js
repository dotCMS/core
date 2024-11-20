! function() {
    "use strict";
    if (window) {
        console.log("YAAAAY CA lib is working")
    } else {
        console.log("CA lib called outside browser context. It will be ignored")
    }
}();
