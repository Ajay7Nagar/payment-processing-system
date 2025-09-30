INSERT INTO roles (id, code, description)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN', 'System administrator'),
    ('22222222-2222-2222-2222-222222222222', 'SUBSCRIPTIONS_WRITE', 'Manage subscription lifecycle'),
    ('33333333-3333-3333-3333-333333333333', 'SUBSCRIPTIONS_READ', 'Read subscription data'),
    ('44444444-4444-4444-4444-444444444444', 'PAYMENTS_WRITE', 'Execute payment commands'),
    ('55555555-5555-5555-5555-555555555555', 'PAYMENTS_READ', 'View payments and refunds'),
    ('66666666-6666-6666-6666-666666666666', 'SETTLEMENT_EXPORT', 'Generate settlement exports'),
    ('77777777-7777-7777-7777-777777777777', 'COMPLIANCE_OFFICER', 'Access compliance data')
ON CONFLICT (code) DO NOTHING;
