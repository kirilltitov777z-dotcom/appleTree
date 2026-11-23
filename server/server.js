/*
 * AppleTree server — общий мир для игры.
 * Без зависимостей: только Node.js 18+ (http, crypto, fs).
 *
 * Запуск:  PORT=8080 node server.js
 * Данные:  server/data/db.json
 */
const http = require("http");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const PORT = Number(process.env.PORT || 8080);
const DATA_DIR = path.join(__dirname, "data");
const DB_FILE = path.join(DATA_DIR, "db.json");

const COLORS = [
  "#E07A5F", "#7FB069", "#5C8DBC", "#C9A227", "#9B7EBD",
  "#4E9A8E", "#D08C60", "#8A9A5B", "#B56576", "#6C8EBF",
];
const EMBLEMS = ["🍎", "🌿", "🌳", "☀", "🐝", "🦌", "🕊", "🌊", "🍂", "⛰"];

function defaultRegions() {
  return [
    ["north", "Северный сад", 30, 18, 13],
    ["pine", "Сосновый край", 62, 15, 11],
    ["lake", "Озёрный берег", 82, 30, 12],
    ["valley", "Тихая долина", 18, 40, 14],
    ["orchard", "Большой сад", 45, 38, 15],
    ["hills", "Яблочные холмы", 72, 48, 13],
    ["river", "Речные луга", 25, 62, 14],
    ["field", "Пшеничные поля", 52, 63, 13],
    ["coast", "Тёплый берег", 80, 68, 12],
    ["south", "Южная роща", 33, 82, 12],
    ["meadow", "Медовый луг", 60, 86, 13],
    ["cape", "Мыс Ветров", 86, 87, 10],
  ].map(([id, name, x, y, r]) => ({
    id, name, x, y, r,
    ownerId: null, ownerName: null, ownerColor: null,
    progressCountryId: null, progress: 0,
  }));
}

let db = { users: {}, countries: [], applications: [], regions: defaultRegions() };

function load() {
  try {
    const raw = fs.readFileSync(DB_FILE, "utf8");
    const parsed = JSON.parse(raw);
    db.users = parsed.users || {};
    db.countries = parsed.countries || [];
    db.applications = parsed.applications || [];
    db.regions = parsed.regions && parsed.regions.length ? parsed.regions : defaultRegions();
    // страховка от старых данных
    for (const c of db.countries) {
      if (!Array.isArray(c.membersList)) c.membersList = c.leader ? [c.leader] : [];
      c.members = c.membersList.length || c.members || 1;
    }
  } catch {
    save();
  }
}

let saveTimer = null;
function save() {
  if (saveTimer) return;
  saveTimer = setTimeout(() => {
    saveTimer = null;
    fs.mkdirSync(DATA_DIR, { recursive: true });
    const tmp = DB_FILE + ".tmp";
    fs.writeFileSync(tmp, JSON.stringify(db, null, 1));
    fs.renameSync(tmp, DB_FILE);
  }, 300);
}

// ---------- утилиты ----------
const sha256 = (s) => crypto.createHash("sha256").update(s).digest("hex");
const newSalt = () => crypto.randomBytes(16).toString("hex");
const newToken = () => crypto.randomBytes(24).toString("hex");
const newId = (p) => p + "_" + crypto.randomBytes(6).toString("hex");
const nickKey = (n) => n.trim().toLowerCase();

function send(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Cache-Control": "no-store",
  });
  res.end(body);
}

function fail(res, code, error) {
  send(res, code, { error });
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on("data", (c) => {
      size += c.length;
      if (size > 64 * 1024) {
        reject(new Error("Слишком большой запрос"));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on("end", () => {
      if (!chunks.length) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")));
      } catch {
        reject(new Error("Некорректный JSON"));
      }
    });
    req.on("error", reject);
  });
}

function authUser(req) {
  const h = req.headers.authorization || "";
  const token = h.startsWith("Bearer ") ? h.slice(7).trim() : "";
  if (!token) return null;
  const user = Object.values(db.users).find((u) => u.token === token);
  return user || null;
}

function publicCountry(c) {
  return {
    id: c.id,
    name: c.name,
    color: c.color,
    emblem: c.emblem,
    leader: c.leader,
    members: c.members,
    score: c.score,
    regions: c.regions,
    createdAt: c.createdAt,
  };
}

