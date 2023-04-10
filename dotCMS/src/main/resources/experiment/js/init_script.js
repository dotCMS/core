
function setJitsuExperimentData (experimentData) {
    let experimentsShortData = {
        experiments: experimentData.experiments.map((experiment) => ({
                experiment: experiment.id,
                variant: experiment.variant.name,
                lookBackWindow: experiment.lookBackWindow.value
            })
        )
    };

    jitsu('set', experimentsShortData);
}

window.addEventListener("experiment_loaded", function (event) {
    let experimentData = event.detail;
    setJitsuExperimentData(experimentData);

    if (!location.href.includes("redirect=true")) {
        for (let i = 0; i < experimentData.experiments.length; i++) {
            const pageUrl = experimentData.experiments[i].pageUrl;

            const alternativePageUrl = experimentData.experiments[i].pageUrl.endsWith(
                "/index") ?
                experimentData.experiments[i].pageUrl.replace("index", "")
                : experimentData.experiments[i].pageUrl;

            if (location.href.includes(pageUrl)
                || location.href.endsWith(alternativePageUrl)) {

                let url = experimentData.experiments[i].variant.url
                const param = (url.includes("?") ? "&" : "?")
                    + "redirect=true";

                location.href = url + param;
                break;
            }
        }
    }
});

let experimentAlreadyCheck = sessionStorage.getItem("experimentAlreadyCheck");

if (!experimentAlreadyCheck) {
    let currentRunningExperimentsId = [${running_experiments_list}];

    function shouldHitEndPoint() {
        let experimentData = localStorage.getItem('experiment_data');

        if (experimentData) {
            let includedExperimentIds = JSON.parse(
                experimentData).includedExperimentIds;

            return !currentRunningExperimentsId.every(
                element => includedExperimentIds.includes(element));
        } else {
            return true;
        }
    }

    function cleanExperimentDataUp() {
        let experimentDataAsString = localStorage.getItem('experiment_data');

        if (experimentDataAsString) {
            let experimentData = JSON.parse(experimentDataAsString);

            var now = Date.now();

            experimentData.experiments = experimentData.experiments
            .filter(experiment => currentRunningExperimentsId.includes(experiment.id))
            .filter(experiment => experiment.lookBackWindow.expireTime > now);


            experimentData.includedExperimentIds = experimentData.includedExperimentIds
            .filter(experimentId => currentRunningExperimentsId.includes(experimentId));

            if (!experimentData.experiments.length) {
                localStorage.removeItem('experiment_data');
            } else {
                localStorage.setItem('experiment_data', JSON.stringify(experimentData));
            }
        }
    }

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
                let oldExperimentData = JSON.parse(
                    localStorage.getItem('experiment_data'));

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

                var now = Date.now();

                dataToStorage.experiments = dataToStorage.experiments.map(experiment => ({
                    ...experiment,
                    lookBackWindow: {
                        ...experiment.lookBackWindow,
                        expireTime: now + experiment.lookBackWindow.expireMillis
                    }
                }));

                localStorage.setItem('experiment_data',
                    JSON.stringify(dataToStorage));

                const event = new CustomEvent('experiment_data_loaded',
                    {detail: dataToStorage});
                window.dispatchEvent(event);
            }
        });
    }

    cleanExperimentDataUp();
    let experimentDataAsString = localStorage.getItem('experiment_data');

    if (experimentDataAsString) {
        let experimentData = JSON.parse(experimentDataAsString);

        const event = new CustomEvent('experiment_loaded',
            {detail: experimentData});
        window.dispatchEvent(event);
    }

    sessionStorage.setItem("experimentAlreadyCheck", true);
} else {
    let experimentData = JSON.parse(localStorage.getItem('experiment_data'));

    const event = new CustomEvent('experiment_loaded',
        {detail: experimentData});
    window.dispatchEvent(event);
}

