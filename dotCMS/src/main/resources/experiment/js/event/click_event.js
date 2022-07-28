console.log('Listener click events in', ${query_selector});
var elements = document.querySelectorAll('${query_selector}');

for (var i = 0; i < elements.length; i++){
    console.log("element", elements[i]);
    elements[i].addEventListener('click', (event) => {

        jitsu('track', 'click', {
          target: {
            name: event.target.name,
            class: event.target.classList,
            id: event.target.id,
            tag: event.target.tagName,
          },
        });
    });
}
