#!/usr/bin/env python3
"""Tests for it-suites.py on a throwaway source tree. Run: python3 scripts/it-suites.test.py"""

import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("it_suites", Path(__file__).with_name("it-suites.py"))
it = importlib.util.module_from_spec(spec)
spec.loader.exec_module(it)

JUNIT4_SUITE = """package com.dotcms;

import com.other.BTest;
import com.other.DTest;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        BTest.class,
        // a comment between entries
        DTest.class,
        com.other.FTest.class
})
public class Suite4 {
}
"""
JUNIT5_SUITE = """package com.dotcms;

import org.junit.platform.suite.api.SelectClasses;

@SelectClasses({
        ATest.class,
})
public class Suite5 {
}
"""


def test_class(pkg, name, body="@Test public void t() {}", imports="import org.junit.Test;"):
    return f"package {pkg};\n{imports}\npublic class {name} {{ {body} }}\n"


class ItSuitesTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        src = self.root / it.TEST_SRC
        files = {
            "com/dotcms/Suite4.java": JUNIT4_SUITE,
            "com/dotcms/Suite5.java": JUNIT5_SUITE,
            "com/other/BTest.java": test_class("com.other", "BTest"),
            "com/other/DTest.java": test_class("com.other", "DTest"),
            "com/other/FTest.java": test_class("com.other", "FTest"),
            "com/dotcms/ATest.java": test_class("com.dotcms", "ATest", imports="import org.junit.jupiter.api.Test;"),
            "com/fresh/CTest.java": test_class("com.fresh", "CTest"),
            "com/fresh/ZTest.java": test_class("com.fresh", "ZTest"),
            "com/fresh/JupiterTest.java": test_class("com.fresh", "JupiterTest", imports="import org.junit.jupiter.api.Test;"),
            "com/fresh/BTest.java": test_class("com.fresh", "BTest"),
            "com/fresh/Base.java": "package com.fresh;\npublic abstract class Base { @Test void t() {} }\n",
        }
        for rel, text in files.items():
            (src / rel).parent.mkdir(parents=True, exist_ok=True)
            (src / rel).write_text(text)
        (self.root / ".github").mkdir()
        (self.root / it.MATRIX).write_text('        test_class: "Suite4"\n        test_class: "Suite5"\n')
        (self.root / it.ALLOWLIST).write_text("# none\n")
        self.suite4 = src / "com/dotcms/Suite4.java"

    def tearDown(self):
        self._tmp.cleanup()

    def entries(self):
        return [w for _, w, _ in it.Suite(self.suite4).entries]

    def test_inserts_fqn_entry_before_the_next_larger_name_with_comma(self):
        it.add(self.root, "CTest", "Suite4")
        self.assertEqual(self.entries(), ["BTest", "com.fresh.CTest", "DTest", "com.other.FTest"])
        text = self.suite4.read_text()
        self.assertNotIn("import com.fresh.CTest;", text)
        self.assertIn("        com.fresh.CTest.class,\n", text)

    def test_sorting_last_appends_and_commas_the_old_last_entry(self):
        it.add(self.root, "ZTest", "Suite4")
        self.assertIn("        com.other.FTest.class,\n        com.fresh.ZTest.class,\n})", self.suite4.read_text())

    def test_unsorted_list_spreads_insertions_by_nearest_name(self):
        # Entries are B, D, F. A name below B goes before B, one between D and F goes before F:
        # different gaps, so two PRs adding them do not touch the same lines.
        (self.root / it.TEST_SRC / "com/fresh/AaTest.java").write_text(test_class("com.fresh", "AaTest"))
        (self.root / it.TEST_SRC / "com/fresh/EeTest.java").write_text(test_class("com.fresh", "EeTest"))
        it.add(self.root, "AaTest", "Suite4")
        it.add(self.root, "EeTest", "Suite4")
        self.assertEqual([e.rsplit(".", 1)[-1] for e in self.entries()], ["AaTest", "BTest", "DTest", "EeTest", "FTest"])

    def test_refuses_duplicates_and_reports_where(self):
        with self.assertRaisesRegex(SystemExit, "already registered in Suite4"):
            it.add(self.root, "com.other.BTest", "Suite4")
        with self.assertRaisesRegex(SystemExit, "already registered in Suite4"):
            it.add(self.root, "DTest", "Suite5")

    def test_refuses_junit_version_mismatch_both_ways(self):
        with self.assertRaisesRegex(SystemExit, "JUnit 5 test"):
            it.add(self.root, "JupiterTest", "Suite4")
        with self.assertRaisesRegex(SystemExit, "JUnit 4 test"):
            it.add(self.root, "CTest", "Suite5")

    def test_ambiguous_simple_name_asks_for_fqn(self):
        (self.root / it.TEST_SRC / "com/dotcms/CTest.java").write_text(test_class("com.dotcms", "CTest"))
        with self.assertRaisesRegex(SystemExit, "ambiguous"):
            it.add(self.root, "CTest", "Suite4")

    def test_check_reports_unregistered_but_skips_abstract_suites_and_allowlisted(self):
        self.assertEqual(it.unregistered(self.root),
                         ["com.fresh.BTest", "com.fresh.CTest", "com.fresh.JupiterTest", "com.fresh.ZTest"])
        (self.root / it.ALLOWLIST).write_text("com.fresh.BTest # helper\ncom.fresh.CTest\n")
        self.assertEqual(it.unregistered(self.root), ["com.fresh.JupiterTest", "com.fresh.ZTest"])

    def test_check_passes_once_registered(self):
        (self.root / it.ALLOWLIST).write_text("com.fresh.BTest\ncom.fresh.ZTest\ncom.fresh.JupiterTest\n")
        it.add(self.root, "CTest", "Suite4")
        self.assertEqual(it.unregistered(self.root), [])

    def put(self, rel, text):
        (self.root / it.TEST_SRC / rel).parent.mkdir(parents=True, exist_ok=True)
        (self.root / it.TEST_SRC / rel).write_text(text)

    def test_junit4_test_importing_jupiter_assertions_is_still_junit4(self):
        self.put("com/fresh/MixedTest.java", test_class(
            "com.fresh", "MixedTest",
            imports="import org.junit.Test;\nimport static org.junit.jupiter.api.Assertions.assertEquals;"))
        self.assertEqual(it.class_info(self.root, "MixedTest"), ("com.fresh.MixedTest", False))
        it.add(self.root, "MixedTest", "Suite4")
        with self.assertRaisesRegex(SystemExit, "JUnit 4 test"):
            it.add(self.root, "com.fresh.CTest", "Suite5")

    def test_both_or_neither_junit_annotations_are_refused(self):
        self.put("com/fresh/BothTest.java", test_class(
            "com.fresh", "BothTest", imports="import org.junit.Test;\nimport org.junit.jupiter.api.Test;"))
        self.put("com/fresh/NeitherTest.java", test_class("com.fresh", "NeitherTest", imports=""))
        with self.assertRaisesRegex(SystemExit, "imports of both"):
            it.add(self.root, "BothTest", "Suite4")
        with self.assertRaisesRegex(SystemExit, "no org.junit.Test"):
            it.add(self.root, "NeitherTest", "Suite4")

    def test_comma_goes_before_a_trailing_comment_on_the_old_last_entry(self):
        self.suite4.write_text(JUNIT4_SUITE.replace("com.other.FTest.class", "com.other.FTest.class // flaky"))
        it.add(self.root, "ZTest", "Suite4")
        self.assertIn("        com.other.FTest.class, // flaky\n        com.fresh.ZTest.class,\n})",
                      self.suite4.read_text())

    def test_unparseable_suite_shapes_fail_loudly(self):
        shapes = {
            "same line as the end": JUNIT4_SUITE.replace("com.other.FTest.class\n})", "com.other.FTest.class})"),
            "split entry": JUNIT4_SUITE.replace("com.other.FTest.class", "com.other.\n        FTest.class"),
            "two per line": JUNIT4_SUITE.replace("BTest.class,", "BTest.class, DTest.class,"),
        }
        for label, text in shapes.items():
            with self.subTest(label):
                self.suite4.write_text(text)
                with self.assertRaises(SystemExit):
                    it.Suite(self.suite4)

    def test_close_paren_inside_a_comment_does_not_end_the_block(self):
        self.suite4.write_text(JUNIT4_SUITE.replace("// a comment between entries", "// see foo({}) here"))
        self.assertEqual(len(it.Suite(self.suite4).entries), 3)

    def test_nested_abstract_class_does_not_hide_a_test_from_the_guard(self):
        self.put("com/fresh/NestedTest.java", "package com.fresh;\nimport org.junit.Test;\n"
                 "public class NestedTest {\n  private abstract static class Helper {}\n  @Test public void t() {}\n}\n")
        self.assertIn("com.fresh.NestedTest", it.unregistered(self.root))

    def test_check_fails_on_stale_allowlist_entries(self):
        (self.root / it.ALLOWLIST).write_text("com.other.BTest # now registered\ncom.fresh.Gone\ncom.fresh.CTest\n")
        self.assertEqual(it.stale_allowlist(self.root),
                         ["com.other.BTest is now registered in a suite", "com.fresh.Gone no longer exists"])


if __name__ == "__main__":
    unittest.main()
