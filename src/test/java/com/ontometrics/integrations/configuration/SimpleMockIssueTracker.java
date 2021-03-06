package com.ontometrics.integrations.configuration;

import com.ontometrics.integrations.events.Issue;
import ontometrics.test.util.TestUtil;

import java.net.URL;

/**
 *  Mock issue tracker
 *
 * MockIssueTracker.java
 */
public class SimpleMockIssueTracker implements IssueTracker {
    private String filePathToFeed;
    private String filePathToChanges;
    private String filePathToAttachment;

    public SimpleMockIssueTracker(Builder builder) {
        filePathToFeed = builder.filePathToFeed;
        filePathToChanges = builder.filePathToChanges;
        filePathToAttachment = builder.filePathToAttachment;
    }

    public static class Builder {

        private String filePathToFeed;
        private String filePathToChanges;
        private String filePathToAttachment;

        public Builder feed(String filePathToFeed){
            this.filePathToFeed = filePathToFeed;
            return this;
        }

        public Builder changes(String filePathToChanges){
            this.filePathToChanges = filePathToChanges;
            return this;
        }

        public Builder attachments(String filePathToAttachment){
            this.filePathToAttachment = filePathToAttachment;
            return this;
        }

        public SimpleMockIssueTracker build(){
            return new SimpleMockIssueTracker(this);
        }
    }

    @Override
    public URL getBaseUrl() {
        return null;
    }

    @Override
    public URL getFeedUrl() {
        return TestUtil.getFileAsURL(filePathToFeed);
    }

    @Override
    public URL getChangesUrl(Issue issue) {
        return TestUtil.getFileAsURL(filePathToChanges);
    }

    @Override
    public URL getAttachmentsUrl(Issue issue) {
        return TestUtil.getFileAsURL(filePathToAttachment);
    }

}