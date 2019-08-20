package com.gs.photo.workflow;

import java.util.Properties;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.gs.photos.serializers.ExchangedDataSerializer;
import com.workflow.model.ExchangedTiffData;
import com.workflow.model.HbaseImageThumbnail;

@Configuration
@PropertySource("file:${user.home}/config/application.properties")
public class ApplicationConfig {
	private static final String KAFKA_EXCHANGED_DATA_SERIALIZER = ExchangedDataSerializer.class.getName();
	final static String KAFKA_STRING_DESERIALIZER = org.apache.kafka.common.serialization.StringDeserializer.class
			.getName();
	final static String KAFKA_STRING_SERIALIZER = org.apache.kafka.common.serialization.StringSerializer.class
			.getName();
	final static String KAFKA_BYTES_DESERIALIZER = org.apache.kafka.common.serialization.ByteArrayDeserializer.class
			.getName();
	final static String KAFKA_BYTE_SERIALIZER = org.apache.kafka.common.serialization.ByteArraySerializer.class
			.getName();
	final static String HBASE_IMAGE_THUMBNAIL_SERIALIZER = com.gs.photos.serializers.HbaseImageThumbnailSerializer.class
			.getName();
	final static String HBASE_IMAGE_THUMBNAIL_DESERIALIZER = com.gs.photos.serializers.HbaseImageThumbnailDeserializer.class
			.getName();

	@Value("${application.id}")
	protected String applicationId;

	@Value("${application.kafkastreams.id}")
	protected String applicationGroupId;

	@Value("${group.id}")
	protected String groupId;

	@Value("${transaction.id}")
	protected String transactionId;

	@Value("${bootstrap.servers}")
	protected String bootstrapServers;

	@Value("${kafkaStreamDir.dir}")
	protected String kafkaStreamDir;

	@Value("${topic.inputImageNameTopic}")
	protected String inputImageNameTopic;

	@Value("${topic.inputExifTopic}")
	protected String inputExifTopic;

	@Value("${topic.recordThumbTopic}")
	protected String topicToRecordThumb;

	@Value("${topic.processedThumbTopic}")
	protected String processedThumbTopic;

	@Value("${copy.group.id}")
	protected String copyGroupId;

	@Value("${transaction.timeout}")
	protected String transactionTimeout;

	@Value("${hbase.master}")
	protected String hbaseMaster;

	@Value("${zookeeper.hosts}")
	protected String zookeeperHosts;

	@Value("${zookeeper.port}")
	protected int zookeeperPort;

	@Bean
	public Properties kafkaStreamProperties() {
		Properties config = new Properties();
		config.put(
			StreamsConfig.APPLICATION_ID_CONFIG,
			applicationGroupId + "-duplicate-streams");
		config.put(
			StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		config.put(
			StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
			Serdes.String().getClass());
		config.put(
			StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
			Serdes.String().getClass());
		config.put(
			StreamsConfig.STATE_DIR_CONFIG,
			kafkaStreamDir);
		config.put(
			StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
			StreamsConfig.EXACTLY_ONCE);
		config.put(
			StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
			"0");

		config.put(
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
			"earliest");
		config.put(
			StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,
			0);

		return config;
	}

	@Bean
	public org.apache.hadoop.conf.Configuration hbaseConfiguration() {

		org.apache.hadoop.conf.Configuration hBaseConfig = HBaseConfiguration.create();
		hBaseConfig.setInt(
			"timeout",
			120000);
		hBaseConfig.set(
			"hbase.zookeeper.quorum",
			zookeeperHosts);
		hBaseConfig.setInt(
			"hbase.zookeeper.property.clientPort",
			zookeeperPort);
		return hBaseConfig;
	}

	@Bean
	public Producer<String, String> producerForPublishingOnImageTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		settings.put(
			"value.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<>(settings);
		return producer;
	}

	@Bean
	public Producer<String, String> producerForPublishingOnStringTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		settings.put(
			"value.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<>(settings);
		return producer;
	}

	@Bean("producerForPublishingInModeTransactionalOnStringTopic")
	public Producer<String, String> producerForPublishingInModeTransactionalOnStringTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			ProducerConfig.TRANSACTIONAL_ID_CONFIG,
			transactionId);
		settings.put(
			ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
			"true");
		settings.put(
			ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,
			transactionTimeout);

