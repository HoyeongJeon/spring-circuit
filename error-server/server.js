const http = require("http");

let isErrorMode = true;

// 10초마다 상태 전환
setInterval(() => {
  isErrorMode = !isErrorMode;
  console.log(`Server is now in ${isErrorMode ? "error mode" : "normal mode"}`);
}, 10000);

const server = http.createServer((req, res) => {
  // 요청 경로가 '/api/random-error'가 아니면 404 반환
  if (req.url !== "/api/random-error") {
    res.writeHead(404, { "Content-Type": "text/plain" });
    return res.end("Not Found");
  }

  // 10~20ms 지연
  const delay = Math.floor(Math.random() * 11) + 20;

  setTimeout(() => {
    if (isErrorMode) {
      const random = Math.random();
      if (random < 0.2) {
        // 20% 확률로 500 에러
        res.writeHead(500, { "Content-Type": "text/plain" });
        return res.end("Internal Server Error");
      } else if (random < 0.3) {
        // 10% 확률로 400 에러
        res.writeHead(400, { "Content-Type": "text/plain" });
        return res.end("Bad Request");
      }
    }

    // 정상 응답
    res.writeHead(200, { "Content-Type": "text/plain" });
    res.end("Hello, world!");
  }, delay);
});

const PORT = 10001;
server.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
