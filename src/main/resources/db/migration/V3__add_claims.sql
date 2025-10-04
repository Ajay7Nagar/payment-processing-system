-- Claims catalog
CREATE TABLE claims (
    id UUID PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Role to claim association table
CREATE TABLE role_claims (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    claim_id UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, claim_id)
);

-- Seed claims for granular access control
INSERT INTO claims (id, code, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'PAYMENTS_PURCHASE_CREATE', 'Create purchase transactions'),
    ('00000000-0000-0000-0000-000000000002', 'PAYMENTS_PURCHASE_VIEW_ANY', 'View purchase orders for any merchant'),
    ('00000000-0000-0000-0000-000000000003', 'PAYMENTS_PURCHASE_VIEW_OWN', 'View purchase orders created by the same subject'),
    ('00000000-0000-0000-0000-000000000004', 'PAYMENTS_AUTHORIZE_CREATE', 'Create authorization transactions'),
    ('00000000-0000-0000-0000-000000000005', 'PAYMENTS_AUTHORIZE_VIEW_ANY', 'View authorization transactions for any merchant'),
    ('00000000-0000-0000-0000-000000000006', 'PAYMENTS_CAPTURE_EXECUTE', 'Capture authorized transactions'),
    ('00000000-0000-0000-0000-000000000007', 'PAYMENTS_CANCEL_EXECUTE', 'Cancel/void transactions'),
    ('00000000-0000-0000-0000-000000000008', 'PAYMENTS_REFUND_EXECUTE', 'Issue refunds'),
    ('00000000-0000-0000-0000-000000000009', 'PAYMENTS_REFUND_VIEW_ANY', 'View refunds for any merchant'),
    ('00000000-0000-0000-0000-00000000000A', 'PAYMENTS_REFUND_VIEW_OWN', 'View refunds initiated by the same subject'),
    ('00000000-0000-0000-0000-00000000000B', 'SUBSCRIPTIONS_CREATE', 'Create subscriptions'),
    ('00000000-0000-0000-0000-00000000000C', 'SUBSCRIPTIONS_UPDATE', 'Modify subscriptions'),
    ('00000000-0000-0000-0000-00000000000D', 'SUBSCRIPTIONS_VIEW_ANY', 'View subscriptions for any merchant'),
    ('00000000-0000-0000-0000-00000000000E', 'SUBSCRIPTIONS_VIEW_OWN', 'View subscriptions tied to the subject'),
    ('00000000-0000-0000-0000-00000000000F', 'SETTLEMENT_EXPORT_REQUEST', 'Request settlement exports'),
    ('00000000-0000-0000-0000-000000000010', 'SETTLEMENT_EXPORT_VIEW', 'View settlement exports'),
    ('00000000-0000-0000-0000-000000000011', 'COMPLIANCE_AUDIT_VIEW', 'Access compliance audit logs');

-- Map existing roles to claims
INSERT INTO role_claims (role_id, claim_id)
SELECT r.id, c.id
FROM roles r
JOIN claims c ON (
    (r.code = 'PAYMENTS_WRITE' AND c.code IN (
        'PAYMENTS_PURCHASE_CREATE',
        'PAYMENTS_AUTHORIZE_CREATE',
        'PAYMENTS_CAPTURE_EXECUTE',
        'PAYMENTS_CANCEL_EXECUTE',
        'PAYMENTS_REFUND_EXECUTE'))
    OR (r.code = 'PAYMENTS_READ' AND c.code IN (
        'PAYMENTS_PURCHASE_VIEW_ANY',
        'PAYMENTS_REFUND_VIEW_ANY'))
    OR (r.code = 'SUBSCRIPTIONS_WRITE' AND c.code IN (
        'SUBSCRIPTIONS_CREATE',
        'SUBSCRIPTIONS_UPDATE'))
    OR (r.code = 'SUBSCRIPTIONS_READ' AND c.code IN (
        'SUBSCRIPTIONS_VIEW_ANY'))
    OR (r.code = 'SETTLEMENT_EXPORT' AND c.code IN (
        'SETTLEMENT_EXPORT_REQUEST',
        'SETTLEMENT_EXPORT_VIEW'))
    OR (r.code = 'COMPLIANCE_OFFICER' AND c.code IN (
        'COMPLIANCE_AUDIT_VIEW'))
    OR (r.code = 'ADMIN')
);

-- Admin should have every claim
INSERT INTO role_claims (role_id, claim_id)
SELECT r.id, c.id
FROM roles r
CROSS JOIN claims c
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

