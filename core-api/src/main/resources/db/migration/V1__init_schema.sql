-- GlobalR Asset Inventory System - Initial Schema (ISO 27001 / LOPCYMAT / Multitenancy)

CREATE TABLE countries (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    national_id VARCHAR(40),
    role VARCHAR(30) NOT NULL,
    multi_country_access BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE personnel (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    target_type VARCHAR(20) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    national_id VARCHAR(40),
    group_or_office_name VARCHAR(150),
    email VARCHAR(150),
    department VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    version BIGINT NOT NULL DEFAULT 0,
    asset_number VARCHAR(60) NOT NULL,
    equipment_number VARCHAR(60),
    category VARCHAR(30) NOT NULL,
    subcategory VARCHAR(60) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(120) NOT NULL,
    serial_number VARCHAR(120) NOT NULL,
    condition_status VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processor VARCHAR(120),
    operating_system VARCHAR(120),
    ram_spec VARCHAR(80),
    storage_rom_spec VARCHAR(80),
    has_ethernet_port BOOLEAN,
    physical_location VARCHAR(200) NOT NULL,
    bitlocker_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    primary_photo_key VARCHAR(300),
    observations TEXT,
    specs_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_assignee_id UUID REFERENCES personnel(id),
    qr_token_id VARCHAR(80),
    qr_payload_encrypted TEXT,
    quality_rating INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE UNIQUE INDEX uq_assets_country_number_active
    ON assets(country_id, asset_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_country_status ON assets(country_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_assets_specs_gin ON assets USING GIN (specs_json);

CREATE TABLE asset_vault (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL UNIQUE REFERENCES assets(id),
    bitlocker_recovery_key_enc TEXT,
    device_email_enc TEXT,
    device_password_enc TEXT,
    extra_notes_enc TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE asset_complements (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    parent_asset_id UUID NOT NULL REFERENCES assets(id),
    complement_asset_id UUID REFERENCES assets(id),
    complement_type VARCHAR(60) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(120) NOT NULL,
    serial_number VARCHAR(120) NOT NULL,
    notes TEXT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE stored_parts (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    part_type VARCHAR(40) NOT NULL,
    capacity VARCHAR(60) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(120),
    serial_number VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    installed_in_asset_id UUID REFERENCES assets(id),
    physical_location VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    version BIGINT NOT NULL DEFAULT 0,
    ticket_code VARCHAR(40) NOT NULL UNIQUE,
    ticket_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    reason TEXT NOT NULL,
    asset_id UUID NOT NULL REFERENCES assets(id),
    requester_user_id UUID NOT NULL REFERENCES users(id),
    target_personnel_id UUID REFERENCES personnel(id),
    assigned_maintenance_user_id UUID REFERENCES users(id),
    transporter_name VARCHAR(150),
    transporter_id_doc VARCHAR(50),
    origin_location VARCHAR(200),
    destination_location VARCHAR(200),
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    approved_by_user_id UUID REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE INDEX idx_tickets_country_status ON tickets(country_id, status) WHERE deleted_at IS NULL;

CREATE TABLE ticket_approvals (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(40) NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    personnel_id UUID NOT NULL REFERENCES personnel(id),
    ticket_id UUID REFERENCES tickets(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    returned_at TIMESTAMPTZ,
    reason TEXT NOT NULL,
    asset_status_at_assign VARCHAR(30) NOT NULL,
    asset_status_at_return VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE movements_transport (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    ticket_id UUID REFERENCES tickets(id),
    movement_type VARCHAR(30) NOT NULL,
    entry_source VARCHAR(40),
    origin_location VARCHAR(200),
    destination_location VARCHAR(200) NOT NULL,
    transporter_name VARCHAR(150),
    transporter_id_doc VARCHAR(50),
    executed_by_user_id UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    moved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE maintenances (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    ticket_id UUID REFERENCES tickets(id),
    maintenance_mode VARCHAR(20) NOT NULL,
    provider_name VARCHAR(150),
    technician_user_id UUID REFERENCES users(id),
    technician_name VARCHAR(150),
    failure_description TEXT NOT NULL,
    reason TEXT NOT NULL,
    work_performed TEXT,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE part_changes (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    maintenance_id UUID REFERENCES maintenances(id),
    ticket_id UUID REFERENCES tickets(id),
    stored_part_id UUID REFERENCES stored_parts(id),
    removed_part_desc VARCHAR(255),
    installed_part_desc VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    performed_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE decommissionings (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL UNIQUE REFERENCES assets(id),
    ticket_id UUID REFERENCES tickets(id),
    disposition VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    parts_usage_notes TEXT NOT NULL,
    recipient_info VARCHAR(200),
    decommissioned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE financial_records (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    acquisition_type VARCHAR(40) NOT NULL,
    local_cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    local_currency VARCHAR(10) NOT NULL,
    usd_reference_cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    depreciation_rate_annual NUMERIC(5, 2) DEFAULT 20.00,
    residual_value_usd NUMERIC(15, 2) DEFAULT 0,
    provider_name VARCHAR(150),
    invoice_number VARCHAR(100),
    warranty_months INTEGER,
    warranty_expires_at DATE,
    lease_counterparty VARCHAR(150),
    lease_start_date DATE,
    lease_end_date DATE,
    lease_monthly_price NUMERIC(15, 2),
    consignment_installments INTEGER,
    consignment_paid_installments INTEGER DEFAULT 0,
    consignment_term_months INTEGER,
    consignment_due_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE os_licenses (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    os_name VARCHAR(100) NOT NULL,
    edition VARCHAR(100) NOT NULL,
    license_type VARCHAR(30) NOT NULL,
    license_key_masked VARCHAR(100),
    total_seats INTEGER NOT NULL DEFAULT 1,
    assigned_asset_id UUID REFERENCES assets(id),
    provider_name VARCHAR(150),
    invoice_number VARCHAR(100),
    cost_usd NUMERIC(15, 2) NOT NULL DEFAULT 0,
    purchased_at DATE,
    expires_at DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE media_evidences (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    ticket_id UUID REFERENCES tickets(id),
    movement_id UUID REFERENCES movements_transport(id),
    stage VARCHAR(30) NOT NULL,
    minio_object_key VARCHAR(350) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    original_size_bytes BIGINT NOT NULL,
    compressed_size_bytes BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    captured_by_user_id UUID REFERENCES users(id),
    captured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE generated_documents (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    ticket_id UUID REFERENCES tickets(id),
    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(60) NOT NULL UNIQUE,
    reason TEXT NOT NULL,
    minio_object_key VARCHAR(350) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    generated_by_user_id UUID REFERENCES users(id),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(80),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(80),
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(80)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    country_id UUID,
    actor_user_id UUID,
    actor_username VARCHAR(80) NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(80),
    source_channel VARCHAR(20) NOT NULL DEFAULT 'WEB',
    client_ip VARCHAR(64),
    details_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_country_time ON audit_logs(country_id, created_at DESC);

-- ISO 27001 / LOPCYMAT Immutability Guarantee: prevent UPDATE or DELETE on audit_logs
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable (ISO 27001 / LOPCYMAT compliance): % is not permitted.', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
