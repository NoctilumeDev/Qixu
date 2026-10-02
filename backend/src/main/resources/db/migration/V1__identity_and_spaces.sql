CREATE TABLE identity_user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 display_name VARCHAR(80) NOT NULL,
 role VARCHAR(16) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 student_verified BOOLEAN NOT NULL DEFAULT FALSE,
 auth_version BIGINT NOT NULL DEFAULT 1,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK (role IN ('STUDENT','TEACHER','ADMIN'))
) ENGINE=InnoDB;
CREATE TABLE external_identity (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 provider VARCHAR(32) NOT NULL,
 subject VARCHAR(128) NOT NULL,
 user_id BIGINT NOT NULL,
 UNIQUE KEY uq_external_subject(provider,subject),
 FOREIGN KEY(user_id) REFERENCES identity_user(id)
) ENGINE=InnoDB;
CREATE TABLE auth_session (
 token_hash CHAR(64) PRIMARY KEY,
 user_id BIGINT NOT NULL,
 auth_version BIGINT NOT NULL,
 csrf_token CHAR(64) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 INDEX ix_session_user(user_id),
 FOREIGN KEY(user_id) REFERENCES identity_user(id)
) ENGINE=InnoDB;
CREATE TABLE login_attempt (
 attempt_key CHAR(64) PRIMARY KEY,
 failures INT NOT NULL DEFAULT 0,
 window_end DATETIME(6) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE floor (
 id BIGINT PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 building VARCHAR(80) NOT NULL,
 level_number INT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1,
 source_kind VARCHAR(16) NOT NULL DEFAULT 'DEMO'
) ENGINE=InnoDB;
CREATE TABLE space (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 floor_id BIGINT NOT NULL,
 parent_id BIGINT NULL,
 code VARCHAR(32) NOT NULL,
 name VARCHAR(100) NOT NULL,
 kind VARCHAR(16) NOT NULL,
 use_mode VARCHAR(16) NOT NULL,
 capacity INT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 1,
 map_x INT NOT NULL,
 map_y INT NOT NULL,
 map_w INT NOT NULL,
 map_h INT NOT NULL,
 image_key VARCHAR(80) NULL,
 profile_json JSON NOT NULL,
 UNIQUE KEY uq_space_code(floor_id,code),
 UNIQUE KEY uq_space_floor(id,floor_id),
 FOREIGN KEY(floor_id) REFERENCES floor(id),
 FOREIGN KEY(parent_id,floor_id) REFERENCES space(id,floor_id),
 INDEX ix_space_floor_kind(floor_id,kind,active),
 CHECK (kind IN ('AREA','SEAT','ROOM','HALL')),
 CHECK (use_mode IN ('WALK_IN','BOOKABLE','PREPARATION','VENUE')),
 CHECK (capacity > 0),
 CHECK (map_x >= 0 AND map_y >= 0 AND map_w > 0 AND map_h > 0 AND map_x+map_w <= 1000 AND map_y+map_h <= 1000)
) ENGINE=InnoDB;
CREATE TABLE admin_scope (
 user_id BIGINT NOT NULL,
 floor_id BIGINT NOT NULL,
 PRIMARY KEY(user_id,floor_id),
 FOREIGN KEY(user_id) REFERENCES identity_user(id),
 FOREIGN KEY(floor_id) REFERENCES floor(id)
) ENGINE=InnoDB;
CREATE TABLE audit_entry (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 actor_id BIGINT NULL,
 action VARCHAR(64) NOT NULL,
 entity_type VARCHAR(32) NOT NULL,
 entity_id VARCHAR(64) NOT NULL,
 request_id VARCHAR(40) NOT NULL,
 detail_json JSON NOT NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX ix_audit_actor(actor_id,created_at),
 FOREIGN KEY(actor_id) REFERENCES identity_user(id)
) ENGINE=InnoDB;
