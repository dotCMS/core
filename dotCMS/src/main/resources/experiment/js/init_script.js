
let regexsChecks = [];

function setJitsuExperimentData (experimentData) {
    let experimentsShortData = {
        experiments: experimentData.experiments.map((experiment) => ({
                experiment: experiment.id,
                runningId: experiment.runningId,
                variant: experiment.variant.name,
                lookBackWindow: experiment.lookBackWindow.value,
                ...regexsChecks.filter(regexCheked => regexCheked.id === experiment.id)[0].checks
            })
        )
    };

    jitsu('set', experimentsShortData);
}

function stopRender(){
    window.stop();
}

function getParams (experimentData) {
    return (location.href.includes("?") ? "&" : "?") + `variantName=${experimentData.variant.name}&redirect=true`;
}

function validateRegexs(experimentsData){

    for (let i = 0; i < experimentsData.experiments.length; i++) {
        let experimentId = experimentsData.experiments[i].id;
        let experimentRegex = {id: experimentId, checks: []}

        for (const key in experimentsData.experiments[i].regexs) {
            const pattern = new RegExp(experimentsData.experiments[i].regexs[key]);

            let indexOf = location.href.indexOf("?");
            let urlWithoutParameters = indexOf > -1 ? location.href.substring(0, indexOf) : location.href;
            let parameters = indexOf > -1 ? location.href.substring(indexOf) : '';

            let url = urlWithoutParameters.toLowerCase() + parameters;
            experimentRegex.checks[key] = pattern.test(url)
        }
        regexsChecks.push(experimentRegex);

    }
}

function redirectIfNeedIt(experimentsData,
                          additionalValidation = (experimentData) => true){

    if (!location.href.includes("redirect=true")) {

        for (let i = 0; i < experimentsData.experiments.length; i++) {
            let experimentId = experimentsData.experiments[i].id;
            let regexs = regexsChecks.filter(regexs => regexs.id === experimentId)[0];

            if (additionalValidation(experimentsData.experiments[i]) && regexs.checks.isExperimentPage) {
                const param = experimentsData.experiments[i].variant.name === 'DEFAULT' ?
                    '' : getParams(experimentsData.experiments[i]);

                location.href = location.href + param;

                return true;
            }
        }
    }

    return false;
}

window.addEventListener("experiment_loaded", function (event) {
    let experimentsData = event.detail;
    validateRegexs(experimentsData);
    setJitsuExperimentData(experimentsData);

    redirectIfNeedIt(experimentsData, (experimentData) => experimentData.variant.name !== 'DEFAULT');
});

window.addEventListener("experiment_loaded_from_endpoint", function (event) {
    let experimentsData = event.detail;
    validateRegexs(experimentsData);
    setJitsuExperimentData(experimentsData);
    const wasRedirect = redirectIfNeedIt(experimentsData);

    if (!wasRedirect) {
        location.reload();
    }
});

let experimentAlreadyCheck = sessionStorage.getItem("experimentAlreadyCheck");

if (!experimentAlreadyCheck) {
    let currentRunningExperimentsId = [${running_experiments_list}];

    function shouldHitEndPoint() {
        let experimentData = localStorage.getItem('experiment_data');

        if (experimentData) {
            let includedExperimentIds = JSON.parse(experimentData)
                .includedExperimentIds;

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

    cleanExperimentDataUp();

    if (shouldHitEndPoint()) {
        stopRender();
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

                    const event = new CustomEvent('experiment_loaded_from_endpoint',
                        {detail: dataToStorage});
                    window.dispatchEvent(event);
                }
            });
    }

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
