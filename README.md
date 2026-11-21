# Backend Docker setup

## Publish the backend image

Start Docker Desktop and install JDK 21 on the build machine. Replace `YOUR_ACCOUNT` with your Docker Hub account and run from the source directory:

```powershell
docker login
.\gradlew.bat bootJar
$env:BACKEND_IMAGE = 'YOUR_ACCOUNT/be-backend:1.0.0'
docker build -t $env:BACKEND_IMAGE .
docker push $env:BACKEND_IMAGE
```

## Run from three configuration files

Give the recipient `compose.yaml`, `compose.backend.yaml` and `.env.example`. Copy `.env.example` to `.env` in the same directory. Set `BACKEND_IMAGE` to the published image tag, `APP_JWT_SECRET` to a random secret of at least 32 bytes, and `POSTGRES_PASSWORD` to a strong password. Keep `.env` private. For a private image, sign in to the registry with an account that can pull it. From that directory, run:

```powershell
docker compose -f compose.yaml -f compose.backend.yaml up -d --no-build --wait
```

The health endpoint is `http://localhost:8080/actuator/health` by default. Set `BACKEND_PORT` in `.env` to use a different host port. To update an existing backend while leaving its running PostgreSQL container in place, add `--no-deps backend` to the Compose command.

Flyway applies migrations on startup. A fresh stack creates its own database. PostgreSQL uses a Docker-managed anonymous volume. Use `docker compose -f compose.yaml -f compose.backend.yaml stop` to pause both services. Back up the database before removing containers. Use a reverse proxy on the same host to give remote clients access to the backend.

## Reset the local database

The backend initializes an empty database from `src/main/resources/db/migration/V1__init.sql`. The consolidated file replaces the former V1–V5 history. For a database created with that history, use the following reset only if you want to discard its data. Back up any data you need first.

```powershell
.\gradlew.bat clean bootJar
docker compose -p be -f compose.yaml -f compose.backend.yaml down --volumes
docker compose -p be -f compose.yaml -f compose.backend.yaml up -d --build --wait
```

`down --volumes` removes the PostgreSQL data volume attached to project `be`. Keep your `.env` so Compose can use the same settings when creating the new stack.
