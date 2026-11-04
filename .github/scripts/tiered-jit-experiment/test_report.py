import tempfile
import unittest
from pathlib import Path
from report import summarize


class ReportTest(unittest.TestCase):
    def test_nonzero_counts_elapsed_failures_and_retries(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp)
            (path / 'failsafe-summary.xml').write_text(
                '<failsafe-summary><completed>3</completed><skipped>1</skipped>'
                '<failures>1</failures><errors>0</errors></failsafe-summary>')
            (path / 'TEST-fixture.xml').write_text(
                '<testsuite tests="3" time="2.5"><testcase><flakyFailure/>'
                '<rerunFailure/></testcase></testsuite>')
            self.assertEqual(summarize(path), dict(completed=3, skipped=1, failures=1,
                                                  errors=0, xml_test_seconds=2.5, retries=2))

    def test_surefire_counts_without_failsafe_summary(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp)
            (path / 'TEST-fixture.xml').write_text(
                '<testsuites><testsuite tests="5" failures="1" errors="0" skipped="1" time="3">'
                '<testcase><flakyFailure/></testcase></testsuite>'
                '<testsuite tests="2" failures="0" errors="1" skipped="0" time="2"/></testsuites>')
            self.assertEqual(summarize(path), dict(completed=7, skipped=1, failures=1,
                                                  errors=1, xml_test_seconds=5.0, retries=1))

    def test_missing_reports_are_invalid(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, 'No executed tests'):
                summarize(temp)

    def test_zero_or_all_skipped_is_invalid(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp)
            for completed, skipped in [(0, 0), (2, 2)]:
                (path / 'failsafe-summary.xml').write_text(
                    f'<failsafe-summary><completed>{completed}</completed>'
                    f'<skipped>{skipped}</skipped></failsafe-summary>')
                with self.assertRaisesRegex(ValueError, 'No executed tests'):
                    summarize(path)


if __name__ == '__main__':
    unittest.main()
