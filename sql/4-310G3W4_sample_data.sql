-- IT Help Desk Ticketing System
-- Week 4: sample data so the report queries return results
-- Class 4-310 | Group 3
--
-- Run AFTER 4-310G3W2.sql (which recreates the database):
--   mysql -u root -p < sql/4-310G3W2.sql
--   mysql -u root -p < sql/4-310G3W4_sample_data.sql
--
-- Dates are relative to NOW() so "this month" and "days open" always make sense.
-- IDs below assume a fresh database from the Week 2 script:
--   users 1-3 = technicians (Jose, Miguel, Paolo)
--   status   1 Open, 2 Assigned, 3 In Progress, 4 Resolved, 5 Closed, 6 Reopened
--   priority 1 Critical (4h), 2 High (8h), 3 Medium (24h), 4 Low (72h)
--   category 1 Computer Hardware, 2 Printer Issues, 3 Network Connectivity, 4 Software Installation

USE it_helpdesk;

-- ---------------------------------------------------------------
-- USERS (ids 4-9)
-- ---------------------------------------------------------------
INSERT INTO user_account (first_name, last_name, email, contact_number, password_hash, role, department_id, team_id) VALUES
    ('Ana',   'Santos', 'ana@example.com',   '09171234501', 'hash5', 'Requester',     2, NULL),  -- 4
    ('Ben',   'Cruz',   'ben@example.com',   '09171234502', 'hash6', 'Requester',     3, NULL),  -- 5
    ('Carla', 'Reyes',  'carla@example.com', '09171234503', 'hash7', 'Requester',     2, NULL),  -- 6
    ('Dan',   'Lim',    'dan@example.com',   '09171234504', 'hash8', 'Requester',     3, NULL),  -- 7
    ('Ella',  'Garcia', 'ella@example.com',  '09171234505', 'hash9', 'Administrator', 1, NULL),  -- 8
    ('Felix', 'Uy',     'felix@example.com', '09171234506', 'hash10','Requester',     1, NULL);  -- 9

-- ---------------------------------------------------------------
-- ASSETS (ids 1-7). LT-003 and MN-001 never get a ticket (query 4).
-- ---------------------------------------------------------------
INSERT INTO asset (asset_tag, asset_type, brand, model, serial_number, location, asset_status) VALUES
    ('LT-001', 'Laptop',  'Dell',    'Latitude 5440',  'DL5440-001', 'HR Office',      'In Use'),        -- 1
    ('LT-002', 'Laptop',  'Lenovo',  'ThinkPad E14',   'LN-E14-002', 'Finance Office', 'In Use'),        -- 2
    ('PR-001', 'Printer', 'HP',      'LaserJet M404',  'HP-M404-001','2nd Floor',      'In Use'),        -- 3
    ('PC-001', 'Desktop', 'Acer',    'Veriton X',      'AC-VX-001',  'HR Office',      'Under Repair'),  -- 4
    ('RT-001', 'Router',  'Cisco',   'ISR 1100',       'CS-1100-001','Server Room',    'In Use'),        -- 5
    ('LT-003', 'Laptop',  'HP',      'ProBook 440',    'HP-PB-003',  'IT Storage',     'In Storage'),    -- 6
    ('MN-001', 'Monitor', 'Samsung', 'S24R350',        'SM-S24-001', 'IT Storage',     'In Storage');    -- 7

