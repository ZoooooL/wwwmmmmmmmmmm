import os
import tempfile
import unittest

from scripts.verify_connections import load_env_file, redact_secret, require_env


class VerifyConnectionsTests(unittest.TestCase):
    def test_redact_secret(self):
        self.assertEqual(redact_secret("12345678"), "***")
        self.assertEqual(redact_secret("123456789"), "1234...6789")

    def test_load_env_file(self):
        with tempfile.TemporaryDirectory() as td:
            env_path = os.path.join(td, ".env")
            with open(env_path, "w", encoding="utf-8") as f:
                f.write("A=1\n#comment\nB='two'\n")

            os.environ.pop("A", None)
            os.environ.pop("B", None)

            load_env_file(env_path)

            self.assertEqual(os.getenv("A"), "1")
            self.assertEqual(os.getenv("B"), "two")

    def test_require_env_missing(self):
        os.environ.pop("MISSING_TEST_VAR", None)
        with self.assertRaises(RuntimeError):
            require_env("MISSING_TEST_VAR")


if __name__ == "__main__":
    unittest.main()
