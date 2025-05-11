package com.kalapa.heuristik.infrastructure.configs;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorStoreConfiguration {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel ollamaEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, ollamaEmbeddingModel)
                .dimensions(768) // o el valor correcto según tu modelo
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true) // crea la tabla si no existe
                .schemaName("public")
                .vectorTableName("vector_store")
                .build();
    }
}
