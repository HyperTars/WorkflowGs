package com.workflow.model;

import java.io.Serializable;
import java.util.Arrays;

import javax.annotation.Generated;

import org.apache.avro.reflect.Nullable;

@HbaseTableName("image_exif")
public class HbaseExifData extends HbaseData implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	// Row key
	@Column(hbaseName = "exif_tag", isPartOfRowkey = true, rowKeyNumber = 0, toByte = ToByteLong.class, fixedWidth = ModelConstants.FIXED_WIDTH_EXIF_TAG)
	protected long            exifTag;
	@Column(hbaseName = "image_id", isPartOfRowkey = true, rowKeyNumber = 1, toByte = ToByteString.class, fixedWidth = ModelConstants.FIXED_WIDTH_IMAGE_ID)
	protected String          imageId;

	// Data

	@Nullable
	@Column(hbaseName = "exv_bytes", toByte = ToByteIdempotent.class, columnFamily = "exv", rowKeyNumber = 100)
	protected byte[]          exifValueAsByte;
	@Nullable
	@Column(hbaseName = "exv_ints", toByte = ToByteIntArray.class, columnFamily = "exv", rowKeyNumber = 100)
	protected int[]           exifValueAsInt;
	@Nullable
	@Column(hbaseName = "exv_shorts", toByte = ToByteShortArray.class, columnFamily = "exv", rowKeyNumber = 100)
	protected short[]         exifValueAsShort;
	@Nullable
	@Column(hbaseName = "thumb_name", toByte = ToByteString.class, columnFamily = "imd", rowKeyNumber = 101)
	protected String          thumbName        = "";
	@Column(hbaseName = "creation_date", toByte = ToByteLong.class, columnFamily = "imd", rowKeyNumber = 102)
	protected long            creationDate;
	@Column(hbaseName = "width", toByte = ToByteLong.class, columnFamily = "sz", rowKeyNumber = 103)
	protected long            width;
	@Column(hbaseName = "height", toByte = ToByteLong.class, columnFamily = "sz", rowKeyNumber = 104)
	protected long            height;

	@Generated("SparkTools")
	private HbaseExifData(
			Builder builder) {
		this.exifTag = builder.exifTag;
		this.imageId = builder.imageId;
		this.exifValueAsByte = builder.exifValueAsByte;
		this.exifValueAsInt = builder.exifValueAsInt;
		this.exifValueAsShort = builder.exifValueAsShort;
		this.thumbName = builder.thumbName;
		this.creationDate = builder.creationDate;
		this.width = builder.width;
		this.height = builder.height;
	}

	public HbaseExifData() {
	}

	public long getExifTag() {
		return this.exifTag;
	}

	public void setExifTag(long exifTag) {
		this.exifTag = exifTag;
	}

	public String getImageId() {
		return this.imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public long getWidth() {
		return this.width;
	}

	public void setWidth(long width) {
		this.width = width;
	}

	public long getHeight() {
		return this.height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public String getThumbName() {
		return this.thumbName;
	}

	public void setThumbName(String thumbName) {
		this.thumbName = thumbName;
	}

	public byte[] getExifValueAsByte() {
		return this.exifValueAsByte;
	}

	public int[] getExifValueAsInt() {
		return this.exifValueAsInt;
	}

	public short[] getExifValueAsShort() {
		return this.exifValueAsShort;
	}

	public long getCreationDate() {
		return this.creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + (int) (this.creationDate ^ (this.creationDate >>> 32));
		result = (prime * result) + (int) (this.exifTag ^ (this.exifTag >>> 32));
		result = (prime * result) + Arrays.hashCode(this.exifValueAsByte);
		result = (prime * result) + Arrays.hashCode(this.exifValueAsInt);
		result = (prime * result) + Arrays.hashCode(this.exifValueAsShort);
		result = (prime * result) + (int) (this.height ^ (this.height >>> 32));
		result = (prime * result) + ((this.imageId == null) ? 0 : this.imageId.hashCode());
		result = (prime * result) + ((this.thumbName == null) ? 0 : this.thumbName.hashCode());
		result = (prime * result) + (int) (this.width ^ (this.width >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		HbaseExifData other = (HbaseExifData) obj;
		if (this.creationDate != other.creationDate) {
			return false;
		}
		if (this.exifTag != other.exifTag) {
			return false;
		}
		if (!Arrays.equals(this.exifValueAsByte,
				other.exifValueAsByte)) {
			return false;
		}
		if (!Arrays.equals(this.exifValueAsInt,
				other.exifValueAsInt)) {
			return false;
		}
		if (!Arrays.equals(this.exifValueAsShort,
				other.exifValueAsShort)) {
			return false;
		}
		if (this.height != other.height) {
			return false;
		}
		if (this.imageId == null) {
			if (other.imageId != null) {
				return false;
			}
		} else if (!this.imageId.equals(other.imageId)) {
			return false;
		}
		if (this.thumbName == null) {
			if (other.thumbName != null) {
				return false;
			}
		} else if (!this.thumbName.equals(other.thumbName)) {
			return false;
		}
		if (this.width != other.width) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HbaseExifData [exifTag=" + this.exifTag + ", imageId=" + this.imageId + ", exifValueAsByte="
				+ Arrays.toString(this.exifValueAsByte) + ", exifValueAsInt=" + Arrays.toString(this.exifValueAsInt)
				+ ", exifValueAsShort=" + Arrays.toString(this.exifValueAsShort) + ", thumbName=" + this.thumbName
				+ ", creationDate=" + this.creationDate + ", width=" + this.width + ", height=" + this.height + "]";
	}

	/**
	 * Creates builder to build {@link HbaseExifData}.
	 *
	 * @return created builder
	 */
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder to build {@link HbaseExifData}.
	 */
	@Generated("SparkTools")
	public static final class Builder {
		private long    exifTag;
		private String  imageId;
		private byte[]  exifValueAsByte;
		private int[]   exifValueAsInt;
		private short[] exifValueAsShort;
		private String  thumbName;
		private long    creationDate;
		private long    width;
		private long    height;

		private Builder() {
		}

		/**
		 * Builder method for exifTag parameter.
		 *
		 * @param exifTag
		 *            field to set
		 * @return builder
		 */
		public Builder withExifTag(long exifTag) {
			this.exifTag = exifTag;
			return this;
		}

		/**
		 * Builder method for imageId parameter.
		 *
		 * @param imageId
		 *            field to set
		 * @return builder
		 */
		public Builder withImageId(String imageId) {
			this.imageId = imageId;
			return this;
		}

		/**
		 * Builder method for exifValueAsByte parameter.
		 *
		 * @param exifValueAsByte
		 *            field to set
		 * @return builder
		 */
		public Builder withExifValueAsByte(byte[] exifValueAsByte) {
			this.exifValueAsByte = exifValueAsByte;
			return this;
		}

		/**
		 * Builder method for exifValueAsInt parameter.
		 *
		 * @param exifValueAsInt
		 *            field to set
		 * @return builder
		 */
		public Builder withExifValueAsInt(int[] exifValueAsInt) {
			this.exifValueAsInt = exifValueAsInt;
			return this;
		}

		/**
		 * Builder method for exifValueAsShort parameter.
		 *
		 * @param exifValueAsShort
		 *            field to set
		 * @return builder
		 */
		public Builder withExifValueAsShort(short[] exifValueAsShort) {
			this.exifValueAsShort = exifValueAsShort;
			return this;
		}

		/**
		 * Builder method for thumbName parameter.
		 *
		 * @param thumbName
		 *            field to set
		 * @return builder
		 */
		public Builder withThumbName(String thumbName) {
			this.thumbName = thumbName;
			return this;
		}

		/**
		 * Builder method for creationDate parameter.
		 *
		 * @param creationDate
		 *            field to set
		 * @return builder
		 */
		public Builder withCreationDate(long creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		/**
		 * Builder method for width parameter.
		 *
		 * @param width
		 *            field to set
		 * @return builder
		 */
		public Builder withWidth(long width) {
			this.width = width;
			return this;
		}

		/**
		 * Builder method for height parameter.
		 *
		 * @param height
		 *            field to set
		 * @return builder
		 */
		public Builder withHeight(long height) {
			this.height = height;
			return this;
		}

		/**
		 * Builder method of the builder.
		 *
		 * @return built class
		 */
		public HbaseExifData build() {
			return new HbaseExifData(this);
		}
	}

}
