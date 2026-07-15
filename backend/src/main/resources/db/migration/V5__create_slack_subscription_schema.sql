CREATE TABLE slack_workspaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slack_team_id VARCHAR(50) NOT NULL,
    slack_team_name VARCHAR(100) NOT NULL,
    encrypted_bot_token VARCHAR(1000) NOT NULL,
    bot_user_id VARCHAR(50) NULL,
    scope VARCHAR(500) NULL,
    installed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_slack_workspaces_team_id UNIQUE (slack_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE slack_channels (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slack_workspace_id BIGINT NOT NULL,
    slack_channel_id VARCHAR(50) NOT NULL,
    slack_channel_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_slack_channels_workspace_channel UNIQUE (slack_workspace_id, slack_channel_id),
    CONSTRAINT fk_slack_channels_workspace FOREIGN KEY (slack_workspace_id) REFERENCES slack_workspaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE slack_channel_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slack_channel_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_slack_channel_subscriptions_channel_company UNIQUE (slack_channel_id, company_id),
    CONSTRAINT fk_slack_channel_subscriptions_channel FOREIGN KEY (slack_channel_id) REFERENCES slack_channels (id),
    CONSTRAINT fk_slack_channel_subscriptions_company FOREIGN KEY (company_id) REFERENCES companies (id),
    INDEX idx_slack_channel_subscriptions_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
