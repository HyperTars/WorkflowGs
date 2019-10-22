package com.gs.photo.workflow;

import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.gs.photos.serializers.ExchangedDataSerDe;
import com.gs.photos.serializers.FinalImageSerDe;
import com.gs.photos.serializers.HbaseImageThumbnailSerDe;
import com.workflow.model.ExchangedTiffData;
import com.workflow.model.HbaseImageThumbnail;
import com.workflow.model.storm.FinalImage;

@Configuration
@PropertySource("file:${user.home}/config/application.properties")
public class ApplicationConfig extends AbstractApplicationConfig {

	private static final Logger     LOGGER                      = LoggerFactory.getLogger(ApplicationConfig.class);
	public static final int         JOIN_WINDOW_TIME            = 86400;
	public static final short       EXIF_CREATION_DATE_ID       = (short) 0x9003;
	private final DateTimeFormatter FORMATTER_FOR_CREATION_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

	@Value("${topic.topicDupFilteredFile}")
	protected String                topicDupFilteredFile;

	@Bean
	public Properties kafkaStreamProperties() {
		Properties config = new Properties();
		config.put(StreamsConfig.APPLICATION_ID_CONFIG,
				this.applicationGroupId + "-duplicate-streams");
		config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
				this.bootstrapServers);
		config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
				Serdes.String().getClass());
		config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
				Serdes.String().getClass());
		config.put(StreamsConfig.STATE_DIR_CONFIG,
				this.kafkaStreamDir);
		config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
				StreamsConfig.EXACTLY_ONCE);
		config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,
				"0");

		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
				"earliest");
		config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG,
				0);

		return config;
	}

	@Bean
	public Topology kafkaStreamsTopology() {
		StreamsBuilder builder = new StreamsBuilder();
		KTable<String, String> imageKTable = this.buildKTableToStoreCreatedImages(builder);
		KStream<String, String> pathOfImageKStream = imageKTable.toStream();

		KStream<String, FinalImage> thumbImages = this.buildKStreamToGetThumbImages(builder);
		KStream<String, ExchangedTiffData> exifOfImageStream = this.buildKStreamToGetExifValue(builder);
		KStream<String, ExchangedTiffData> filteredImageKStreamForCreationDate = exifOfImageStream
				.filter((key, exif) -> {
					boolean b = exif.getTag() == ApplicationConfig.EXIF_CREATION_DATE_ID;
					return b;
				}).map((k, v) -> {
					return new KeyValue<String, ExchangedTiffData>(k, v);
				});

		KStream<String, HbaseImageThumbnail> jointureToFindTheCreationDate = filteredImageKStreamForCreationDate.join(
				imageKTable,
				(v_exchangedTiffData, v_imagePath) -> {
					return this.buildHBaseImageThumbnail(v_exchangedTiffData,
							v_imagePath);
				},
				Joined.with(Serdes.String(),
						new ExchangedDataSerDe(),
						Serdes.String()));

		KStream<String, Long> imageCountsStream = jointureToFindTheCreationDate.flatMap((key, value) -> {
			return this.splitCreationDateToYearMonthDayAndHour(value);
		});

		imageCountsStream.to(this.topicImageDate,
				Produced.with(Serdes.String(),
						Serdes.Long()));

		KStream<String, HbaseImageThumbnail> finalStream = jointureToFindTheCreationDate.join(thumbImages,
				(v_hbaseImageThumbnail, v_finalImage) -> {
					return this.buildHbaseImageThumbnail(v_finalImage,
							v_hbaseImageThumbnail);
				},
				JoinWindows.of(ApplicationConfig.JOIN_WINDOW_TIME),
				Joined.with(Serdes.String(),
						new HbaseImageThumbnailSerDe(),
						new FinalImageSerDe()));

		KStream<String, HbaseImageThumbnail> final2Stream = finalStream.join(pathOfImageKStream,
				(v_hbaseImageThumbnail, v_path) -> {
					v_hbaseImageThumbnail.setPath(v_path);
					return v_hbaseImageThumbnail;
				},
				JoinWindows.of(ApplicationConfig.JOIN_WINDOW_TIME),
				Joined.with(Serdes.String(),
						new HbaseImageThumbnailSerDe(),
						Serdes.String()));

		this.publishImageDataInRecordTopic(final2Stream);

		return builder.build();

	}

	protected HbaseImageThumbnail buildHbaseImageThumbnail(FinalImage v_FinalImage,
			HbaseImageThumbnail v_hbaseImageThumbnail) {
		HbaseImageThumbnail retValue = null;
		if (v_hbaseImageThumbnail != null) {
			retValue = v_hbaseImageThumbnail;
			retValue.setThumbnail(v_FinalImage.getCompressedImage());
			retValue.setHeight(v_FinalImage.getHeight());
			retValue.setWidth(v_FinalImage.getWidth());
			retValue.setOrignal(v_FinalImage.isOriginal());
		}
		return retValue;
	}

	private HbaseImageThumbnail buildHBaseImageThumbnail(ExchangedTiffData key, String value) {
		HbaseImageThumbnail.Builder builder = HbaseImageThumbnail.builder();
		try {
			builder.withImageId(key.getImageId())
					.withCreationDate(DateTimeHelper.toEpochMillis(new String(key.getDataAsByte(), "UTF-8").trim()));
		} catch (UnsupportedEncodingException e) {
			ApplicationConfig.LOGGER.error("unsupported charset ",
					e);
		}
		return builder.build();
	}

	private Iterable<? extends KeyValue<String, Long>> splitCreationDateToYearMonthDayAndHour(
			HbaseImageThumbnail value) {
		OffsetDateTime ldt = DateTimeHelper.toLocalDateTime(value.getCreationDate());

		List<KeyValue<String, Long>> retValue;
		String keyYear = "Y:" + (long) ldt.getYear();
		String keyMonth = keyYear + "/M:" + (long) ldt.getMonthValue();
		String keyDay = keyMonth + "/D:" + (long) ldt.getDayOfMonth();
		String keyHour = keyDay + "/H:" + (long) ldt.getHour();
		String keyMinute = keyHour + "/Mn:" + (long) ldt.getMinute();
		String keySeconde = keyMinute + "/S:" + (long) ldt.getSecond();
		retValue = Arrays.asList(new KeyValue<String, Long>(keyYear, 1L),
				new KeyValue<String, Long>(keyMonth, 1L),
				new KeyValue<String, Long>(keyDay, 1L),
				new KeyValue<String, Long>(keyHour, 1L),
				new KeyValue<String, Long>(keyMinute, 1L),
				new KeyValue<String, Long>(keySeconde, 1L));
		return retValue;
	}

	protected KTable<String, String> buildKTableToStoreCreatedImages(StreamsBuilder builder) {
		return builder.table(this.topicDupFilteredFile,
				Consumed.with(Serdes.String(),
						Serdes.String()));
	}

	protected KStream<String, String> buildKTableToGetPathValue(StreamsBuilder streamsBuilder) {
		KStream<String, String> stream = streamsBuilder.stream(this.topicDupFilteredFile,
				Consumed.with(Serdes.String(),
						Serdes.String()));
		return stream;
	}

	protected KStream<String, FinalImage> buildKStreamToGetThumbImages(StreamsBuilder streamsBuilder) {
		KStream<String, FinalImage> stream = streamsBuilder.stream(this.topicTransformedThumb,
				Consumed.with(Serdes.String(),
						new FinalImageSerDe()));
		return stream;
	}

	public KStream<String, ExchangedTiffData> buildKStreamToGetExifValue(StreamsBuilder streamsBuilder) {
		KStream<String, ExchangedTiffData> stream = streamsBuilder.stream(this.topicExif,
				Consumed.with(Serdes.String(),
						new ExchangedDataSerDe()));
		return stream;
	}

	protected void publishImageDataInRecordTopic(KStream<String, HbaseImageThumbnail> finalStream) {
		finalStream.to(this.topicImageDataToPersist,
				Produced.with(Serdes.String(),
						new HbaseImageThumbnailSerDe()));
	}

}