		settings.put(
			"key.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		settings.put(
			"value.serializer",
			"org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<>(settings);
		return producer;
	}

	@Bean(name = "producerForPublishingOnExifTopic")
	public Producer<String, ExchangedTiffData> producerForPublishingOnExifTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"value.serializer",
			KAFKA_EXCHANGED_DATA_SERIALIZER);
		Producer<String, ExchangedTiffData> producer = new KafkaProducer<>(settings);
		return producer;
	}

	@Bean(name = "producerForPublishingOnJpegImageTopic")
	public Producer<String, byte[]> producerForPublishingOnJpegImageTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"value.serializer",
			KAFKA_BYTE_SERIALIZER);
		Producer<String, byte[]> producer = new KafkaProducer<>(settings);
		return producer;
	}

	@Bean(name = "consumerForTopicWithStringKey")
	public Consumer<String, String> consumerForTopicWithStringKey() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"key.deserializer",
			KAFKA_STRING_DESERIALIZER);
		settings.put(
			"value.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"value.deserializer",
			KAFKA_STRING_DESERIALIZER);
		settings.put(
			ConsumerConfig.CLIENT_ID_CONFIG,
			applicationId);
		settings.put(
			ConsumerConfig.GROUP_ID_CONFIG,
			groupId);
		settings.put(
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
			"earliest");
		settings.put(
			ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
			5);
		Consumer<String, String> producer = new KafkaConsumer<>(settings);
		return producer;
	}

	@Bean(name = "consumerForTransactionalCopyForTopicWithStringKey")
	public Consumer<String, String> consumerForTransactionalCopyForTopicWithStringKey() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"key.deserializer",
			KAFKA_STRING_DESERIALIZER);
		settings.put(
			"value.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"value.deserializer",
			KAFKA_STRING_DESERIALIZER);
		settings.put(
			ConsumerConfig.CLIENT_ID_CONFIG,
			"tr-" + applicationId);
		settings.put(
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
			"earliest");
		settings.put(
			ConsumerConfig.GROUP_ID_CONFIG,
			copyGroupId);
		settings.put(
			"enable.auto.commit",
			"false");
		settings.put(
			"isolation.level",
			"read_committed");
		settings.put(
			ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
			5);
		Consumer<String, String> producer = new KafkaConsumer<>(settings);
		return producer;
	}

	@Bean(name = "consumerForRecordingImageFromTopic")
	public Consumer<String, HbaseImageThumbnail> consumerForRecordingImageFromTopic() {
		Properties settings = new Properties();
		settings.put(
			CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
			bootstrapServers);
		settings.put(
			"key.serializer",
			KAFKA_STRING_SERIALIZER);
		settings.put(
			"key.deserializer",
			KAFKA_STRING_DESERIALIZER);
		settings.put(
			"value.serializer",
			HBASE_IMAGE_THUMBNAIL_SERIALIZER);
		settings.put(
			"value.deserializer",
			HBASE_IMAGE_THUMBNAIL_DESERIALIZER);
		settings.put(
			ConsumerConfig.CLIENT_ID_CONFIG,
			"tr-" + applicationId);
		settings.put(
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
			"earliest");
		settings.put(
			ConsumerConfig.GROUP_ID_CONFIG,
			copyGroupId);
		settings.put(
			"enable.auto.commit",
			"false");
		settings.put(
			"isolation.level",
			"read_committed");
		settings.put(
			ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
			5);
		Consumer<String, HbaseImageThumbnail> consumer = new KafkaConsumer<>(settings);
		return consumer;

	}

	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

		threadPoolTaskExecutor.setCorePoolSize(
			4);
		threadPoolTaskExecutor.setMaxPoolSize(
			10);
		threadPoolTaskExecutor.setThreadNamePrefix(
			"wf-task-executor");
		threadPoolTaskExecutor.initialize();
		return threadPoolTaskExecutor;
	}
}