chat-youtrack
==============

Integration of slack and you track, and hipchat :)

Main functionality here is change management:

* show ticket changes
* linkify references to issues
* map the changes to the appropriate channel

Of course, to do this we have an agent being woken up regularly that:

1. goes to the RSS feed
1. for each item found there, calls into the REST interface to get changes
1. formats the change info
1. posts it to the channel

# Installation and configuration

## Local build and setup
------------
1. List of required properties
    * YOUTRACK_USERNAME - YouTrack username
    * YOUTRACK_PASSWORD - YouTrack password
    * Slack related options
        * SLACK_AUTH_TOKEN - token for authentication to Slack REST services
    * Hipchat related options
        * HIPCHAT_AUTH_TOKEN - token fo authentication to Hipchat REST services
    * ROOM_MAPPING - mapping of messages to rooms, eg. JOBS-general;WA-waroom
    * DEFAULT_ROOM - default room, where will be all not mapped messages sent
    * APP_DATA_DIR - directory where app will store it's data-files (configuration)
    * YOUTRACK_URL - YouTrack server url
    * ISSUE_HISTORY_WINDOW - Time In minutes - how deep should we look for issues in the past. If set to 10, it means that issues and changes that happened not longer than 10 minutes will be posted to chat server

2. Run mvn -DAPP_DATA_DIR=$PWD -DYOUTRACK_URL=YOUTRACK_URL -DISSUE_HISTORY_WINDOW=10 -DYOUTRACK_USERNAME=USERNAME -DYOUTRACK_PASSWORD=PASS jetty:run

## Docker setup

1. build -t chat-youtrack .
2. docker run -d -v $PWD:/home/chat-youtrack chat-youtrack -e YOUTRACK_URL=[YOUR_YOUTRACK_URL] -e .....

That's it.

Troubleshooting
------------

If you experience any problems, e.g. YouTrack updates are not posted to Slack channel, rebuild the project setting http client log level to DEBUG (so that all requests and responses are logged), redeploy and feel free to file an issue with information from the log. To set the log level to DEBUG, edit [src/main/resources/logback.xml](https://github.com/ontometrics/slack-youtrack/blob/master/src/main/resources/logback.xml) and uncomment lines

```xml
<logger name="org.apache.http" level="DEBUG" />
<logger name="com.ontometrics.integration.youtrack.response" level="DEBUG" />
```