-- ---------------------------------------------------------------
-- TICKETS (ids 1-12)
-- ---------------------------------------------------------------
INSERT INTO ticket (subject, description, date_created, date_updated, date_resolved, date_closed,
                    requester_id, technician_id, category_id, priority_id, status_id, asset_id) VALUES
    -- 1: Closed, High, resolved in 6h (on time)
    ('Laptop will not turn on', 'Power light blinks but the screen stays black.',
     NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,
     NOW() - INTERVAL 40 DAY + INTERVAL 6 HOUR, NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,
     4, 1, 1, 2, 5, 1),
    -- 2: Closed, Medium, resolved in 30h (missed 24h target)
    ('Printer jams on every print', 'Paper jams in tray 2 every time.',
     NOW() - INTERVAL 35 DAY, NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,
     NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR, NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,
     5, 1, 2, 3, 5, 3),
    -- 3: Closed, Critical, resolved in 3h (on time)
    ('No internet on 2nd floor', 'Whole floor lost connection after a power outage.',
     NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,
     NOW() - INTERVAL 20 DAY + INTERVAL 3 HOUR, NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,
     6, 2, 3, 1, 5, 5),
    -- 4: Resolved, Low, resolved in 80h (missed 72h target)
    ('Install accounting software', 'Need the new accounting package on my laptop.',
     NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR,
     NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR, NULL,
     5, 3, 4, 4, 4, NULL),
    -- 5: In Progress, High (overdue)
    ('VPN keeps disconnecting', 'VPN drops every 10 minutes when working from home.',
     NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 8 DAY, NULL, NULL,
     7, 2, 3, 2, 3, 2),
    -- 6: Assigned, Medium (overdue)
    ('Desktop is very slow', 'Takes 10 minutes to start and programs freeze.',
     NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 5 DAY, NULL, NULL,
     4, 1, 1, 3, 2, 4),
    -- 7: Resolved, Low, resolved in 20h (on time)
    ('Need MS Office license', 'Office says my license has expired.',
     NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR,
     NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR, NULL,
     6, 3, 4, 4, 4, NULL),
    -- 8: Open, unassigned, Medium (overdue)
    ('Printer shows offline', 'The 2nd floor printer shows offline on all PCs.',
     NOW() - INTERVAL 3 DAY, NULL, NULL, NULL,
     4, NULL, 2, 3, 1, 3),
    -- 9: Open, unassigned, new
    ('Email not syncing on phone', 'Work email stopped syncing on my phone.',
     NOW() - INTERVAL 2 HOUR, NULL, NULL, NULL,
     7, NULL, 4, 3, 1, NULL),
    -- 10: Reopened, High (overdue)
    ('Wi-Fi drops in meeting room', 'Fixed last week but it is dropping again.',
     NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 2 DAY, NULL, NULL,
     5, 2, 3, 2, 6, NULL),
    -- 11: Open, unassigned, new
    ('Monitor flickering', 'Second monitor flickers when the laptop is docked.',
     NOW() - INTERVAL 1 HOUR, NULL, NULL, NULL,
     4, NULL, 1, 4, 1, NULL),
    -- 12: In Progress, Low
    ('Keyboard keys stuck', 'Some keys on my keyboard do not respond.',
     NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 3 HOUR, NULL, NULL,
     9, 1, 1, 4, 3, NULL);