function recomputeCountryScores() {
  const scores = new Map();
  for (const u of Object.values(db.users)) {
    if (u.countryId) scores.set(u.countryId, (scores.get(u.countryId) || 0) + (u.score || 0));
  }
  for (const c of db.countries) {
    const ownerScore = db.regions.filter((r) => r.ownerId === c.id).length * 250;
    c.score = (scores.get(c.id) || 0) + ownerScore;
    c.regions = db.regions.filter((r) => r.ownerId === c.id).length;
    c.members = c.membersList.length || 1;
  }
}

function countryById(id) {
  return db.countries.find((c) => c.id === id) || null;
}

function myCountryOf(user) {
  return user.countryId ? countryById(user.countryId) : null;
}

// ---------- обработчики ----------
async function handle(req, res) {
  const url = new URL(req.url, "http://localhost");
  const p = url.pathname.replace(/\/+$/, "") || "/";
  const method = req.method;

  if (method === "OPTIONS") return send(res, 204, {});

  if (method === "GET" && (p === "/" || p === "/api")) {
    return send(res, 200, {
      ok: true,
      name: "AppleTree server",
      players: Object.keys(db.users).length,
      countries: db.countries.length,
      regions: db.regions.length,
    });
  }

  if (method === "GET" && p === "/api/ping") return send(res, 200, { ok: true, time: Date.now() });

  if (method === "POST" && p === "/api/register") {
    const body = await readBody(req);
    const nickname = String(body.nickname || "").trim();
    const password = String(body.password || "");
    if (!/^[\wА-Яа-яЁё -]{3,16}$/u.test(nickname)) return fail(res, 400, "Ник: 3–16 символов, буквы/цифры");
    if (password.length < 4) return fail(res, 400, "Пароль минимум 4 символа");
    if (db.users[nickKey(nickname)]) return fail(res, 409, "Такой ник уже занят");
    const salt = newSalt();
    const user = {
      nickname,
      salt,
      hash: sha256(salt + password),
      token: newToken(),
      countryId: null,
      score: 0,
      captured: 0,
      createdAt: Date.now(),
    };
    db.users[nickKey(nickname)] = user;
    save();
    return send(res, 200, { token: user.token, nickname: user.nickname });
  }

  if (method === "POST" && p === "/api/login") {
    const body = await readBody(req);
    const nickname = String(body.nickname || "").trim();
    const password = String(body.password || "");
    const user = db.users[nickKey(nickname)];
    if (!user || user.hash !== sha256(user.salt + password)) return fail(res, 401, "Неверный ник или пароль");
    user.token = newToken();
    save();
    return send(res, 200, { token: user.token, nickname: user.nickname });
  }

  // всё дальше требует токен
  const user = authUser(req);
  if (!user) return fail(res, 401, "Нужно войти");

  if (method === "GET" && p === "/api/world") {
    recomputeCountryScores();
    const myCountry = myCountryOf(user);
    const isLeader = !!(myCountry && myCountry.leader === user.nickname);
    const applications = isLeader
      ? db.applications
          .filter((a) => a.countryId === myCountry.id)
          .map((a) => ({ ...a, countryName: myCountry.name }))
      : db.applications
          .filter((a) => a.nickname === user.nickname)
          .map((a) => ({ ...a, countryName: countryById(a.countryId)?.name || "" }));
    const leaderboard = db.countries
      .slice()
      .sort((a, b) => b.score - a.score)
      .slice(0, 50)
      .map(publicCountry);
    return send(res, 200, {
      countries: db.countries.map(publicCountry),
      regions: db.regions,
      applications,
      leaderboard,
      meCountryId: user.countryId,
      meIsLeader: isLeader,
    });
  }

  if (method === "POST" && p === "/api/countries") {
    const body = await readBody(req);
    const name = String(body.name || "").trim();
    const color = String(body.color || COLORS[0]);
    const emblem = String(body.emblem || "🍎");
    if (name.length < 2 || name.length > 20) return fail(res, 400, "Название: 2–20 символов");
    if (db.countries.some((c) => c.name.toLowerCase() === name.toLowerCase()))
      return fail(res, 409, "Страна с таким названием уже есть");
    if (user.countryId) return fail(res, 400, "Сначала выйди из текущей страны");
    const country = {
      id: newId("c"),
      name,
      color: /^#[0-9a-fA-F]{6}$/.test(color) ? color : COLORS[0],
      emblem: EMBLEMS.includes(emblem) ? emblem : "🍎",
      leader: user.nickname,
      membersList: [user.nickname],
      members: 1,
      score: 0,
      regions: 0,
      createdAt: Date.now(),
    };
    db.countries.push(country);
    user.countryId = country.id;
    save();
    return send(res, 200, { country: publicCountry(country) });
  }

  let m = p.match(/^\/api\/countries\/([\w-]+)\/join$/);
  if (method === "POST" && m) {
    const country = countryById(m[1]);
    if (!country) return fail(res, 404, "Страна не найдена");
    if (user.countryId) return fail(res, 400, "Сначала выйди из текущей страны");
    if (db.applications.some((a) => a.countryId === country.id && a.nickname === user.nickname))
      return fail(res, 409, "Заявка уже отправлена");
    db.applications.push({
      id: newId("a"),
      countryId: country.id,
      nickname: user.nickname,
      createdAt: Date.now(),
    });
    save();
    return send(res, 200, { ok: true });
  }

  m = p.match(/^\/api\/applications\/([\w-]+)\/(accept|decline)$/);
  if (method === "POST" && m) {
    const app = db.applications.find((a) => a.id === m[1]);
    if (!app) return fail(res, 404, "Заявка не найдена");
    const country = countryById(app.countryId);
    if (!country) return fail(res, 404, "Страна не найдена");
    if (country.leader !== user.nickname) return fail(res, 403, "Принимать заявки может только лидер");
    const applicant = db.users[nickKey(app.nickname)];
    if (m[2] === "accept") {
      if (applicant && !applicant.countryId) {
        applicant.countryId = country.id;
        if (!country.membersList.includes(applicant.nickname)) country.membersList.push(applicant.nickname);
      }
    }
    db.applications = db.applications.filter((a) => a.id !== app.id);
    save();
    return send(res, 200, { ok: true });
  }

  if (method === "POST" && p === "/api/leave") {
    const country = myCountryOf(user);
    if (!country) return fail(res, 400, "Ты не в стране");
    country.membersList = country.membersList.filter((n) => n !== user.nickname);
    if (country.membersList.length === 0) {
      db.countries = db.countries.filter((c) => c.id !== country.id);
      db.applications = db.applications.filter((a) => a.countryId !== country.id);
      for (const r of db.regions) {
        if (r.ownerId === country.id) {
          r.ownerId = null;
          r.ownerName = null;
          r.ownerColor = null;
          r.progress = 0;
          r.progressCountryId = null;
        }
      }
    } else if (country.leader === user.nickname) {
      country.leader = country.membersList[0];
    }
    user.countryId = null;
    save();
    return send(res, 200, { ok: true });
  }

  if (method === "POST" && p === "/api/capture") {
    const body = await readBody(req);
    const region = db.regions.find((r) => r.id === body.regionId);
    if (!region) return fail(res, 404, "Регион не найден");
    const country = myCountryOf(user);
    if (!country) return fail(res, 400, "Сначала вступи в страну");
    if (region.ownerId === country.id) return fail(res, 400, "Это уже твоя земля");
    const amount = Math.max(1, Math.min(10_000_000, Number(body.amount) || 0));
    if (!amount) return fail(res, 400, "Неверная ставка");

    if (region.progressCountryId !== country.id) {
      region.progressCountryId = country.id;
      region.progress = 0;
    }
    const step = 12;
    region.progress = Math.min(100, region.progress + step);
    let captured = false;
    if (region.progress >= 100) {
      region.ownerId = country.id;
      region.ownerName = country.name;
      region.ownerColor = country.color;
      region.progress = 0;
      region.progressCountryId = null;
      user.captured = (user.captured || 0) + 1;
      captured = true;
    }
    recomputeCountryScores();
    save();
    return send(res, 200, {
      ok: true,
      captured,
      progress: region.progress,
      message: captured
        ? `${region.name} теперь часть ${country.name}!`
        : `Захват: ${region.progress}%`,
    });
  }

  if (method === "POST" && p === "/api/progress") {
    const body = await readBody(req);
    user.score = Math.max(0, Math.min(1e15, Number(body.score) || 0));
    user.captured = Math.max(0, Math.min(1e9, Number(body.captured) || 0));
    recomputeCountryScores();
    save();
    return send(res, 200, { ok: true });
  }

  if (method === "GET" && p === "/api/leaderboard") {
    recomputeCountryScores();
    return send(res, 200, {
      leaderboard: db.countries
        .slice()
        .sort((a, b) => b.score - a.score)
        .slice(0, 50)
        .map(publicCountry),
    });
  }

  return fail(res, 404, "Не найдено");
}

load();
const server = http.createServer((req, res) => {
  handle(req, res).catch((e) => {
    if (!res.headersSent) fail(res, 400, e.message || "Ошибка запроса");
  });
});
server.listen(PORT, () => {
  console.log(`AppleTree server: http://0.0.0.0:${PORT}`);
});
