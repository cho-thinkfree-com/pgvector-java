package com.pgvector

import groovy.sql.Sql
import com.pgvector.PGvector
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

public class GroovySqlTest {
    @Test
    void example() {
        def sql = Sql.newInstance("jdbc:postgresql://localhost:5432/pgvector_java_test")

        sql.execute "CREATE EXTENSION IF NOT EXISTS vector"
        sql.execute "DROP TABLE IF EXISTS groovy_sql_items"

        sql.execute "CREATE TABLE groovy_sql_items (id bigserial PRIMARY KEY, embedding vector(3))"

        def params = [
            new PGvector(new float[] {1, 1, 1}),
            new PGvector(new float[] {2, 2, 2}),
            new PGvector(new float[] {1, 1, 2}),
            null
        ]
        sql.executeInsert "INSERT INTO groovy_sql_items (embedding) VALUES (?), (?), (?), (?)", params

        def embedding = new PGvector(new float[] {1, 1, 1})
        def ids = new ArrayList<Long>()
        def embeddings = new ArrayList<PGvector>()
        sql.eachRow("SELECT * FROM groovy_sql_items ORDER BY embedding <-> ? LIMIT 5", [embedding]) { row ->
            ids.add(row.id)
            embeddings.add(row.embedding == null ? null : new PGvector(row.embedding.getValue()))
        }
        assertArrayEquals(new Long[] {1L, 3L, 2L, 4L}, ids.toArray())
        assertArrayEquals(new float[] {1, 1, 1}, embeddings.get(0).toArray())
        assertArrayEquals(new float[] {1, 1, 2}, embeddings.get(1).toArray())
        assertArrayEquals(new float[] {2, 2, 2}, embeddings.get(2).toArray())
        assertNull(embeddings.get(3))

        sql.executeInsert "CREATE INDEX ON groovy_sql_items USING ivfflat (embedding vector_l2_ops) WITH (lists = 100)"

        sql.close()
    }
}