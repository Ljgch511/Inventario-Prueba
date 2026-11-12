# Sistema de Inventario GlobalR — Prototipo Funcional

Prototipo funcional basado en la **Arquitectura Híbrida (Monolito Satelital)** y la **Minuta de Levantamiento de Requerimientos (5 de noviembre de 2026)**.

## Arquitectura de Servicios (Docker Compose)

| Contenedor | Tecnología | Puerto | Rol |
| :--- | :--- | :---: | :--- |
| `globalr-proxy` | Nginx (TLS 1.3 + HTTP) | `80`, `443` | Proxy inverso unificado para SPA, API y APK |
| `globalr-web` | React 18 + TypeScript + Vite | interno `80` | Interfaz ITSM inspirada en Jira / Remedy |
| `globalr-core-api` | Spring Boot 3.3 + Java 21 | `8080` | Núcleo transaccional ACID, Clean Architecture, `@Filter` país, `@SQLRestriction` soft delete, auditoría |
| `globalr-sat-crypto` | Quarkus + BouncyCastle + ZXing | `8081` | Firma RSA + cifrado AES-256-GCM para QR (`< 1s`) y Bóveda de Claves |
| `globalr-sat-pdf` | Quarkus + Qute + OpenHTMLToPDF | `8082` | Renderizado de las 5 actas oficiales PDF (`Asignación`, `Salida`, `Entrada`, `Reparación`, `Transporte`) |
| `globalr-sat-media` | Quarkus + Thumbnailator | `8083` | Compresión automática de evidencias fotográficas hacia MinIO |
| `globalr-sat-notify` | Quarkus Reactive Mailer + Mutiny | `8084` | Envío asíncrono de notificaciones por correo ante eventos de tickets |
| `globalr-db` | PostgreSQL 16 + JSONB | `5432` | Persistencia relacional, extensiones JSONB y trigger inmutable en `audit_logs` |
| `globalr-minio` | MinIO S3 | `9000`, `9001` | Almacenamiento de objetos para fotos comprimidas y actas PDF |
| `globalr-mailpit` | Mailpit SMTP | `1025`, `8025` | Buzón de inspección de correos en desarrollo (`/mailpit/`) |

## Arranque Rápido

```bash
docker compose up -d --build
```

- **Frontend Web (Proxy)**: `http://localhost` (o `https://localhost`)
- **Bandeja de Correos (Mailpit)**: `http://localhost/mailpit/` (o `http://localhost:8025/mailpit/`)
- **Consola MinIO**: `http://localhost:9001` (`globalr_minio` / `globalr_minio_pass_2026`)

## Credenciales de Demostración (Clave común: `GlobalR2026!`)

- `admin.ve` — Administrador Venezuela (con acceso multi-país `VE` / `CO`)
- `supervisor.ve` — Supervisor Venezuela (Fernando Díaz: aprueba tickets y accede a Bóveda)
- `usuario.ve` — Usuario Común Venezuela (Rosa Vargas: vista simplificada + creación de tickets)
- `mantenimiento.ve` — Mantenimiento Venezuela (Luis Hernández)
- `contabilidad.ve` — Contabilidad Venezuela
- `admin.co` — Administrador Colombia (aislado estrictamente a `CO`)

## Compilación Nativa GraalVM (`globalr-sat-crypto`)

```bash
docker build -f satellites/sat-crypto/Dockerfile.native -t globalr-sat-crypto:native ./satellites/sat-crypto
```

## Compilación de la APK Android (`mobile-apk/`)

```bash
docker build -f mobile-apk/Dockerfile.apk -t globalr-apk-builder ./mobile-apk
docker create --name apk-extract globalr-apk-builder
docker cp apk-extract:/project/app/build/outputs/apk/debug/app-debug.apk ./globalr-field-mobile.apk
docker rm apk-extract
```

## Verificación Automatizada End-to-End

```bash
node scripts/verify_prototype.mjs
```
