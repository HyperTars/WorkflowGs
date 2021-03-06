package com.gs.photo.workflow.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gs.photo.workflow.IBeanImageFileHelper;
import com.gs.photo.workflow.IBeanTaskExecutor;
import com.gs.photo.workflow.IIgniteDAO;
import com.gs.photo.workflow.IProcessInputForHashKeyCompute;
import com.gs.photo.workflow.TimeMeasurement;
import com.gs.photo.workflow.internal.KafkaManagedFileToProcess;
import com.workflow.model.files.FileToProcess;

@Service
public class BeanProcessInputForHashKeyCompute implements IProcessInputForHashKeyCompute {

    protected final Logger                    LOGGER = LoggerFactory.getLogger(IProcessInputForHashKeyCompute.class);
    @Value("${topic.topicScannedFiles}")
    protected String                          topicScanOutput;

    @Value("${topic.topicFileHashKey}")
    protected String                          topicHashKeyOutput;

    @Autowired
    @Qualifier("consumerForTopicWithFileToProcessValue")
    protected Consumer<String, FileToProcess> consumerForTopicWithFileToProcessValue;

    @Autowired
    @Qualifier("producerForTopicWithFileToProcessValue")
    protected Producer<String, FileToProcess> producerForTopicWithFileToProcessValue;

    @Value("${kafka.consumer.batchSizeForParallelProcessingIncomingRecords}")
    protected int                             batchSizeForParallelProcessingIncomingRecords;

    @Autowired
    protected IIgniteDAO                      igniteDAO;

    @Autowired
    protected IBeanTaskExecutor               beanTaskExecutor;

    @Autowired
    protected IBeanImageFileHelper            beanImageFileHelper;

    @Value("${kafka.pollTimeInMillisecondes}")
    protected int                             kafkaPollTimeInMillisecondes;

    @PostConstruct
    public void init() {
        this.consumerForTopicWithFileToProcessValue.subscribe(Collections.singleton((this.topicScanOutput)));
        this.beanTaskExecutor.execute(() -> this.processIncomingFile());
    }

    protected void processIncomingFile() {
        boolean ready = true;
        do {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                ready = false;
                break;
            }
        } while (!this.igniteDAO.isReady());
        this.LOGGER.info("Ignite is finally ready, let's go !!!");
        while (ready) {
            try (
                TimeMeasurement timeMeasurement = TimeMeasurement.of(
                    "BATCH_PROCESS_FILES",
                    (d) -> this.LOGGER.info(" Perf. metrics {}", d),
                    System.currentTimeMillis())) {
                Stream<ConsumerRecord<String, FileToProcess>> stream = KafkaUtils.toStreamV2(
                    this.kafkaPollTimeInMillisecondes,
                    this.consumerForTopicWithFileToProcessValue,
                    this.batchSizeForParallelProcessingIncomingRecords,
                    true,
                    (i) -> this.loggerStart(i),
                    timeMeasurement);
                Map<TopicPartition, OffsetAndMetadata> offsets = stream.map((r) -> this.create(r))
                    .map((r) -> this.saveInIgnite(r))
                    .map((r) -> this.sendToNext(r))
                    .collect(
                        () -> new ConcurrentHashMap<TopicPartition, OffsetAndMetadata>(),
                        (mapOfOffset, t) -> this.updateMapOfOffset(mapOfOffset, t),
                        (r, t) -> this.merge(r, t));
                this.LOGGER.info("Offset to commit {} ", offsets);
                this.consumerForTopicWithFileToProcessValue.commitSync(offsets);
                this.producerForTopicWithFileToProcessValue.flush();
            } catch (Throwable e) {
                this.LOGGER.error("Unexpected error {} ", ExceptionUtils.getStackTrace(e));
            }
        }
        this.LOGGER.info("!! END OF PROCESS !!");
    }

    private void merge(Map<TopicPartition, OffsetAndMetadata> r, Map<TopicPartition, OffsetAndMetadata> t) {
        KafkaUtils.merge(r, t);
    }

    private void updateMapOfOffset(
        Map<TopicPartition, OffsetAndMetadata> mapOfOffset,
        KafkaManagedFileToProcess fileToProcess
    ) {
        KafkaUtils.updateMapOfOffset(
            mapOfOffset,
            fileToProcess,
            (f) -> f.getPartition(),
            (f) -> this.topicScanOutput,
            (f) -> f.getKafkaOffset());
    }

    private void loggerStart(int nbOfRecords) { this.LOGGER.info("Starting to process {} records", nbOfRecords); }

    private KafkaManagedFileToProcess sendToNext(KafkaManagedFileToProcess fileToProcess) {
        fileToProcess.getValue()
            .ifPresentOrElse((o) -> {
                o.setImageId(fileToProcess.getHashKey());
                this.producerForTopicWithFileToProcessValue.send(
                    new ProducerRecord<String, FileToProcess>(this.topicHashKeyOutput, fileToProcess.getHashKey(), o));
            },
                () -> this.LOGGER.warn(
                    "Error : offset {} of partition {} of topic {} is not processed",
                    fileToProcess.getKafkaOffset(),
                    fileToProcess.getPartition(),
                    this.topicScanOutput));
        return fileToProcess;
    }

    private KafkaManagedFileToProcess saveInIgnite(KafkaManagedFileToProcess r) {
        boolean saved = this.igniteDAO.save(r.getHashKey(), r.getRawFile());
        r.setRawFile(null);
        if (saved) {
            this.LOGGER.info("[EVENT][{}] saved in ignite {} ", r.getHashKey(), KafkaManagedFileToProcess.toString(r));
        } else {
            this.LOGGER.warn(
                "[EVENT][{}] not saved in ignite {} : was already seen before... ",
                r.getHashKey(),
                KafkaManagedFileToProcess.toString(r));
        }
        return r;
    }

    private KafkaManagedFileToProcess create(ConsumerRecord<String, FileToProcess> f) {
        try {
            byte[] rawFile = this.beanImageFileHelper.readFirstBytesOfFile(f.value());
            String key = this.beanImageFileHelper.computeHashKey(rawFile);

            this.LOGGER.info("[EVENT][{}] getting bytes to compute hash key, length is {}", key, rawFile.length);
            return KafkaManagedFileToProcess.builder()
                .withHashKey(key)
                .withRawFile(rawFile)
                .withValue(Optional.of(f.value()))
                .withKafkaOffset(f.offset())
                .withPartition(f.partition())
                .build();
        } catch (Throwable e) {
            this.LOGGER.warn("[EVENT][{}] Error {}", f, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }
}