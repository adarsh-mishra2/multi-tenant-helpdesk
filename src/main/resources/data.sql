
INSERT INTO organization (id, name, slug, created_at, updated_at) VALUES
    (1, 'Acme Corp', 'acme', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
    (2, 'Globex Inc', 'globex', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days')
ON CONFLICT DO NOTHING;

INSERT INTO app_user (id, organization_id, email, password_hash, full_name, role, active, created_at, updated_at) VALUES
    (1, 1, 'admin@acme.com',      '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Asha Mehta',    'ORG_ADMIN', TRUE, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
    (2, 1, 'agent1@acme.com',     '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Rahul Verma',   'AGENT',     TRUE, NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days'),
    (3, 1, 'agent2@acme.com',     '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Neha Kapoor',   'AGENT',     TRUE, NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days'),
    (4, 1, 'customer1@acme.com',  '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Carlos Diaz',   'CUSTOMER',  TRUE, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
    (5, 1, 'customer2@acme.com',  '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Priya Nair',    'CUSTOMER',  TRUE, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
    (6, 2, 'admin@globex.com',    '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Hank Scorpio',  'ORG_ADMIN', TRUE, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
    (7, 2, 'agent@globex.com',    '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Mindy Simmons', 'AGENT',     TRUE, NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),
    (8, 2, 'customer@globex.com', '$2a$10$ugL63pUJlyki.2ai2Hz1rebkitHby10ACPvl.Pd5v3A6F6s534R4W', 'Homer Jay',     'CUSTOMER',  TRUE, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days')
ON CONFLICT DO NOTHING;

INSERT INTO ticket (id, organization_id, subject, description, priority, status, requester_id, assignee_id, closed_at, version, created_at, updated_at) VALUES
    (1, 1, 'Cannot log in to the billing portal',
        'Since yesterday the billing portal says my password is wrong, even after a reset.',
        'HIGH', 'IN_PROGRESS', 4, 2, NULL, 0, NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days'),
    (2, 1, 'Invoice INV-2041 shows the wrong GST number',
        'The GST number printed on invoice INV-2041 belongs to our old office. Please correct and reissue.',
        'MEDIUM', 'OPEN', 4, NULL, NULL, 0, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (3, 1, 'Dashboard export to CSV times out',
        'Exporting more than a month of data from the dashboard spins for a minute and then fails.',
        'URGENT', 'OPEN', 5, 3, NULL, 0, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (4, 1, 'Request to add two new team members',
        'Please add two seats to our plan for new joiners starting next Monday.',
        'LOW', 'RESOLVED', 5, 2, NULL, 0, NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days'),
    (5, 1, 'Mobile app crashes on Android 14',
        'The app closes immediately after the splash screen on a Pixel 7 running Android 14.',
        'HIGH', 'CLOSED', 4, 3, NOW() - INTERVAL '12 days', 0, NOW() - INTERVAL '15 days', NOW() - INTERVAL '12 days'),
    (6, 1, 'Email notifications arrive twice',
        'Every ticket update email reaches us two times within a few seconds.',
        'MEDIUM', 'OPEN', 5, NULL, NULL, 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (7, 1, 'Single sign-on with Azure AD',
        'We would like to enable SSO for our staff using Azure AD. What are the steps?',
        'MEDIUM', 'IN_PROGRESS', 4, 3, NULL, 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
    (8, 1, 'Typo on the pricing page',
        'The Business plan says "unlimted" instead of "unlimited".',
        'LOW', 'OPEN', 5, NULL, NULL, 0, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
    (9, 2, 'Reactor monitoring report missing',
        'The weekly monitoring report was not generated this Monday.',
        'HIGH', 'OPEN', 8, 7, NULL, 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (10, 2, 'Change company address on account',
        'We have moved to Cypress Creek. Please update the address on our account.',
        'LOW', 'OPEN', 8, NULL, NULL, 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (11, 2, 'API rate limit too low for nightly sync',
        'Our nightly sync hits the rate limit around 2 AM. Can the limit be raised?',
        'MEDIUM', 'IN_PROGRESS', 8, 7, NULL, 0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days')
ON CONFLICT DO NOTHING;

INSERT INTO ticket_comment (id, ticket_id, author_id, body, internal, created_at, updated_at) VALUES
    (1, 1, 2, 'Thanks Carlos, I am looking into this. Could you tell me which browser you are using?', FALSE, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (2, 1, 4, 'Chrome on Windows 11.', FALSE, NOW() - INTERVAL '5 days' + INTERVAL '1 hour', NOW() - INTERVAL '5 days' + INTERVAL '1 hour'),
    (3, 1, 2, 'Account is locked after five failed attempts. Unlocking needs approval from the billing team.', TRUE, NOW() - INTERVAL '5 days' + INTERVAL '2 hours', NOW() - INTERVAL '5 days' + INTERVAL '2 hours'),
    (4, 3, 3, 'Reproduced with a 45 day range. Query plan shows a sequential scan, raising with engineering.', TRUE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (5, 4, 2, 'Both seats have been added. You should see them under Settings, Team.', FALSE, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
    (6, 9, 7, 'Report job failed on a disk quota error. Cleaning up and re-running.', TRUE, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO ticket_history (id, ticket_id, action, from_value, to_value, changed_by_id, created_at, updated_at) VALUES
    (1, 1, 'CREATED', NULL, 'OPEN', 4, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    (2, 1, 'ASSIGNEE_CHANGED', NULL, 'Rahul Verma', 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (3, 1, 'STATUS_CHANGED', 'OPEN', 'IN_PROGRESS', 2, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (4, 2, 'CREATED', NULL, 'OPEN', 4, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (5, 3, 'CREATED', NULL, 'OPEN', 5, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (6, 3, 'ASSIGNEE_CHANGED', NULL, 'Neha Kapoor', 1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    (7, 4, 'CREATED', NULL, 'OPEN', 5, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (8, 4, 'ASSIGNEE_CHANGED', NULL, 'Rahul Verma', 2, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
    (9, 4, 'STATUS_CHANGED', 'OPEN', 'RESOLVED', 2, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
    (10, 5, 'CREATED', NULL, 'OPEN', 4, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
    (11, 5, 'ASSIGNEE_CHANGED', NULL, 'Neha Kapoor', 1, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
    (12, 5, 'STATUS_CHANGED', 'OPEN', 'CLOSED', 3, NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
    (13, 6, 'CREATED', NULL, 'OPEN', 5, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (14, 7, 'CREATED', NULL, 'OPEN', 4, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (15, 7, 'ASSIGNEE_CHANGED', NULL, 'Neha Kapoor', 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (16, 7, 'STATUS_CHANGED', 'OPEN', 'IN_PROGRESS', 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (17, 8, 'CREATED', NULL, 'OPEN', 5, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
    (18, 9, 'CREATED', NULL, 'OPEN', 8, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (19, 9, 'ASSIGNEE_CHANGED', NULL, 'Mindy Simmons', 6, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (20, 10, 'CREATED', NULL, 'OPEN', 8, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    (21, 11, 'CREATED', NULL, 'OPEN', 8, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (22, 11, 'STATUS_CHANGED', 'OPEN', 'IN_PROGRESS', 7, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('organization', 'id'), (SELECT MAX(id) FROM organization));
SELECT setval(pg_get_serial_sequence('app_user', 'id'), (SELECT MAX(id) FROM app_user));
SELECT setval(pg_get_serial_sequence('ticket', 'id'), (SELECT MAX(id) FROM ticket));
SELECT setval(pg_get_serial_sequence('ticket_comment', 'id'), (SELECT MAX(id) FROM ticket_comment));
SELECT setval(pg_get_serial_sequence('ticket_history', 'id'), (SELECT MAX(id) FROM ticket_history));
