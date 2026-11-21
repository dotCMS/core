dojo.require('dijit.form.DateTextBox');

// Keep the legacy forms' M/d/yyyy submission contract rather than Dojo's ISO default.
function initializeLegacyDatePicker(inputId) {
    var input = dojo.byId(inputId);
    if (!input || dijit.byId(inputId)) {
        return;
    }
    var parts = input.value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    var date = parts ? new Date(Number(parts[3]), Number(parts[1]) - 1, Number(parts[2])) : null;
    var year = new Date().getFullYear();
    new dijit.form.DateTextBox({
        name: input.name,
        value: date,
        constraints: {
            datePattern: 'M/d/yyyy',
            min: new Date(year - 90, 0, 1),
            max: new Date(year + 10, 11, 31)
        },
        serialize: function (value) {
            return value && !isNaN(value.getTime()) ? (value.getMonth() + 1) + '/' + value.getDate() + '/' + value.getFullYear() : '';
        }
    }, input);
}
