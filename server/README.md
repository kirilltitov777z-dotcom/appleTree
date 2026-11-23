# AppleTree server

Маленький сервер для игры AppleTree: регистрация, страны, заявки, карта и лидерборд.
Без npm-зависимостей — нужен только Node.js 18+.

## Запуск

```bash
cd server
npm start          # или: PORT=8080 node server.js
```

Проверка: `http://localhost:8080/api/ping` → `{"ok":true,...}`

Данные лежат в `server/data/db.json` (создаётся сам, в git не попадает).

## Куда поставить

- **VPS / домашний ПК**: `node server.js`, снаружи — по IP и порту.
- **Render / Railway / Fly.io**: добавь репозиторий как Web Service,
  корень — `server`, команда запуска — `npm start`.
- **Docker**: в папке есть `Dockerfile`:

```bash
docker build -t appletree-server ./server
docker run -p 8080:8080 -v appletree-data:/app/data appletree-server
```

## Адрес в игре

В приложении на экране входа включи «Играть по сети» и впиши свой адрес,
например `https://appletree-server.onrender.com` (без слэша на конце).
Потом проверь в «Профиль» → «Проверить сервер».

## API

| Метод | Путь | Что делает |
|---|---|---|
| GET | `/api/ping` | проверка связи |
| POST | `/api/register` | ник + пароль → токен |
| POST | `/api/login` | вход → токен |
| GET | `/api/world` | страны, регионы, заявки, лидерборд |
| POST | `/api/countries` | основать страну |
| POST | `/api/countries/:id/join` | заявка на вступление |
| POST | `/api/applications/:id/accept` | принять (лидер) |
| POST | `/api/applications/:id/decline` | отклонить (лидер) |
| POST | `/api/leave` | выйти из страны |
| POST | `/api/capture` | вложить яблоки в регион |
| POST | `/api/progress` | отправить свой счёт |
| GET | `/api/leaderboard` | топ стран |

Авторизация — заголовок `Authorization: Bearer <token>`.
