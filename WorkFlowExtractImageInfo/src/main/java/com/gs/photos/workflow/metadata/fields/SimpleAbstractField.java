package com.gs.photos.workflow.metadata.fields;

import com.gs.photos.workflow.metadata.FileChannelDataInput;
import com.gs.photos.workflow.metadata.Tag;
import com.gs.photos.workflow.metadata.tiff.TiffField;

public abstract class SimpleAbstractField<T> {

	protected final int fieldLength;
	protected final int offset;
	protected final short type;

	public int getFieldLength() {
		return fieldLength;
	}

	protected SimpleAbstractField(int fieldLength, int offset, short type) {
		this.fieldLength = fieldLength;
		this.offset = offset;
		this.type = type;
	}

	public abstract TiffField<T> createTiffField(Tag tag, short tagValue);

	public short getType() {
		return type;
	}

	public abstract T getData();

	public abstract void updateData(FileChannelDataInput rin);

	public abstract int getNextOffset();

}