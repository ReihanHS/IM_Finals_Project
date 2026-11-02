-- IT Help Desk Ticketing System
-- Week 2: MySQL database (tables, keys, constraints, test data)
-- Class 4-310 | Group 3
-- Run with: mysql -u root -p < 4-310G3W2.sql

DROP DATABASE IF EXISTS it_helpdesk;
CREATE DATABASE it_helpdesk;
USE it_helpdesk;

-- ---------------------------------------------------------------
-- TABLES
-- ---------------------------------------------------------------

CREATE TABLE department (
    department_id    INT AUTO_INCREMENT PRIMARY KEY,
    department_name  VARCHAR(100) NOT NULL UNIQUE,
    location         VARCHAR(100)
);

CREATE TABLE support_team (
    team_id      INT AUTO_INCREMENT PRIMARY KEY,
    team_name    VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(255)
);

CREATE TABLE priority (
    priority_id          INT AUTO_INCREMENT PRIMARY KEY,
    priority_name        VARCHAR(30) NOT NULL UNIQUE,
    response_time_hrs    INT NOT NULL,
    resolution_time_hrs  INT NOT NULL
);

CREATE TABLE ticket_status (
    status_id    INT AUTO_INCREMENT PRIMARY KEY,
    status_name  VARCHAR(30) NOT NULL UNIQUE,
    description  VARCHAR(255)
);

CREATE TABLE asset (
    asset_id       INT AUTO_INCREMENT PRIMARY KEY,
    asset_tag      VARCHAR(30) NOT NULL UNIQUE,
    asset_type     VARCHAR(50) NOT NULL,
    brand          VARCHAR(50),
    model          VARCHAR(80),
    serial_number  VARCHAR(60),
    location       VARCHAR(100),
    asset_status   ENUM('In Use', 'In Storage', 'Under Repair', 'Retired') NOT NULL DEFAULT 'In Use'
);

CREATE TABLE category (
    category_id    INT AUTO_INCREMENT PRIMARY KEY,
    category_name  VARCHAR(100) NOT NULL UNIQUE,
    description    VARCHAR(255),
    team_id        INT NOT NULL,
    FOREIGN KEY (team_id) REFERENCES support_team (team_id)
);

CREATE TABLE user_account (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    email           VARCHAR(100) NOT NULL UNIQUE,
    contact_number  VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('Requester', 'Technician', 'Administrator') NOT NULL DEFAULT 'Requester',
    is_active       BOOLEAN NOT NULL DEFAULT 1,
    date_created    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    department_id   INT NOT NULL,
    team_id         INT,
    FOREIGN KEY (department_id) REFERENCES department (department_id),
    FOREIGN KEY (team_id) REFERENCES support_team (team_id)
);

CREATE TABLE ticket (
    ticket_id      INT AUTO_INCREMENT PRIMARY KEY,
    subject        VARCHAR(150) NOT NULL,
    description    TEXT NOT NULL,
    date_created   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated   DATETIME,
    date_resolved  DATETIME,
    date_closed    DATETIME,
    requester_id   INT NOT NULL,
    technician_id  INT,
    category_id    INT NOT NULL,
    priority_id    INT NOT NULL,
    status_id      INT NOT NULL,
    asset_id       INT,
    FOREIGN KEY (requester_id)  REFERENCES user_account (user_id),
    FOREIGN KEY (technician_id) REFERENCES user_account (user_id),
    FOREIGN KEY (category_id)   REFERENCES category (category_id),
    FOREIGN KEY (priority_id)   REFERENCES priority (priority_id),
    FOREIGN KEY (status_id)     REFERENCES ticket_status (status_id),
    FOREIGN KEY (asset_id)      REFERENCES asset (asset_id)
);

CREATE TABLE ticket_comment (
    comment_id    INT AUTO_INCREMENT PRIMARY KEY,
    comment_text  TEXT NOT NULL,
    is_internal   BOOLEAN NOT NULL DEFAULT 0,
    date_posted   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id     INT NOT NULL,
    user_id       INT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id),
    FOREIGN KEY (user_id)   REFERENCES user_account (user_id)
);

CREATE TABLE attachment (
    attachment_id  INT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    file_type      VARCHAR(100) NOT NULL,
    file_size_kb   INT NOT NULL,
    file_path      VARCHAR(500) NOT NULL,
    date_uploaded  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id      INT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id)
);

CREATE TABLE ticket_history (
    history_id    INT AUTO_INCREMENT PRIMARY KEY,
    date_changed  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks       VARCHAR(255),
    ticket_id     INT NOT NULL,
    status_id     INT NOT NULL,
    changed_by    INT NOT NULL,
    FOREIGN KEY (ticket_id)  REFERENCES ticket (ticket_id),
    FOREIGN KEY (status_id)  REFERENCES ticket_status (status_id),
    FOREIGN KEY (changed_by) REFERENCES user_account (user_id)
);

CREATE TABLE feedback (
    feedback_id     INT AUTO_INCREMENT PRIMARY KEY,
    rating          TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments        VARCHAR(500),
    date_submitted  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ticket_id       INT NOT NULL UNIQUE,
    FOREIGN KEY (ticket_id) REFERENCES ticket (ticket_id)
);

-- ---------------------------------------------------------------
-- STARTING DATA
-- (only the lookup lists and the technicians; no sample tickets)
-- ---------------------------------------------------------------

INSERT INTO department (department_name, location) VALUES
    ('Information Technology', '3rd Floor'),
    ('Human Resources', '2nd Floor'),
    ('Finance', '2nd Floor');

INSERT INTO support_team (team_name, description) VALUES
    ('Hardware Support', 'Computers and printers'),
    ('Network Support', 'Network and VPN'),
    ('Software Support', 'Software and accounts');

INSERT INTO category (category_name, team_id) VALUES
    ('Computer Hardware', 1),
    ('Printer Issues', 1),
    ('Network Connectivity', 2),
    ('Software Installation', 3);

INSERT INTO priority (priority_name, response_time_hrs, resolution_time_hrs) VALUES
    ('Critical', 1, 4),
    ('High', 2, 8),
    ('Medium', 4, 24),
    ('Low', 8, 72);

INSERT INTO ticket_status (status_name, description) VALUES
    ('Open', 'Waiting to be assigned'),
    ('Assigned', 'Assigned to a technician'),
    ('In Progress', 'Technician is working on it'),
    ('Resolved', 'Fixed, waiting for requester to confirm'),
    ('Closed', 'Requester confirmed the fix'),
    ('Reopened', 'Problem came back');

INSERT INTO user_account (first_name, last_name, email, password_hash, role, department_id, team_id) VALUES
    ('Jose', 'Ramirez', 'jose@example.com', 'hash2', 'Technician', 1, 1),
    ('Miguel', 'Torres', 'miguel@example.com', 'hash3', 'Technician', 1, 2),
    ('Paolo', 'Navarro', 'paolo@example.com', 'hash4', 'Technician', 1, 3);

-- ---------------------------------------------------------------
-- INSTRUCTOR CHECKPOINT
-- ---------------------------------------------------------------
-- SHOW TABLES;
SELECT * FROM user_account;
SELECT * FROM ticket;
SELECT * FROM ticket_history;


