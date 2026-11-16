package com.example.AI_Projekt.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

/** The result of one catalog sync run. */
@Entity
@Getter
@Setter
public class SyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Timestamp startedAt;
    private Timestamp finishedAt;
    // Stored as text. Without @JdbcTypeCode, Hibernate expects a native MySQL ENUM column
    // and schema validation fails against VARCHAR.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private SyncStatus status;
    private String countsJson;
    private String errorMessage;
}
