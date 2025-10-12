This project is a Java Spring Boot application that demonstrates Retrieval-Augmented Generation (RAG) using PostgreSQL (with pgvector extension) and AI model integration via Ollama.

## Functionality Overview

- **Spring Boot Backend**: Provides a RESTful API for interacting with the application.
- **PostgreSQL with pgvector**: Stores and retrieves vector embeddings for efficient similarity search, enabling RAG workflows.
- **AI Model Integration (Ollama)**: Connects to a local Ollama instance to perform text generation and embedding using models like `tinyllama` and `nomic-embed-text`.
- **Automatic Schema Initialization**: The application can automatically initialize the pgvector schema in the database.
- **Configurable via Properties**: All major settings (database, AI, server port) are managed in `application.properties`.

## How It Works

1. **Data Ingestion**: Text data is embedded using the configured AI model and stored in PostgreSQL with vector columns (pgvector).
2. **Similarity Search**: When a query is received, its embedding is computed and compared against stored vectors to find relevant context.
3. **RAG Pipeline**: The retrieved context is combined with the query and sent to the AI model for generation, improving answer relevance.
4. **REST API**: Exposes endpoints (see `RagController.java`) for submitting queries and retrieving results.

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL with pgvector extension enabled
- Ollama running locally with required models pulled

## Configuration

Edit `src/main/resources/application.properties` to set:
- Database connection (URL, username, password)
- Ollama base URL and model names
- Server port

## Running the Application

1. Ensure PostgreSQL and Ollama are running.
2. Build and start the app:
   ```
   mvn clean spring-boot:run
   ```
3. Access the API at `http://localhost:8086` (or your configured port).

## Main Files

- `src/main/java/com/ai/rag_pg/RagPgApplication.java`: Main Spring Boot entry point.
- `src/main/java/com/ai/rag_pg/RagController.java`: REST controller for RAG endpoints.
- `src/main/resources/application.properties`: Configuration file.

## Example Endpoints

- `POST /your-endpoint` — (Describe your main API endpoint here)

## Notes

- Ensure the AI models specified in `application.properties` are available in your Ollama instance.
- The application will initialize the pgvector schema if not present.
- Extend the controller and service logic as needed for your use case.

---

Feel free to update this README as you add more features or endpoints!
