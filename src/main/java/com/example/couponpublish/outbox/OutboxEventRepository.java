package com.example.couponpublish.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public OutboxEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initializeSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS outbox_events (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                aggregate_type VARCHAR(100) NOT NULL,
                aggregate_id VARCHAR(100) NOT NULL,
                event_type VARCHAR(100) NOT NULL,
                topic VARCHAR(200) NOT NULL,
                event_key VARCHAR(200) NOT NULL,
                payload TEXT NOT NULL,
                status VARCHAR(30) NOT NULL,
                attempts INT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                published_at TIMESTAMP NULL,
                last_error TEXT NULL
            )
            """);
    }

    public void save(
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String eventKey,
        String payload
    ) {
        jdbcTemplate.update("""
                INSERT INTO outbox_events (
                    aggregate_type, aggregate_id, event_type, topic, event_key,
                    payload, status, attempts, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0, ?)
                """,
            aggregateType,
            aggregateId,
            eventType,
            topic,
            eventKey,
            payload,
            Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public List<OutboxEvent> findPublishable(int limit, int maxAttempts) {
        return jdbcTemplate.query("""
                SELECT *
                FROM outbox_events
                WHERE status IN ('PENDING', 'FAILED')
                  AND attempts < ?
                ORDER BY id
                LIMIT ?
                """,
            this::map,
            maxAttempts,
            limit
        );
    }

    public void markPublished(Long id) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'PUBLISHED',
                    published_at = ?,
                    last_error = NULL
                WHERE id = ?
                """,
            Timestamp.valueOf(LocalDateTime.now()),
            id
        );
    }

    public void markFailed(Long id, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET status = 'FAILED',
                    attempts = attempts + 1,
                    last_error = ?
                WHERE id = ?
                """,
            truncate(errorMessage),
            id
        );
    }

    private OutboxEvent map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OutboxEvent(
            resultSet.getLong("id"),
            resultSet.getString("aggregate_type"),
            resultSet.getString("aggregate_id"),
            resultSet.getString("event_type"),
            resultSet.getString("topic"),
            resultSet.getString("event_key"),
            resultSet.getString("payload"),
            resultSet.getString("status"),
            resultSet.getInt("attempts"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            nullableDateTime(resultSet.getTimestamp("published_at")),
            resultSet.getString("last_error")
        );
    }

    private static LocalDateTime nullableDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1_000) {
            return value;
        }
        return value.substring(0, 1_000);
    }
}
