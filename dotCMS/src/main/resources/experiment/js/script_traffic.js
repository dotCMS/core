console.log('traffic_scripts');

window.addEventListener("init_custom", function (event) {
    console.log('traffic init_custom', event.detail.experiment.variant);

    if (!window.location.href.includes('redirect=true')) {
        window.location = event.detail.experiment.variant.url;
    }
});