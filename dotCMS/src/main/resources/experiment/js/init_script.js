
let currentRunningExperimentsId = [${running_experiments_list}]

function shouldHitEndPoint() {
    let experimentData = localStorage.getItem('experiment_data');

    if (experimentData) {
        let includedExperimentIds = JSON.parse(
            experimentData).includedExperimentIds;

        return !currentRunningExperimentsId.every(element => includedExperimentIds.includes(element));
    } else {
        return true;
    }
}

window.addEventListener("experiment_data_loaded", function (event) {

    let experimentData = event.detail;
    console.log('experiment_data', experimentData);
    for (let i = 0; i < experimentData.experiments.length; i++){
        let pageUrl = experimentData.experiments[i].pageUrl;

        let alternativePageUrl = experimentData.experiments[i].pageUrl.endsWith("/index") ?
            experimentData.experiments[i].pageUrl.replace("/index", "") :
            experimentData.experiments[i].pageUrl;

        if (window.location.href.includes(pageUrl) || window.location.href.includes(alternativePageUrl)) {

            let url = experimentData.experiments[i].variant.url
            const param = (url.includes("?") ? "&" : "?") + "redirect=true";
            window.location.href = url + param;
            break;
        }
    }
});

if (shouldHitEndPoint()) {
    let experimentData = localStorage.getItem('experiment_data');
    let body = experimentData ?
        {
            exclude: JSON.parse(experimentData).includedExperimentIds
        } : {
            exclude: []
        };

    fetch('/api/v1/experiments/isUserIncluded', {
        method: 'POST',
        body: JSON.stringify(body),
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.entity.experiments) {
            let dataToStorage = Object.assign({}, data.entity);
            let oldExperimentData = JSON.parse(localStorage.getItem('experiment_data'));

            delete dataToStorage['excludedExperimentIds'];

            dataToStorage.includedExperimentIds = [
                ...dataToStorage.includedExperimentIds,
                ...data.entity.excludedExperimentIds
            ];

            if (oldExperimentData) {
                dataToStorage.experiments = [
                    ...oldExperimentData.experiments,
                    ...dataToStorage.experiments
                ];
            }

            localStorage.setItem('experiment_data', JSON.stringify(dataToStorage));

            const event = new CustomEvent('experiment_data_loaded', { detail: dataToStorage });
            window.dispatchEvent(event);
        }
    });
} else if (!window.location.href.includes("redirect=true")) {
    let experimentData = JSON.parse(localStorage.getItem('experiment_data'));

    const event = new CustomEvent('experiment_data_loaded', { detail: experimentData });
    window.dispatchEvent(event);
}

