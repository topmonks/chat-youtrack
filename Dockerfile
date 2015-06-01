# Hipchat Youtrack integration
#
# VERSION               0.0.1
#
FROM java:7-jdk
MAINTAINER Jiri filemon Fabian "jiri.fabian@topmonks.com"
#VOLUME /var/data
RUN apt-get update
RUN apt-get install -y maven

RUN cd /home && git clone https://github.com/filemon/fluent-hc-nossl-checks
RUN cd /home/fluent-hc-nossl-checks && mvn install

RUN mkdir /home/chat-youtrack

WORKDIR /home/chat-youtrack
CMD mvn -DHIPCHAT_AUTH_TOKEN=$HIPCHAT_AUTH_TOKEN -DHIPCHAT_ROOM_ID=$HIPCHAT_ROOM_ID -DYOUTRACK_PROJECT=$YOUTRACK_PROJECT -DAPP_DATA_DIR=/home/chat-youtrack -DYOUTRACK_URL=$YOUTRACK_URL -DISSUE_HISTORY_WINDOW=10 -DYOUTRACK_USERNAME=$YOUTRACK_USERNAME -DYOUTRACK_PASSWORD=$YOUTRACK_PASSWORD jetty:run
