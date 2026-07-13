package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.slack.application.modal.SlackModalView;

public interface SlackViewClient {

    void open(String botToken, String triggerId, SlackModalView view);
}
