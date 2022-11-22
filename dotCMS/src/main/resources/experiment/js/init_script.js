
let currentRunningExperimentsId = [${running_experiments_list}]

function shouldHitEndPoint() {
    let experimentData = localStorage.getItem('experiment_data');

    if (experimentData) {
        let includedExperimentIds = JSON.parse(
            experimentData).includedExperimentIds;

        return includedExperimentIds.every(element => currentRunningExperimentsId.includes(element));
    } else {
        return true;
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
        }
    });
}