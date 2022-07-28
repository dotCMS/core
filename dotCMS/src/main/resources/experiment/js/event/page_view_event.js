console.log('Listener page view events in', window.location);

window.addEventListener('load', (event) => {
  console.log('Tracking page view event');
 jitsu('track', 'pageview');
});