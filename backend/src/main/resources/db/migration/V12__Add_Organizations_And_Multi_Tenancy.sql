-- V12__Add_Organizations_And_Multi_Tenancy.sql

-- Organizations table (master DB only)
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    db_url TEXT NOT NULL,
    admin_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROVISIONING',
    plan_tier VARCHAR(20) DEFAULT 'STANDARD',
    provisioning_log TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add org_id to users table (nullable — NULL means Super Admin / master context)
ALTER TABLE users ADD COLUMN org_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_org_id
    FOREIGN KEY (org_id) REFERENCES organizations(id);

-- Update existing admin user to SUPER_ADMIN role
UPDATE users SET role = 'SUPER_ADMIN' WHERE username = 'admin';

-- Create default organization pointing to current master DB connection
INSERT INTO organizations (name, slug, db_url, admin_email, status, created_at, updated_at)
VALUES (
    'Default Organization',
    'default',
    '${master_db_url}',
    'admin@travelbillpro.com',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Link existing non-super-admin users to default org
UPDATE users SET org_id = (SELECT id FROM organizations WHERE slug = 'default')
WHERE role != 'SUPER_ADMIN' AND org_id IS NULL;

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_users_org_id ON users(org_id);
