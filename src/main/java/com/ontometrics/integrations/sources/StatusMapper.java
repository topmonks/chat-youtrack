package com.ontometrics.integrations.sources;

import com.ontometrics.integrations.events.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by filemon
 */
public class StatusMapper {

    private final List<String> projects;

    private static final Logger log = LoggerFactory.getLogger(StatusMapper.class);

    public StatusMapper(String listprojects) {
        projects = new ArrayList(Arrays.asList(listprojects.split(":")));
    }


    public boolean statusUpdatesOnly(final Issue issue){
        return projects.contains(issue.getPrefix());
    }

}
