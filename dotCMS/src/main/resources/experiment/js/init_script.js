
let currentRunningExperimentsId = [${running_experiments_list}]

function shouldHitEndPoint() {
    let experimentData = localStorage.getItem('experiment_data');
    if (experimentData) {
        let includedExperimentIds = JSON.stringify(
            experimentData).includedExperimentIds;
        return includedExperimentIds.every(element => {
        return currentRunningExperimentsId.includes(element);
        });
    } else {
        return false;
    }
}

let shouldHitEndPoint = shouldHitEndPoint();

if (!shouldHitEndPoint) {
    fetch('/api/v1/experiment')
        .then(response => response.json())
        .then(data => {
            if (data.experiments) {
                let dataToStorage = Object.assign({}, data);
                delete dataToStorage['excludedExperimentIds'];
                dataToStorage['includedExperimentIds'] = [
                    ...dataToStorage['includedExperimentIds'],
                    ...dataToStorage['excludedExperimentIds']
                ];
                localStorage.setItem('experiment_data', JSON.stringify(dataToStorage));
            }
        });
}