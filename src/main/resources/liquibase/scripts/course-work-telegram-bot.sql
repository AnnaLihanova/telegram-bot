-- liquibase formatted sql

-- changeset annaLihanova:1
CREATE TABLE IF NOT EXISTS notification_task
(
    id                serial    NOT NULL PRIMARY KEY,
    chat_id           bigint    NOT NULL,
    date_time timestamp NOT NULL,
    message           text   NOT NULL
);