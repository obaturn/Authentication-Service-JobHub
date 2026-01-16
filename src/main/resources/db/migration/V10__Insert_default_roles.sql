-- Insert default roles
INSERT INTO roles (id, name, description, created_at) VALUES
(gen_random_uuid(), 'job_seeker', 'Default role for job seekers', CURRENT_TIMESTAMP),
(gen_random_uuid(), 'employer', 'Role for employers posting jobs', CURRENT_TIMESTAMP),
(gen_random_uuid(), 'admin', 'Administrator role with full access', CURRENT_TIMESTAMP);