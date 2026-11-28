import json
import unittest
import urllib.error
import urllib.request
from unittest import mock

import server_collector as sc


class ServerCollectorSecurityAndStatsTest(unittest.TestCase):
    def test_core_protected_cannot_be_controlled_even_if_in_allowlist(self):
        allow = frozenset({"mosquitto", "server-collector", "smarthome-web", "camera-recorder"})
        token = "a" * 24

        for core in ("mosquitto", "server-collector", "smarthome-web", "homeserver-mosquitto-1"):
            ok, code, msg = sc.kiem_tra_quyen_dieu_khien(
                core,
                "stop",
                enabled=True,
                token=token,
                allowed_containers=allow,
            )
            self.assertFalse(ok, f"{core} must be blocked")
            self.assertEqual(code, 403)
            self.assertIn("bảo vệ cứng", msg)

    def test_self_container_id_is_protected(self):
        ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
            "custom-name",
            "stop",
            container_id="abcdef1234567890",
            enabled=True,
            token="t" * 24,
            allowed_containers=frozenset({"custom-name"}),
            self_id="abcdef123456",
        )
        self.assertFalse(ok)
        self.assertEqual(code, 403)

    def test_fail_closed_allowlist_and_action_validation(self):
        token = "secret-token-1234567890"
        allow = frozenset({"camera-recorder", "beszel"})

        # Not in allowlist -> 403
        ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
            "go2rtc", "stop", enabled=True, token=token, allowed_containers=allow
        )
        self.assertFalse(ok)
        self.assertEqual(code, 403)

        # Short token -> 503
        ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
            "camera-recorder", "stop", enabled=True, token="short", allowed_containers=allow
        )
        self.assertFalse(ok)
        self.assertEqual(code, 503)

        # Invalid action -> 400
        ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
            "camera-recorder", "rm", enabled=True, token=token, allowed_containers=allow
        )
        self.assertFalse(ok)
        self.assertEqual(code, 400)

        # Path traversal container name -> 400
        ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
            "../images", "stop", enabled=True, token=token, allowed_containers=allow
        )
        self.assertFalse(ok)
        self.assertEqual(code, 400)

        # Allowed container + valid action -> 200
        for act in ("start", "stop", "restart"):
            ok, code, _ = sc.kiem_tra_quyen_dieu_khien(
                "camera-recorder", act, enabled=True, token=token, allowed_containers=allow
            )
            self.assertTrue(ok)
            self.assertEqual(code, 200)

    def test_tinh_stats_container_cpu_delta_and_ram_cache_subtraction(self):
        prev = {}
        # First sample (one-shot without precpu) -> cpu=None, mem computed accurately
        raw1 = {
            "cpu_stats": {
                "cpu_usage": {"total_usage": 1_000_000_000},
                "system_cpu_usage": 10_000_000_000,
                "online_cpus": 4,
            },
            "precpu_stats": {},
            "memory_stats": {
                "usage": 300 * 1024 * 1024,
                "limit": 1024 * 1024 * 1024,
                "stats": {"inactive_file": 44 * 1024 * 1024},
            },
        }
        cpu1, mem1, lim1, mpct1 = sc.tinh_stats_container("cid1", raw1, prev)
        self.assertIsNone(cpu1)
        self.assertEqual(mem1, 256.0)
        self.assertEqual(lim1, 1024.0)
        self.assertEqual(mpct1, 25.0)

        # Second sample: +0.5s CPU over +4.0s system across 4 CPUs = 50.0%
        raw2 = {
            "cpu_stats": {
                "cpu_usage": {"total_usage": 1_500_000_000},
                "system_cpu_usage": 14_000_000_000,
                "online_cpus": 4,
            },
            "precpu_stats": {},
            "memory_stats": {
                "usage": 512 * 1024 * 1024,
                "limit": 1024 * 1024 * 1024,
                "stats": {"cache": 0},
            },
        }
        cpu2, mem2, lim2, mpct2 = sc.tinh_stats_container("cid1", raw2, prev)
        self.assertEqual(cpu2, 50.0)
        self.assertEqual(mem2, 512.0)
        self.assertEqual(lim2, 1024.0)
        self.assertEqual(mpct2, 50.0)

    def test_http_control_server_requires_bearer_and_enforces_policy(self):
        token = "super-secret-control-token-2026"
        with (
            mock.patch.object(sc, "DOCKER_CONTROL_ENABLED", True),
            mock.patch.object(sc, "DOCKER_CONTROL_TOKEN", token),
            mock.patch.object(sc, "DOCKER_ALLOWED_CONTAINERS", frozenset({"camera-recorder"})),
            mock.patch.object(
                sc,
                "thuc_thi_lenh_docker",
                wraps=sc.thuc_thi_lenh_docker,
            ),
        ):
            srv = sc.tao_docker_control_server("127.0.0.1", 0)
            port = srv.server_address[1]
            import threading

            t = threading.Thread(target=srv.serve_forever, daemon=True)
            t.start()
            try:
                # 1. Missing Bearer token -> 401
                req_no_auth = urllib.request.Request(f"http://127.0.0.1:{port}/api/docker/status")
                with self.assertRaises(urllib.error.HTTPError) as ctx:
                    urllib.request.urlopen(req_no_auth)
                self.assertEqual(ctx.exception.code, 401)

                # 2. Valid Bearer token -> 200 status
                req_ok = urllib.request.Request(
                    f"http://127.0.0.1:{port}/api/docker/status",
                    headers={"Authorization": f"Bearer {token}"},
                )
                with urllib.request.urlopen(req_ok) as resp:
                    self.assertEqual(resp.status, 200)
                    body = json.loads(resp.read().decode("utf-8"))
                    self.assertTrue(body["enabled"])
                    self.assertIn("mosquitto", body["protected_containers"])
                    self.assertEqual(body["allowed_containers"], ["camera-recorder"])

                # 3. Attempting to stop core container -> 403
                req_core = urllib.request.Request(
                    f"http://127.0.0.1:{port}/api/docker/action",
                    data=json.dumps({"name": "mosquitto", "action": "stop"}).encode("utf-8"),
                    headers={
                        "Authorization": f"Bearer {token}",
                        "Content-Type": "application/json",
                    },
                    method="POST",
                )
                with self.assertRaises(urllib.error.HTTPError) as ctx2:
                    urllib.request.urlopen(req_core)
                self.assertEqual(ctx2.exception.code, 403)
            finally:
                srv.shutdown()
                srv.server_close()


if __name__ == "__main__":
    unittest.main()
