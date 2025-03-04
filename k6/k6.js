import http from "k6/http";
import { check } from "k6";

const BASE_URL = "http://localhost:8080";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ["rate>0.95"],
  },
};

export default function () {
  for (let i = 0; i < 1000; i++) {
    const res = http.get(`${BASE_URL}/api/call`);
    check(res, {
      "200 응답": (r) => r.status === 200,
    });
  }

  const statsRes = http.get(`${BASE_URL}/api/stats`);
  console.log("최종 통계:", JSON.stringify(statsRes.json(), null, 2));
}
