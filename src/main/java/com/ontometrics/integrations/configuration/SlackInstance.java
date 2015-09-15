package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.StatusMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Rob on 8/23/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class SlackInstance implements ChatServer {

    private Logger log = getLogger(SlackInstance.class);
    public static final String BASE_URL = "https://slack.com";
    public static final String API_PATH = "api";
    public static final String CHANNEL_POST_PATH = "chat.postMessage";
    public static final String TOKEN_KEY = "token";
    public static final String TEXT_KEY = "text";
    public static final String CHANNEL_KEY = "channel";

    private static String NOTIFY_ON_STATE_UPDATE_ONLY = ConfigurationFactory.get().getString("PROP.NOTIFY_ON_STATE_UPDATE_ONLY", "");
    private static Pattern PATTERN = Pattern.compile("StatusMsg</th><td>(.*)</td>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private final ChannelMapper channelMapper;
    private StatusMapper statusMapper;

    public SlackInstance(Builder builder) {
        channelMapper = builder.channelMapper;
        statusMapper = new StatusMapper(NOTIFY_ON_STATE_UPDATE_ONLY);
    }

    public static class Builder {

        private ChannelMapper channelMapper;

        public Builder channelMapper(ChannelMapper channelMapper){
            this.channelMapper = channelMapper;
            return this;
        }

        public SlackInstance build(){
            return new SlackInstance(this);
        }
    }

    @Override
    public void postIssueCreation(Issue issue) {
        postToChannel(channelMapper.getChannel(issue), buildNewIssueMessage(issue));
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        if(statusMapper.statusUpdatesOnly(issueEditSession.getIssue()) && !issueEditSession.hasStateChanged()) {
            log.info("Doing nothing - notyfying only on status changes");
        } else {
            String channel = channelMapper.getChannel(issueEditSession.getIssue());
            postToChannel(channel, buildEditSessionMessage(issueEditSession));
        }
    }

    private void postToChannel(String channel, String message) {
        log.info("posting message {} to channel: {}.", message, channel);
        Client client = ClientBuilder.newClient();

        WebTarget slackApi = client.target(BASE_URL).path(String.format("%s/%s", API_PATH, CHANNEL_POST_PATH))
                .queryParam(TOKEN_KEY, ConfigurationFactory.get().getString("PROP.SLACK_AUTH_TOKEN"))
                .queryParam(TEXT_KEY, processMessage(message))
                .queryParam(CHANNEL_KEY, "#" + channel);

        Invocation.Builder invocationBuilder = slackApi.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        log.info("response code: {} response: {}", response.getStatus(), response.readEntity(String.class));

    }

    private String processMessage(String message) {
        return StringUtils.replaceChars(message, "{}", "[]");
    }


    public String buildNewIssueMessage(Issue issue){
        String description = issue.getDescription().replace("\n","");
        log.info("Description received: "+ description);
        Matcher matcher = PATTERN.matcher(description);
        String statusMsg;
        if(matcher.find()) {
            statusMsg = matcher.group(1);
        } else {
            statusMsg = "New ticket";
        }
        return String.format("%s <a href=\"%s\">%s</a>", statusMsg, issue.getLink(), issue.getTitle());
    }

    public String buildEditSessionMessage(IssueEditSession session){
        String statusMsg = session.getStatusMsg();
        if(statusMsg == null) {
            statusMsg = "Updated";
        }
        Issue issue = session.getIssue();
        return String.format(":monk: %s %s", statusMsg, MessageFormatter.getIssueLink(issue));
    }

    private static class MessageFormatter {
        static String getIssueLink(Issue issue){
            return String.format("<%s|%s>", issue.getLink(), issue.getTitle());
        }
    }
}

