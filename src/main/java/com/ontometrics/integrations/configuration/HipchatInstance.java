package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.sources.ChannelMapper;
import com.ontometrics.integrations.sources.ChannelMapperFactory;
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

    private ChannelMapper channelMapper;

    public HipchatInstance() {
        channelMapper = ChannelMapperFactory.from(DEFAULT_ROOM, ROOM_MAPPINGS);
    }

    public HipchatInstance(Builder builder) {
        channelMapper = builder.channelMapper;
    }

    public static class Builder {

        private ChannelMapper channelMapper;

        public Builder channelMapper(ChannelMapper channelMapper){
            this.channelMapper = channelMapper;
            return this;
        }

        public HipchatInstance build(){
            return new HipchatInstance(this);
        }
    }

    @Override
    public void postIssueCreation(Issue issue) {
        postToChannel(channelMapper.getChannel(issue), issue);
    }

    @Override
    public void post(IssueEditSession issueEditSession){
        postToChannel(channelMapper.getChannel(issueEditSession.getIssue()), issueEditSession.getIssue());
        
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

