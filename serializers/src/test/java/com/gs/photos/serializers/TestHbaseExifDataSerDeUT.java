package com.gs.photos.serializers;

import org.junit.Assert;
import org.junit.Test;

import com.workflow.model.HbaseExifData;

public class TestHbaseExifDataSerDeUT {

	@Test
	public void test001_shouldSerializeAndDeserializeSuccessWithDefaultPojo() {
		HbaseExifDataSerializer ser = new HbaseExifDataSerializer();
		final HbaseExifData.Builder builder = HbaseExifData.builder();
		builder.withImageId("<imgId>");
		final HbaseExifData data = builder.build();
		byte[] results = ser.serialize(null,
				data);

		HbaseExifDataDeserializer deser = new HbaseExifDataDeserializer();

		HbaseExifData hit = deser.deserialize(null,
				results);

		Assert.assertNotNull(hit);

	}

	@Test
	public void test002_shouldSerializeAndDeserializeSuccessWithValuedPojo() {
		HbaseExifDataSerializer ser = new HbaseExifDataSerializer();
		final HbaseExifData.Builder builder = HbaseExifData.builder();
		HbaseExifData hbaseExifData = builder.withCreationDate(100).withHeight(768).withWidth(1024).withImageId("<img>")
				.withExifTag(25).withExifValueAsByte(new byte[] { 0, 1, 2 }).withThumbName("img-1").build();
		byte[] results = ser.serialize(null,
				hbaseExifData);

		HbaseExifDataDeserializer deser = new HbaseExifDataDeserializer();

		HbaseExifData hit = deser.deserialize(null,
				results);

		Assert.assertEquals(hbaseExifData,
				hit);
	}

}