-- ---------------------------------------------------------------
-- TICKET HISTORY (every status change is logged, BR11)
-- changed_by 8 = Ella Garcia (Administrator)
-- ---------------------------------------------------------------
INSERT INTO ticket_history (date_changed, remarks, ticket_id, status_id, changed_by) VALUES
    (NOW() - INTERVAL 40 DAY,                         'Ticket submitted',         1, 1, 4),
    (NOW() - INTERVAL 40 DAY + INTERVAL 1 HOUR,       'Assigned to technician',   1, 2, 8),
    (NOW() - INTERVAL 40 DAY + INTERVAL 6 HOUR,       'Replaced power adapter',   1, 4, 1),
    (NOW() - INTERVAL 40 DAY + INTERVAL 30 HOUR,      'Requester confirmed',      1, 5, 4),

    (NOW() - INTERVAL 35 DAY,                         'Ticket submitted',         2, 1, 5),
    (NOW() - INTERVAL 35 DAY + INTERVAL 2 HOUR,       'Assigned to technician',   2, 2, 8),
    (NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR,      'Replaced pickup roller',   2, 4, 1),
    (NOW() - INTERVAL 35 DAY + INTERVAL 50 HOUR,      'Requester confirmed',      2, 5, 5),

    (NOW() - INTERVAL 20 DAY,                         'Ticket submitted',         3, 1, 6),
    (NOW() - INTERVAL 20 DAY + INTERVAL 15 MINUTE,    'Assigned to technician',   3, 2, 8),
    (NOW() - INTERVAL 20 DAY + INTERVAL 3 HOUR,       'Router restarted and reconfigured', 3, 4, 2),
    (NOW() - INTERVAL 20 DAY + INTERVAL 24 HOUR,      'Requester confirmed',      3, 5, 6),

    (NOW() - INTERVAL 12 DAY,                         'Ticket submitted',         4, 1, 5),
    (NOW() - INTERVAL 12 DAY + INTERVAL 5 HOUR,       'Assigned to technician',   4, 2, 8),
    (NOW() - INTERVAL 12 DAY + INTERVAL 80 HOUR,      'Software installed',       4, 4, 3),

    (NOW() - INTERVAL 9 DAY,                          'Ticket submitted',         5, 1, 7),
    (NOW() - INTERVAL 9 DAY + INTERVAL 1 HOUR,        'Assigned to technician',   5, 2, 8),
    (NOW() - INTERVAL 8 DAY,                          'Checking VPN logs',        5, 3, 2),

    (NOW() - INTERVAL 6 DAY,                          'Ticket submitted',         6, 1, 4),
    (NOW() - INTERVAL 5 DAY,                          'Assigned to technician',   6, 2, 8),

    (NOW() - INTERVAL 5 DAY,                          'Ticket submitted',         7, 1, 6),
    (NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR,        'Assigned to technician',   7, 2, 8),
    (NOW() - INTERVAL 5 DAY + INTERVAL 20 HOUR,       'License renewed',          7, 4, 3),

    (NOW() - INTERVAL 3 DAY,                          'Ticket submitted',         8, 1, 4),
    (NOW() - INTERVAL 2 HOUR,                         'Ticket submitted',         9, 1, 7),

    (NOW() - INTERVAL 15 DAY,                         'Ticket submitted',        10, 1, 5),
    (NOW() - INTERVAL 15 DAY + INTERVAL 1 HOUR,       'Assigned to technician',  10, 2, 8),
    (NOW() - INTERVAL 14 DAY,                         'Access point restarted',  10, 4, 2),
    (NOW() - INTERVAL 2 DAY,                          'Problem came back',       10, 6, 5),

    (NOW() - INTERVAL 1 HOUR,                         'Ticket submitted',        11, 1, 4),

    (NOW() - INTERVAL 4 HOUR,                         'Ticket submitted',        12, 1, 9),
    (NOW() - INTERVAL 3 HOUR + INTERVAL -30 MINUTE,   'Assigned to technician',  12, 2, 8),
    (NOW() - INTERVAL 3 HOUR,                         'Ordering replacement keyboard', 12, 3, 1);

-- ---------------------------------------------------------------
-- COMMENTS
-- ---------------------------------------------------------------
INSERT INTO ticket_comment (comment_text, is_internal, date_posted, ticket_id, user_id) VALUES
    ('Laptop works now, thank you!',                     0, NOW() - INTERVAL 40 DAY + INTERVAL 29 HOUR, 1, 4),
    ('Pickup roller was worn out. Ordered spares.',      1, NOW() - INTERVAL 35 DAY + INTERVAL 30 HOUR, 2, 1),
    ('VPN client updated, still monitoring.',            0, NOW() - INTERVAL 8 DAY,                     5, 2),
    ('Same issue on Tuesday afternoon.',                 0, NOW() - INTERVAL 2 DAY,                    10, 5),
    ('Possibly interference from the new projector.',    1, NOW() - INTERVAL 1 DAY,                    10, 2);

-- Quick check
SELECT COUNT(*) AS users   FROM user_account;
SELECT COUNT(*) AS assets  FROM asset;
SELECT COUNT(*) AS tickets FROM ticket;
