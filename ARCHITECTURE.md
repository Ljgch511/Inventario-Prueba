# Arquitectura del Sistema — Monolito Modular con Clean Architecture

## 1. Principio Fundamental: Monolito Modular
El sistema está construido como un **Monolito Modular**. Todo corre en una sola aplicación Spring Boot para mantener la simplicidad operativa (**KISS**), pero el código está organizado en **módulos de negocio completamente aislados** (`asset`, `auth`, `ticket`, `audit`).

### Regla de oro para futura migración a Microservicios:
1. **Comunicación exclusiva por interfaces (DIP - Dependency Inversion):** Un módulo **nunca** accede a clases internas ni repositorios de otro módulo.
2. **Propiedad de datos:** Cada módulo gestiona sus propias tablas. No existen `@ManyToOne` ni llaves foráneas JPA cruzadas entre módulos; solo se referencian por identificadores (`UUID`).
3. **Eventos de dominio:** Las acciones que involucran a otros módulos (ej. ticket aprobado -> enviar correo y auditar) se emiten como eventos desacoplados.

---

## 2. Las 3 Capas de Clean Architecture (por módulo)

Cada módulo (`modules/<nombre-modulo>/`) se divide estrictamente en 3 capas:

```
modules/asset/
├── domain/            <-- CAPA 1: Reglas de negocio puras (CERO Spring, CERO JPA)
│   ├── model/         <-- Entidades puras y Value Objects (inmutables, sin setters libres)
│   ├── event/         <-- Eventos de dominio generados por la entidad
│   └── exception/     <-- Excepciones de negocio (ej. AssetNotFoundException)
│
├── application/       <-- CAPA 2: Casos de uso y Puertos
│   ├── port/
│   │   ├── in/        <-- Interfaces de Entrada: Qué puede hacer el usuario (Use Cases)
│   │   └── out/       <-- Interfaces de Salida: Qué necesita el caso de uso (SPI: Repositorios, Servicios externos)
│   ├── usecase/       <-- Implementación de los casos de uso (Orquestación del negocio)
│   └── dto/           <-- Data Transfer Objects (Comandos y Queries sin lógica)
│
└── infrastructure/    <-- CAPA 3: Adaptadores y Tecnología externa
    ├── adapter/
    │   ├── in/web/    <-- Controladores REST (@RestController, @PostMapping)
    │   └── out/persistence/ <-- Repositorios Spring Data JPA, Entidades de base de datos (@Table)
    └── config/        <-- Beans de configuración de Spring para el módulo
```

### ¿Por qué esta separación? (SOLID)
* **Single Responsibility (SRP):** El modelo de dominio solo se encarga de sus reglas de validación. La persistencia solo guarda en SQL. El controlador solo atiende HTTP.
* **Dependency Inversion (DIP):** La capa `application` define la interfaz `AssetRepositoryPort`. La capa `infrastructure` implementa esa interfaz con Spring Data JPA. **El negocio no sabe qué base de datos existe**.
* **Open/Closed (OCP):** Si mañana cambiamos PostgreSQL por MongoDB o por una API REST remota (microservicio), el dominio y los casos de uso no cambian ni una sola línea.
