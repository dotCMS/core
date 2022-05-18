import { CwFilter } from './CwFilter';
describe('Unit.api.util.CwFilter', function () {
    beforeEach(function () {});

    // filtering (hiding)

    it("An object with foo=true should be filtered out for filter='foo:false' (diff values)", function () {
        expect(CwFilter.isFiltered({ foo: true }, 'foo:false')).toEqual(true);
    });

    it("An object with name='test' should be filtered out for filter='tes*' (no wildcards allowed)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'tes*')).toEqual(true);
    });

    it("An object with foo=null should be filtered out for filter='foo:null' (null value vs string value)", function () {
        expect(CwFilter.isFiltered({ foo: null }, 'foo:null')).toEqual(true);
    });

    it("An object with foo='enabled' should be filtered out for filter='foo:true' (diff values)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled' }, 'foo:true')).toEqual(true);
    });

    it("An object with foo='enabled' and name='test' should be filtered out for filter='foo:enabled sample' (property + name)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'foo:enabled sample')).toEqual(
            true
        );
    });

    it("An object with foo='enabled' and bar='foo' and name='test' should be filtered out for filter='foo:enabled bar:foobar tes' (multiple property + name)", function () {
        expect(
            CwFilter.isFiltered(
                { foo: 'enabled', bar: 'foo', name: 'test' },
                'foo:enabled bar:foobar tes'
            )
        ).toEqual(true);
    });

    // not filtering (not hiding)
    it("An object with name='test' should NOT be filtered out for filter='test' (full name)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'test')).toEqual(false);
    });

    it("An object with name='test' should NOT be filtered out for filter='tes' (partial name)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'tes')).toEqual(false);
    });

    it("An object with name='test' should NOT be filtered out for filter='es' (partial name)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'es')).toEqual(false);
    });

    it("An object with name='test' should be filtered out for filter='TEst' (name case insensitive)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'TEst')).toEqual(false);
    });

    it("An object with foo='true' should NOT be filtered out for filter='foo:true' (same value, diff type boolean vs string)", function () {
        expect(CwFilter.isFiltered({ foo: 'true' }, 'foo:true')).toEqual(false);
    });

    it("An object with foo='enabled' should NOT be filtered out for filter='foo:enabled' (string test)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled' }, 'foo:enabled')).toEqual(false);
    });

    it("An object with foo=true should NOT be filtered out for filter='foo:true' (boolean test)", function () {
        expect(CwFilter.isFiltered({ foo: true }, 'foo:true')).toEqual(false);
    });

    it("An object with foo=1 should NOT be filtered out for filter='foo:1' (numeric test)", function () {
        expect(CwFilter.isFiltered({ foo: 1 }, 'foo:1')).toEqual(false);
    });

    it("An object with foo='1' should NOT be filtered out for filter='foo:1' (diff type string vs numeric)", function () {
        expect(CwFilter.isFiltered({ foo: '1' }, 'foo:1')).toEqual(false);
    });

    it("An object with foo='null' should NOT be filtered out for filter='foo:null' ('null' string value)", function () {
        expect(CwFilter.isFiltered({ foo: 'null' }, 'foo:null')).toEqual(false);
    });

    it("An object with foo='enabled' and name='test' should NOT be filtered out for filter='foo:enabled tes' (property + name)", function () {
        expect(CwFilter.isFiltered({ foo: 'enabled', name: 'test' }, 'foo:enabled tes')).toEqual(
            false
        );
    });

    it("An object with foo='enabled' and bar='foo' and name='test' should NOT be filtered out for filter='foo:enabled bar:foo tes' (multiple property + name)", function () {
        expect(
            CwFilter.isFiltered(
                { foo: 'enabled', bar: 'foo', name: 'test' },
                'foo:enabled bar:foo tes'
            )
        ).toEqual(false);
    });
});
