package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.AttachmentEvent;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEdit;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.ChannelMapper;
import io.evanwong.oss.hipchat.v2.HipChatClient;
import io.evanwong.oss.hipchat.v2.commons.NoContent;
import io.evanwong.oss.hipchat.v2.rooms.MessageColor;
import io.evanwong.oss.hipchat.v2.rooms.SendRoomNotificationRequestBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Filemon on 5/28/15.
 */
public class HipchatInstance implements ChatServer {

    private Logger log = getLogger(HipchatInstance.class);

    private static String TOKEN;
    private static String ROOM;
    private static String PROJECT;
    private static Pattern PATTERN;


    static {
        TOKEN = ConfigurationFactory.get().getString("PROP.HIPCHAT_AUTH_TOKEN");
        ROOM = ConfigurationFactory.get().getString("PROP.HIPCHAT_ROOM_ID");
        PROJECT = ConfigurationFactory.get().getString("PROP.YOUTRACK_PROJECT");
        PATTERN = Pattern.compile("StatusMsg</th><td>(.*)</td>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    }


    @Override
    public void postIssueCreation(Issue issue) {
        postToChannel(ROOM, issue);
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        postToChannel(ROOM, issueEditSession.getIssue());
        
    }


    private void postToChannel(String room, Issue issue) {
        if(PROJECT.equalsIgnoreCase(issue.getPrefix())) {
            String message = buildNewIssueMessage(issue);
            log.info(String.format("Sending: %s to %s/%s", message,room,TOKEN));
            HipChatClient client = new HipChatClient(TOKEN);
            SendRoomNotificationRequestBuilder builder = client.prepareSendRoomNotificationRequestBuilder(room, message);
            builder.setNotify(true).build().execute();
        } else {
            log.info("Doing nothing, issue is from unrelated project: " + issue.getPrefix());
        }
    }

    public String buildNewIssueMessage(Issue newIssue){
        String description = newIssue.getDescription().replace("\n","");
        log.info("Description received: "+ description);
        Matcher matcher = PATTERN.matcher(description);
        String statusMsg;
        if(matcher.find()) {
            statusMsg = matcher.group(1);
        } else {
            statusMsg = "Updated";
        }
        return String.format(" %s : *%s* <a href=\"%s\">%s</a>", statusMsg, MessageFormatter.getTitleWithoutIssueID(newIssue),newIssue.getLink(),newIssue.getLink());

    }

    private static class MessageFormatter {
        static String getIssueLink(Issue issue){
            return String.format("<%s|%s-%d>", issue.getLink(), issue.getPrefix(), issue.getId());
        }

        static String getNamedLink(String url, String text){
            return String.format("<%s|%s>", url, text);
        }

        static String getTitleWithoutIssueID(Issue issue){
            return issue.getTitle().substring(issue.getTitle().indexOf(":") + 2);
        }
    }

}

