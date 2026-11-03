/* BTR 本地前端静态服务器：静态托管 frontend/，并将 /api 与 /uploads 代理到后端 8080 */
const http = require("http");
const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "..", "frontend");
const BACKEND = { host: "127.0.0.1", port: 8080 };
const PORT = Number(process.env.PORT || 5173);

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".webp": "image/webp",
  ".ico": "image/x-icon",
  ".woff2": "font/woff2",
};

function proxy(req, res) {
  const upstream = http.request(
    {
      host: BACKEND.host,
      port: BACKEND.port,
      method: req.method,
      path: req.url,
      headers: { ...req.headers, host: BACKEND.host + ":" + BACKEND.port },
    },
    (up) => {
      res.writeHead(up.statusCode || 502, up.headers);
      up.pipe(res);
    }
  );
  upstream.on("error", () => {
    res.writeHead(502, { "Content-Type": "application/json; charset=utf-8" });
    res.end(JSON.stringify({ code: 502, message: "后端未启动（http://localhost:8080）", data: null }));
  });
  req.pipe(upstream);
}

const server = http.createServer((req, res) => {
  const url = decodeURIComponent((req.url || "/").split("?")[0]);
  if (url.startsWith("/api/") || url.startsWith("/uploads/")) return proxy(req, res);

  let filePath = path.join(ROOT, url === "/" ? "index.html" : url);
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403).end("Forbidden");
    return;
  }
  if (!path.extname(filePath)) filePath += ".html";

  fs.stat(filePath, (err, stat) => {
    if (err || !stat.isFile()) {
      res.writeHead(404, { "Content-Type": "text/html; charset=utf-8" });
      res.end("<h1>404</h1><p>页面不存在</p>");
      return;
    }
    res.writeHead(200, {
      "Content-Type": MIME[path.extname(filePath).toLowerCase()] || "application/octet-stream",
      "Cache-Control": "no-cache",
    });
    fs.createReadStream(filePath).pipe(res);
  });
});

server.listen(PORT, () => {
  console.log(`BTR frontend  -> http://localhost:${PORT}`);
  console.log(`API proxy     -> http://localhost:${BACKEND.port}/api/v1`);
});
