package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.ChannelMapperFactory;
import com.ontometrics.integrations.sources.StatusMapper;
import com.ontometrics.util.NaiveClientBuilder;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by Filemon on 5/28/15.
 */
public class HipchatInstance implements ChatServer {

    private Logger log = getLogger(HipchatInstance.class);

    private static String API_URL = "https://api.hipchat.com";

    private static String TOKEN = ConfigurationFactory.get().getString("PROP.HIPCHAT_AUTH_TOKEN");
    private static String DEFAULT_ROOM = ConfigurationFactory.get().getString("PROP.DEFAULT_ROOM");
    private static String[] ROOM_MAPPINGS = ConfigurationFactory.get().getStringArray("PROP.ROOM_MAPPING");
    private static Pattern PATTERN = Pattern.compile("StatusMsg</th><td>(.*)</td>",Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static String NOTIFY_ON_STATE_UPDATE_ONLY = ConfigurationFactory.get().getString("PROP.NOTIFY_ON_STATE_UPDATE_ONLY", "");

    private ChannelMapper channelMapper;
    private StatusMapper statusMapper;

    public HipchatInstance() {
        channelMapper = ChannelMapperFactory.from(DEFAULT_ROOM, ROOM_MAPPINGS);
        statusMapper = new StatusMapper(NOTIFY_ON_STATE_UPDATE_ONLY);
    }

    @Override
    public void postIssueCreation(Issue issue) {
        postToChannel(channelMapper.getChannel(issue), issue);
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        if(statusMapper.statusUpdatesOnly(issueEditSession.getIssue()) && !issueEditSession.hasStateChanged()) {
            log.info("Doing nothing - notyfying only on status changes");
        } else {
            postToChannel(channelMapper.getChannel(issueEditSession.getIssue()), issueEditSession.getIssue());
        }
    }

    private void postToChannel(String room, Issue issue) {
        if (room != null) {
            String message = buildNewIssueMessage(issue);
            Form form = new Form()
                    .param("from", "YouTrack")
                    .param("message", message)
                    .param("room_id", room)
                    .param("color", "gray")
                    .param("notify", "false")
                    .param("message_format", "html");

            Client client = NaiveClientBuilder.newClient();
            Response response = client.target(API_URL + "/v1/rooms/message?auth_token=" + TOKEN)
                    .request()
                    .buildPost(Entity.form(form))
                    .invoke();

            if (response.getStatus() >= 400) {
                log.error("notification to hipchat failed - " + response.getStatus() + "\n " + response.readEntity(String.class));
            }
        }
    }

    public String buildNewIssueMessage(Issue issue){
        String description = issue.getDescription().replace("\n","");
        log.info("Description received: "+ description);
        Matcher matcher = PATTERN.matcher(description);
        String statusMsg;
        if(matcher.find()) {
            statusMsg = matcher.group(1);
        } else {
            statusMsg = "issue updated";
        }
        return String.format("%s <a href=\"%s\">%s</a>", statusMsg, issue.getLink(), issue.getTitle());
    }
}

