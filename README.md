# Heuristik ‑ AI‑driven Sales Insights

> **SpringBoot 3 + pgvector + ollama + Spring AI + GPT‑4o**

Heuristik convierte resúmenes diarios de ventas en **informes ejecutivos** mensuales alimentados por IA.
Embeddings almacenados en **pgvector** permiten búsquedas semánticas, mientras que GPT‑4o genera análisis de rendimiento, tendencias y sugerencias de negocio en un solo endpoint.

---
## Características principales

| Función                             | Descripción                                                                                             |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------- |
| **Generación de embeddings**        | Extrae metadatos de cada día de ventas y los vectoriza para búsquedas semánticas.                       |
| **Análisis mensual por IA**         | GPT‑4o resume cifras, detecta problemas y propone acciones de marketing y administración.               |
| **Separación de responsabilidades** | Los totales numéricos se calculan en Java → la IA se centra en la interpretación, no en las sumas.      |
| **JSON estricto**                   | El endpoint de análisis devuelve únicamente JSON validado contra un esquema, listo para front‑end o BI. |

---
## Stack tecnológico

| Capa / Herramienta                        | Versión / Imagen                                        | Rol |
|-------------------------------------------|---------------------------------------------------------|-----|
| **Lenguaje & Build**                      | **Java 17**, **Gradle 8.13**                            | Código fuente y build |
| **Framework**                             | **Spring Boot 3.4.5**                                   | Backend REST y configuración |
| **Vectorización**                         | **Ollama** – modelo `paraphrase-multilingual`           | Genera embeddings localmente |
| **Base de datos vectorial**               | **PostgreSQL 15 + pgvector** (`pgvector/pgvector:pg15`) | Almacena y consulta vectores |
| **Spring AI**                             | `spring-ai-openai`, `spring-ai-pgvector-store`          | Abstracción LLM + vector store |
| **LLM**                                   | **OpenAI GPT-4o**                                       | Redacta el informe mensual |
| **Contenedores**                          | **Docker**, **Docker Compose**                          | Orquestación local |

---
## Estructura clave del proyecto

```
heuristik/
 ├─ docker-compose.yml         # Postgres + pgvector
 ├─ src/main/java/com/kalapa/heuristik
 │   ├─ application/service
 │   │   └─ SalesSummaryAiService.java   # Lógica AI principal
 │   ├─ domain/entities
 │   │   └─ DailySalesSummary.java
 │   ├─ domain/repository
 │   │   └─ DailySalesSummaryRepository.java
 |   ├─ infrastructure/configs
 |   |   └─ PgVectorStoreConfiguration.java # Configuracion para VetorStore con ollama
 │   └─ interfaces/controller
 │       └─ EmbeddingTestController.java  # /api/ai/sales/* endpoints
 ├─ database
 |   ├─ product_generator.py    # Script python para generar productos
 |   ├─ sale_generator.py       # Script python para generar ventas y detalle de ventas
 |   └─ Dockerfile              # Generacion de imagen de DB con data integrada
 └─ README.md
```

---
## Inicio rápido

### 1. Prerrequisitos

| Requisito          | Versión mínima | Detalle |
|--------------------|----------------|---------|
| **Docker**         | 20.10+         | Para ejecutar `docker compose up -d` |
| **Java**           | 17             | Solo necesario si quieres correr la app fuera de Docker |
| **Gradle**         | 8.13           | Solo necesario si quieres correr la app fuera de Docker |
| **Cuenta OpenAI**  | n/a            | Con acceso al modelo **GPT-4o** |
| **OPENAI_API_KEY** | —              | Debe estar configurada en `.env` |

> ⚠️ **Nota sobre costos de OpenAI**  
> El uso de `OPENAI_API_KEY` genera cargos según la tarifa vigente de OpenAI para GPT-4o (u o3).  
> Asegúrate de contar con saldo suficiente antes de ejecutar los endpoints de análisis, pues cada invocación al modelo será facturada.  
> Si solo deseas probar la creación de embeddings sin coste, comenta o deshabilita temporalmente las llamadas a `SalesSummaryAiService.generateAISummary()`.


### 2. Clonar y configurar

```bash
git clone https://github.com/jl24pereira/heuristik.git
cd heuristik
cp .env_template .env        # edita tus credenciales aquí
```

> **Variables clave** (puedes sobreescribirlas en `application.yml` o `.env`):
>
> * `OPENAI_KEY` – token GPT‑4o
> * `OLLAMA_BASE_URL` – URL para Ollama
> * `POSTGRES_USER`, `POSTGRES_PASSWORD` – credenciales por default BD
> * `POSTGRES_DATASOURCE_URL` – `jdbc:postgresql://postgres:5432/heuristik_db`

### 3. Levantar la base + pgvector + microservicio

```bash
docker compose up --build -d
```

### 4. Agregar model a ollama

```bash
docker exec -it ollama ollama pull paraphrase-multilingual
```

> La aplicación escucha en `http://localhost:8080`.
---
## 🚀 Flujo de uso

1. **Generar embeddings del mes (una sola vez)**

   ```bash
   curl -X POST "http://localhost:8080/api/ai/sales/embeddings?month=01-2024"
   # → "Vectores almacenados exitosamente"
   ```

2. **Obtener análisis ejecutivo**

   ```bash
   curl -X GET "http://localhost:8080/api/ai/sales/analysis?month=01-2024" \
        -H "Accept: application/json"
   {
     "generalResume": "Resumen de enero…",
     "performance": ["…"],
     "tendencies":  ["…"],
     "problems":    ["…"],
     "recommendations": ["…"],
     "conclusions": "…"
   }
   ```
---
## Detalles de implementación

### SalesSummaryAiService

* **Generación de Embeddings**

    * Convierte cada `DailySalesSummary` a un `Document` con texto narrativo + metadata.
    * Usa `VectorStore.add()` para almacenar los vectores.
* **Análisis mensual**

    * Recupera los documentos del mes vía `filterExpression` en la metadata (`month = "01"`).
    * Calcula totales y distribuciones **en Java** a partir de `document.getMetadata()`.
    * Construye un prompt con roles `<system>/<user>`, inyecta los datos numéricos garantizados y los resúmenes diarios.
    * GPT‑4o responde **solo JSON** siguiendo un esquema estricto.
