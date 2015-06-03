package com.ontometrics.integrations.jobs;

import com.ontometrics.integrations.configuration.*;
import com.ontometrics.integrations.events.Issue;
import com.ontometrics.integrations.events.IssueEditSession;
import com.ontometrics.integrations.events.ProcessEvent;
import com.ontometrics.integrations.sources.EditSessionsExtractor;
import com.ontometrics.integrations.sources.StreamProvider;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created on 8/18/14.
 *
 */
public class EventListenerImpl implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(EventListenerImpl.class);

    private static final Comparator<IssueEditSession> CREATED_TIME_COMPARATOR = new Comparator<IssueEditSession>() {
        @Override
        public int compare(IssueEditSession s1, IssueEditSession s2) {
            return s1.getUpdated().compareTo(s2.getUpdated());
        }
    };

    private static final Comparator<ProcessEvent> UPDATED_TIME_COMPARATOR = new Comparator<ProcessEvent>() {
        @Override
        public int compare(ProcessEvent s1, ProcessEvent s2) {
            return s1.getPublishDate().compareTo(s2.getPublishDate());
        }
    };


    private ChatServer chatServer;

    private EditSessionsExtractor editSessionsExtractor;

    /**
     * @param feedStreamProvider feed resource provider
     * @param chatServer chat server
     */
    public EventListenerImpl(StreamProvider feedStreamProvider, ChatServer chatServer) {
        this(createEditSessionExtractor(feedStreamProvider), chatServer);
    }

    private static EditSessionsExtractor createEditSessionExtractor(StreamProvider feedStreamProvider) {
        if(feedStreamProvider == null) {
            throw new IllegalArgumentException("You must provide feedStreamProvider.");
        }
        Configuration configuration = ConfigurationFactory.get();
        return new EditSessionsExtractor(YouTrackInstanceFactory.createYouTrackInstance(configuration), feedStreamProvider);
    }

    /**
     * @param editSessionsExtractor editSessionsExtractor
     * @param chatServer chat server
     */
    public EventListenerImpl(EditSessionsExtractor editSessionsExtractor, ChatServer chatServer) {
        if(editSessionsExtractor == null || chatServer == null) {
            throw new IllegalArgumentException("You must provide sourceURL and chatServer.");
        }
        this.chatServer = chatServer;
        this.editSessionsExtractor = editSessionsExtractor;
    }

    /**
     * <p>
     * On wake, the job of this agent is simply to get any edits that have occurred since its last run from
     * the ticketing system (using the {@link com.ontometrics.integrations.sources.EditSessionsExtractor} and
     * then post them to the {@link com.ontometrics.integrations.configuration.ChatServer}.
     * </p>
     * <p>
     * This should stay simple: if we can't process a session for any reason we should skip it.
     * </p>
     *
     * @return the number of sessions that were processed
     * @throws Exception if it fails to save the last event date
     */
    @Override
    public int checkForNewEvents() throws Exception {
        //get events
        EventProcessorConfiguration eventProcessorConfiguration = EventProcessorConfiguration.instance();
        Date minDateOfEvents = eventProcessorConfiguration
                .resolveMinimumAllowedDate(eventProcessorConfiguration.loadLastProcessedDate());


        //this list is just to get the type of changes
        Map<Issue,IssueEditSession> editSessions = editSessionsExtractor.getLatestEditsMap(minDateOfEvents);

        List<ProcessEvent> events = editSessionsExtractor.getLatestEvents(minDateOfEvents);
        log.info("Found {} events to post.", events.size());

        final AtomicInteger processedSessionsCount = new AtomicInteger(0);
        if (events.size() > 0) {
            Collections.sort(events, UPDATED_TIME_COMPARATOR);
            log.debug("sessions: {}", events);
            Date lastProcessedSessionDate = null;
            for (ProcessEvent event : events) {
                IssueEditSession session = editSessions.get(event.getIssue());
                if(session != null) {
                    addUpdateInfo(event.getIssue(),session);
                }
                chatServer.postIssueCreation(event.getIssue());
                lastProcessedSessionDate = event.getPublishDate();
                processedSessionsCount.incrementAndGet();
            }

            log.debug("setting last processed date to: {}", lastProcessedSessionDate);
            EventProcessorConfiguration.instance().saveLastProcessedEventDate(lastProcessedSessionDate);
        }
        return processedSessionsCount.get();
    }

    private void addUpdateInfo (Issue issue,IssueEditSession session) {
        issue.setIsStatusUpdated(session.getIssue().isStatusUpdated());
    }


}
