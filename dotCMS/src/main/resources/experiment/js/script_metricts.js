var initEvent = new Promise(function(resolve) {
    window.addEventListener("init_custom", resolve, false);
});

var loadEvent = new Promise(function(resolve) {
    window.addEventListener("load", resolve, false);
});

Promise.all([initEvent, loadEvent]).then(function(data) {
    ${dinamycCodeHere}
});