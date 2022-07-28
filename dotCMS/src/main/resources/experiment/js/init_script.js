var experiment_data = localStorage.getItem('experiment_data');
console.log('experiment_data in localStorage', experiment_data);
var isInExperiment = document.cookie.includes('lookBackWindowCookie');

if (isInExperiment && !!experiment_data && window.location.href.includes(JSON.parse(experiment_data).url)){
        console.log('Trigger init event...');
        const event = new CustomEvent('init_custom', { detail: JSON.parse(experiment_data) });
        window.dispatchEvent(event);
} else if (!experiment_data) {
        console.log('Getting init data...');
        fetch('http://localhost:8080/api/v1/experiment')
          .then(response => response.json())
          .then(data => {
             console.log('You are in a experiment: ', data);
              if (data.experiment) {
                localStorage.setItem('experiment_data', JSON.stringify(data));

                console.log('Trigger init event...');
                const event = new CustomEvent('init_custom', { detail: data });
                window.dispatchEvent(event);
              }
           });
}