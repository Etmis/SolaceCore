# SolaceCore

SolaceCore je Paper/Bukkit plugin pro správu trestů (kick, ban, tempban, mute, tempmute, unban, unmute) s jednoduchým webovým rozhraním postaveným na Vite + React. Plugin ukládá hráče a tresty do MySQL/MariaDB, udržuje základní statistiky a poskytuje REST API pro čtení dat na webu.

– Plugin: `Plugin/SolaceCore` (Java 21, Maven, Paper 1.21.x)
– Web: `WEB/SolaceCore` (Node 20, Vite + React, Express API)


## Co umí

- Příkazy pro moderaci: /kick, /ban, /tempban, /unban, /mute, /tempmute, /unmute, a /menu
- Ukládání hráčů a trestů do MySQL/MariaDB + automatická inicializace tabulek
- Lokalizace: `en` a `cs` (volba v `config.yml`)
- REST API (Express) pro čtení hráčů, trestů, statistik a renderované „bust“ skinů s diskovou cache
- Webové UI (React) s proxy na API v režimu vývoje; produkční build přes Vite nebo Docker image (Nginx)


## Struktura repozitáře

- `Plugin/SolaceCore` – Java plugin (Maven, Paper API)
	- `src/main/resources/config.yml` – konfigurace pluginu (jazyk, DB)
	- `src/main/resources/plugin.yml` – metadata pluginu, příkazy a oprávnění
	- `start_server.bat` – jednoduchý launcher pro Paper server (očekává `server/paper.jar`)
- `WEB/SolaceCore` – web + API (Node, Vite, React)
	- `server/index.js` – Express API (+ cache skinů)
	- `Dockerfile` – produkční image webu přes Nginx


## Požadavky

- Plugin
	- Java 21
	- Maven 3.9+
	- PaperMC 1.21.x (soubor `paper.jar` v `Plugin/SolaceCore/server/`)
	- MySQL/MariaDB instance
- Web
	- Node.js 20+
	- npm 10+


## Instalace a spuštění – Plugin

1) Nakonfigurujte databázi v `Plugin/SolaceCore/src/main/resources/config.yml`:

```yaml
language: en
database:
	ip_address: "127.0.0.1"
	port: "3306"
	database_name: "solacecore"
	user: "root"
	password: ""
```

2) Připravte Paper server:
- Vytvořte složku `Plugin/SolaceCore/server/`
- Umístěte `paper.jar` (verze 1.21.x) do `Plugin/SolaceCore/server/`

3) Sestavení pluginu (Windows PowerShell):

```powershell
cd Plugin/SolaceCore
mvn clean package
```

Po buildu se výsledný JAR automaticky zkopíruje do `server/plugins/SolaceCore-1.0.0.jar`.

4) Volitelné: spuštění serveru přes Maven hook (použije `start_server.bat`):

```powershell
mvn verify
```

nebo ručně:

```powershell
cd server
./start_server.bat
```

Při prvním startu plugin automaticky vytvoří tabulky `players`, `punishments` a `operators`.


## Příkazy a oprávnění

Příkazy (usage podle `plugin.yml`):

- `/kick <player> [reason]` – permission: `solacecore.kick`
- `/ban <player> [reason]` – permission: `solacecore.ban`
- `/unban <player>` – permission: `solacecore.unban`
- `/tempban <player> <time> [reason]` – permission: `solacecore.tempban`
- `/mute <player> [reason]` – permission: `solacecore.mute`
- `/tempmute <player> <time> [reason]` – permission: `solacecore.tempmute`
- `/unmute <player>` – permission: `solacecore.unmute`
- `/menu <player>` – permission: `solacecore.menu`

Další oprávnění (ochrany): `solacecore.banprotection`, `solacecore.kickprotection`, `solacecore.muteprotection`. Ve výchozím stavu jsou všechna oprávnění `default: op`.

Časové argumenty akceptují přípony `s`, `m`, `h`, `d` (např. `10m`, `2h`, `1d`).


## Lokalizace

- Soubory jazyků: `Plugin/SolaceCore/src/main/resources/lang/en.yml` a `cs.yml`
- Volba jazyka: `language: en` nebo `language: cs` v `config.yml`


## Web + API

Vývojový režim (Vite + Express běží současně a `/api` je proxynuto na backend):

```powershell
cd WEB/SolaceCore
npm ci
npm run dev
```

Co se spustí:
- Vite dev server na http://localhost:5173
- Express API na http://localhost:3001 (skrze `server/index.js`)
- Proxy `/api` -> `http://localhost:3001` je nastavená ve `vite.config.ts`

Produkční build (statický web):

```powershell
npm run build
npm run preview
```

Docker pro web (Nginx):

```powershell
cd WEB/SolaceCore
docker build -t solacecore-web .
docker run -p 8080:80 solacecore-web
```

### Konfigurace API (server/index.js)

API používá proměnné prostředí (výchozí hodnoty v závorce):

```text
PORT (3001)
DB_HOST (127.0.0.1)
DB_PORT (3306)
DB_USER (root)
DB_PASSWORD ("")
DB_NAME (solacecore)
SKIN_TTL_DAYS (30)
ENABLE_CORS (0/1) – povolí CORS, pokud nepotřebujete Vite proxy
```

Vytvořte `.env` v `WEB/SolaceCore` dle potřeby, např.:

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

- `GET /api/health` – jednoduchý healthcheck DB
- `GET /api/players` – seznam hráčů `{ uuid, name }`
- `GET /api/players/:uuid/punishments` – tresty hráče podle UUID (např. pro modal ve webu)
- `GET /api/stats` – souhrny: `bansToday`, `totalBans`, `totalPunishments`
- `GET /api/skins/:id/bust` – PNG obrázek „bust“ skinu s diskovou cache (30 dní), `?force=1` přinucení obnovení

Pozn.: API očekává, že plugin udržuje tabulky a data v DB. Schéma je tvořeno pluginem při startu.


## Databáze (automaticky spravovaná pluginem)

Plugin při startu zajistí tabulky:
- `players(name, uuid, ipAddress, lastLogin, …)`
- `punishments(id, player_name, reason, operator, punishmentType, start, end, duration, isActive, …)`
- `operators(id, role)`

Relace: `punishments.player_name` -> `players.name` (FK, CASCADE).


## Tipy a kompatibilita

- Doporučená verze serveru: Paper 1.21.x
- Plugin kompilován pro Java 21 (mějte Java 21 i pro server)
- Pokud nasazujete web v produkci, API server (Express) spusťte odděleně – Dockerfile zde pokrývá pouze statický web (Nginx)


## Přispění

PR a issue reporty jsou vítány. Před odesláním PR prosím dodržujte stávající styl kódu a stručně popište změny.


## Licence

Zatím neurčeno. Přidejte prosím soubor `LICENSE`, jakmile bude zvolena.
