package com.gs.photo.workflow.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gsphotos.worflow.hbasefilters.FilterRowByLongAtAGivenOffset;
import com.workflow.model.HbaseData;
import com.workflow.model.HbaseTableName;

@Component
public class ImageFilterDAO extends AbstractDAO {

	@Autowired
	protected Configuration hbaseConfiguration;

	public <T extends HbaseData> List<T> getThumbNailsByDate(LocalDateTime firstDate, LocalDateTime lastDate,
			long minWidth, long minHeight, Class<T> cl) {
		List<T> retValue = new ArrayList<>();
		String tableName = cl.getAnnotation(
			HbaseTableName.class).value();
		try (Connection connection = ConnectionFactory.createConnection(
			hbaseConfiguration)) {
			try (Table table = connection.getTable(
				TableName.valueOf(
					tableName))) {
				Scan scan = new Scan();
				scan.addColumn(
					"image_data".getBytes(),
					"image_name".getBytes());
				scan.addColumn(
					"image_data".getBytes(),
					"thumb_name".getBytes());
				scan.addColumn(
					"thumb_data".getBytes(),
					"thumbnail".getBytes());
				scan.setFilter(
					new FilterRowByLongAtAGivenOffset(
						0,
						firstDate.toInstant(
							ZoneOffset.ofTotalSeconds(
								0)).toEpochMilli(),
						lastDate.toInstant(
							ZoneOffset.ofTotalSeconds(
								0)).toEpochMilli()));
				ResultScanner rs = table.getScanner(
					scan);
				rs.forEach(
					(t) -> {
						try {
							T instance = cl.newInstance();
							retValue.add(
								instance);
							byte[] row = t.getRow();
							HbaseDataInformation hbaseDataInformation = getHbaseDataInformation(
								cl);
							hbaseDataInformation.getFieldsData().forEach(
								(hdfi) -> {
									Object v = null;
									if (hdfi.partOfRowKey) {
										v = hdfi.fromByte(
											row,
											hdfi.offset,
											hdfi.fixedWidth);
									} else {
										byte[] value = t.getValue(
											hdfi.columnFamily.getBytes(),
											hdfi.hbaseName.getBytes());
										v = hdfi.fromByte(
											value,
											0,
											value.length);
									}
									try {
										hdfi.field.set(
											instance,
											v);
									} catch (IllegalArgumentException | IllegalAccessException e) {
										e.printStackTrace();
									}
								});
						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return retValue;

	}

	@Override
	public <T extends HbaseData> void put(T hbaseData, Class<T> cl) {

	}
}