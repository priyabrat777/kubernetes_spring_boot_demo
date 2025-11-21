-- PostgreSQL Initialization Script
-- This script runs when the database is first created

-- Create the database (if not exists)
-- Note: The database is already created by POSTGRES_DB env var

-- Create the data_items table
CREATE TABLE IF NOT EXISTS data_items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    timestamp BIGINT NOT NULL
);

-- Create index on timestamp for better query performance
CREATE INDEX IF NOT EXISTS idx_data_items_timestamp ON data_items(timestamp);

-- Insert initial sample data
INSERT INTO data_items (id, name, description, timestamp) 
VALUES 
    ('1', 'Sample Item 1', 'This is a sample item stored in PostgreSQL', EXTRACT(EPOCH FROM NOW()) * 1000),
    ('2', 'Sample Item 2', 'Another sample item from database', EXTRACT(EPOCH FROM NOW()) * 1000),
    ('3', 'Kubernetes Demo', 'Demo item for Kubernetes deployment', EXTRACT(EPOCH FROM NOW()) * 1000)
ON CONFLICT (id) DO NOTHING;

-- Grant permissions (optional, user already has permissions)
GRANT ALL PRIVILEGES ON TABLE data_items TO demouser;
