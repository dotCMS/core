// Run with: node scripts/test-legacy-ui-removal.cjs [--runtime]
// For --runtime first resolve the classpath with:
// ./mvnw dependency:build-classpath -pl :dotcms-core -DincludeScope=runtime -Dmdep.outputFile=target/legacy-runtime-classpath.txt
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const web = 'dotCMS/src/main/webapp/';
for (const removed of ['html/js/calendar', 'html/js/diff', 'html/common/top_calendar_inc.jsp']) {
    assert.equal(fs.existsSync(path.join(root, web, removed)), false, removed);
}
for (const pom of ['dotCMS/pom.xml', 'bom/application/pom.xml']) {
    assert.doesNotMatch(read(pom), /<artifactId>dot\.(jazzy-core|xpp3-min)<\/artifactId>/);
    assert.match(read(pom), /<artifactId>dot\.woodstox-core-lgpl<\/artifactId>/);
}
function scan(directory) {
    for (const entry of fs.readdirSync(path.join(root, directory), { withFileTypes: true })) {
        const file = `${directory}/${entry.name}`;
        if (entry.isDirectory()) scan(file);
        else if (/\.(jsp|js|xml|xsl|vtl)$/.test(file)) {
            assert.doesNotMatch(read(file), /\/html\/js\/(calendar|diff)\/|calendarOnClick_|htmlDiffInit|wz_tooltip/, file);
        }
    }
}
scan('dotCMS/src/main/webapp');
scan('dotCMS/src/main/resources');
const widgets = new Map();
const fields = new Map([
    ['birthday', { value: '2/29/2000', name: 'birthday' }],
    ['empty', { value: '', name: 'empty' }]
]);
const context = {
    Date,
    dojo: { require() {}, byId: (id) => fields.get(id) },
    dijit: {
        byId: (id) => widgets.get(id),
        form: {
            DateTextBox: function (options, field) {
                this.options = options;
                this.name = options.name;
                widgets.set([...fields].find(([, value]) => value === field)[0], this);
            }
        }
    }
};
vm.runInNewContext(read(`${web}html/js/legacy-date-picker.js`), context);
context.initializeLegacyDatePicker('birthday');
const birthday = widgets.get('birthday');
assert.equal(birthday.name, 'birthday');
assert.equal(birthday.options.value.getFullYear(), 2000);
assert.equal(birthday.options.value.getMonth(), 1);
assert.equal(birthday.options.value.getDate(), 29);
assert.equal(birthday.options.serialize(new Date(2000, 1, 29)), '2/29/2000');
assert.equal(birthday.options.serialize(null), '');
assert.equal(birthday.options.serialize(new Date('invalid')), '');
context.initializeLegacyDatePicker('birthday');
assert.equal(widgets.get('birthday'), birthday, 'initialization is idempotent');
context.initializeLegacyDatePicker('empty');
assert.equal(widgets.get('empty').options.value, null);
context.initializeLegacyDatePicker('missing');
for (const [page, ids] of [
    ['usermanager/register_user.jsp', ['dateOfBirth']],
    ['usermanager/view.jsp', ['lastLoginDateFrom', 'lastLoginDateTo', 'createdDateFrom', 'createdDateTo', 'lastVisitDateFrom', 'lastVisitDateTo', 'dateOfBirthFrom', 'dateOfBirthTo', 'dateOfBirthSince']]
]) {
    const source = read(`${web}html/portlet/ext/${page}`);
    for (const id of ids) {
        assert.ok(source.includes(`initializeLegacyDatePicker('${id}')`), id);
        assert.ok(source.includes(`dijit.byId('${id}').toggleDropDown()`), id);
    }
}
assert.match(read(`${web}html/portlet/ext/usermanager/search_form_inc.jsp`), /function doSearchSubmit\(\)/);
assert.match(read('dotCMS/src/main/java/com/dotcms/util/xstream/XStreamHandler.java'), /new XStream\(new DomDriver\(encoding\)\)/);
console.log('PASS: legacy assets/dependencies absent, WoodStox retained, picker values and wiring preserved');
if (process.argv.includes('--runtime')) {
    const { spawnSync } = require('node:child_process');
    const classpath = read('dotCMS/target/legacy-runtime-classpath.txt').trim();
    assert.doesNotMatch(classpath, /\/(?:dot\.)?(?:jazzy-core|xpp3-min|xpp3_min)\//);
    const result = spawnSync('java', ['--class-path', classpath, 'scripts/LegacyXmlRuntimeCheck.java'], {
        cwd: root,
        stdio: 'inherit',
        timeout: 30000
    });
    assert.ifError(result.error);
    assert.equal(result.status, 0, 'XML runtime smoke check');
}
