# SolaceCore

SolaceCore is a Paper/Bukkit moderation plugin (kick, ban, tempban, mute, tempmute, unban, unmute) with a simple Vite + React web UI. The plugin stores players and punishments in MySQL/MariaDB, keeps basic stats, and exposes a REST API for the web.

– Plugin: `Plugin/SolaceCore` (Java 21, Maven, Paper 1.21.x)
– Web: `WEB/SolaceCore` (Node 20, Vite + React, Express API)

Language: English | Čeština (see `README.md`)


## Features

- Moderation commands: /kick, /ban, /tempban, /unban, /mute, /tempmute, /unmute, and /menu
- MySQL/MariaDB persistence for players and punishments with automatic schema initialization
- Localization: `en` and `cs` (choose in `config.yml`)
- REST API (Express) to read players, punishments, stats, and cached skin “bust” images
- Web UI (React) with a dev proxy to the API; production build via Vite or Docker (Nginx)


## Repository structure

- `Plugin/SolaceCore` – Java plugin (Maven, Paper API)
  - `src/main/resources/config.yml` – plugin config (language, DB)
  - `src/main/resources/plugin.yml` – plugin metadata, commands, permissions
  - `start_server.bat` – simple launcher for the Paper server (expects `server/paper.jar`)
- `WEB/SolaceCore` – web + API (Node, Vite, React)
  - `server/index.js` – Express API (+ skin cache)
  - `Dockerfile` – production image for the web (Nginx)


## Requirements

- Plugin
  - Java 21
  - Maven 3.9+
  - PaperMC 1.21.x (`paper.jar` in `Plugin/SolaceCore/server/`)
  - MySQL/MariaDB instance
- Web
  - Node.js 20+
  - npm 10+


## Plugin: build and run

1) Configure your database in `Plugin/SolaceCore/src/main/resources/config.yml`:

```yaml
language: en
database:
  ip_address: "127.0.0.1"
  port: "3306"
  database_name: "solacecore"
  user: "root"
  password: ""
```

2) Prepare the Paper server:
- Create `Plugin/SolaceCore/server/`
- Put `paper.jar` (1.21.x) into `Plugin/SolaceCore/server/`

3) Build the plugin (Windows PowerShell):

```powershell
cd Plugin/SolaceCore
mvn clean package
```

After the build, the resulting JAR is copied to `server/plugins/SolaceCore-1.0.0.jar` via the Maven antrun step.

4) Optional: start the server through the Maven verify hook (uses `start_server.bat`):

```powershell
mvn verify
```

or manually:

```powershell
cd server
./start_server.bat
```

On first launch, the plugin automatically creates tables `players`, `punishments`, and `operators`.


## Commands and permissions

Commands (per `plugin.yml`):

- `/kick <player> [reason]` – permission: `solacecore.kick`
- `/ban <player> [reason]` – permission: `solacecore.ban`
- `/unban <player>` – permission: `solacecore.unban`
- `/tempban <player> <time> [reason]` – permission: `solacecore.tempban`
- `/mute <player> [reason]` – permission: `solacecore.mute`
- `/tempmute <player> <time> [reason]` – permission: `solacecore.tempmute`
- `/unmute <player>` – permission: `solacecore.unmute`
- `/menu <player>` – permission: `solacecore.menu`

Additional permissions (protections): `solacecore.banprotection`, `solacecore.kickprotection`, `solacecore.muteprotection`. By default, all permissions are `default: op`.

Time arguments support suffixes `s`, `m`, `h`, `d` (e.g., `10m`, `2h`, `1d`).


## Localization

- Language files: `Plugin/SolaceCore/src/main/resources/lang/en.yml` and `cs.yml`
- Choose language: `language: en` or `language: cs` in `config.yml`


## Web + API

Dev mode (Vite + Express run concurrently; `/api` is proxied to the backend):

```powershell
cd WEB/SolaceCore
npm ci
npm run dev
```

What starts:
- Vite dev server at http://localhost:5173
- Express API at http://localhost:3001 (`server/index.js`)
- Proxy `/api` -> `http://localhost:3001` configured in `vite.config.ts`

Production build (static web):

```powershell
npm run build
npm run preview
```

Docker for the web (Nginx):

```powershell
cd WEB/SolaceCore
docker build -t solacecore-web .
docker run -p 8080:80 solacecore-web
```

### API configuration (server/index.js)

API reads environment variables (defaults in parentheses):

```text
PORT (3001)
DB_HOST (127.0.0.1)
DB_PORT (3306)
DB_USER (root)
DB_PASSWORD ("")
DB_NAME (solacecore)
SKIN_TTL_DAYS (30)
ENABLE_CORS (0/1) – enable CORS if you are not using the Vite proxy
```

Example `.env` in `WEB/SolaceCore`:

```env
PORT=3001
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=solacecore
ENABLE_CORS=0
```

### API endpoints

- `GET /api/health` – DB healthcheck
- `GET /api/players` – list of players `{ uuid, name }`
- `GET /api/players/:uuid/punishments` – player punishments by UUID (used by the web modal)
- `GET /api/stats` – aggregates: `bansToday`, `totalBans`, `totalPunishments`
- `GET /api/skins/:id/bust` – PNG skin “bust” with on-disk cache (30 days), `?force=1` to force refresh

Note: The API expects the plugin to maintain the DB schema and data. The schema is created by the plugin at startup.


## Database (managed by the plugin)

The plugin creates on startup:
- `players(name, uuid, ipAddress, lastLogin, …)`
- `punishments(id, player_name, reason, operator, punishmentType, start, end, duration, isActive, …)`
- `operators(id, role)`

Relation: `punishments.player_name` -> `players.name` (FK, CASCADE).


## Tips and compatibility

- Recommended server: Paper 1.21.x
- Compiled for Java 21 (use Java 21 for the server)
- For production, run the API server (Express) separately; the provided Dockerfile packages only the static web (Nginx)


## Contributing

Issues and PRs are welcome. Please follow the existing code style and include a short description of your changes.


## License

TBD. Please add a `LICENSE` file once a license is chosen.
