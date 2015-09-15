# Hipchat Youtrack integration
#
# VERSION               0.0.1
#
FROM java:7-jdk
MAINTAINER Jiri filemon Fabian "jiri.fabian@topmonks.com"
RUN apt-get update
RUN apt-get install -y maven

RUN mkdir /home/chat-youtrack

WORKDIR /home/chat-youtrack
ADD . /home/chat-youtrack

CMD mvn -DNOTIFY_ON_STATE_UPDATE_ONLY=JOBS -DSLACK_AUTH_TOKEN=$SLACK_AUTH_TOKEN -Dyoutrack-slack.default-channel=$DEFAULT_CHANNEL -Dyoutrack-slack.channel-mappings=$ROOM_MAPPING -DAPP_DATA_DIR=/home/chat-youtrack -DYOUTRACK_URL=$YOUTRACK_URL -DISSUE_HISTORY_WINDOW=10 -DYOUTRACK_USERNAME=$YOUTRACK_USERNAME -DYOUTRACK_PASSWORD=$YOUTRACK_PASSWORD jetty:run
