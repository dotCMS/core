var isInExperiment = document.cookie.includes('runningExperiment');

if (!isInExperiment) {
    fetch('/api/v1/experiment')
    .then(response => response.json())
    .then(data => {
        if (data.experiment) {
            localStorage.setItem('experiment_data', JSON.stringify(data));
        }
    });
}