package com.gs.photo.workflow.impl;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.gs.photo.workflow.IBeanScheduleRecordMessageInHbase;
import com.gs.photo.workflow.IBeanTaskExecutor;
import com.gs.photo.workflow.consumers.IConsumerForRecordHbaseExif;
import com.gs.photo.workflow.consumers.IConsumerForRecordHbaseImage;

@Component
@ConditionalOnProperty(name = "unit-test", havingValue = "false")
public class BeanScheduleRecordMessageInHbase implements IBeanScheduleRecordMessageInHbase {

    protected static final Logger          LOGGER = Logger.getLogger(BeanScheduleRecordMessageInHbase.class);

    @Autowired
    protected IConsumerForRecordHbaseImage consumerForRecordHbaseImage;

    @Autowired
    protected IConsumerForRecordHbaseExif  consumerForRecordHbaseExif;

    @Autowired
    protected IBeanTaskExecutor            beanTaskExecutor;

    @PostConstruct
    public void init() {

    }

}
