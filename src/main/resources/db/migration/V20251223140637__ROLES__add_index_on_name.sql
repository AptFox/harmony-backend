/*
    TABLE: roles
    CHANGES: Adding index on name
 */

CREATE INDEX idx_roles_name ON roles(name);