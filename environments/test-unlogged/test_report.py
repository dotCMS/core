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
