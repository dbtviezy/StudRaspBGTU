-- The API also runs these statements lazily once per cold start.

CREATE TABLE IF NOT EXISTS teacher_reviews (
  id          bigserial PRIMARY KEY,
  teacher     text        NOT NULL,
  device_hash text        NOT NULL,
  overall     smallint    NOT NULL CHECK (overall BETWEEN 1 AND 5),
  delivery    smallint    NOT NULL CHECK (delivery BETWEEN 1 AND 5),
  strictness  smallint    NOT NULL CHECK (strictness BETWEEN 1 AND 5),
  loyalty     smallint    NOT NULL CHECK (loyalty BETWEEN 1 AND 5),
  text        varchar(300) NOT NULL DEFAULT '',
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  UNIQUE (teacher, device_hash)
);

CREATE INDEX IF NOT EXISTS teacher_reviews_teacher_idx ON teacher_reviews (teacher);

CREATE TABLE IF NOT EXISTS rate_events (
  id          bigserial PRIMARY KEY,
  device_hash text        NOT NULL,
  created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS rate_events_device_idx ON rate_events (device_hash, created_at);
