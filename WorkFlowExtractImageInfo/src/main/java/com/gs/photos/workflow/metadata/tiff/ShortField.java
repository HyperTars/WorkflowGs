package com.gs.photos.workflow.metadata.tiff;

import com.gs.photo.workflow.exif.Tag;
import com.gs.photos.workflow.metadata.StringUtils;
import com.gs.photos.workflow.metadata.fields.SimpleAbstractField;

public final class ShortField extends TiffField<short[]> {
    private static final long serialVersionUID = 1L;

    public ShortField(
        Tag ifdTagParent,

        Tag tag,
        SimpleAbstractField<short[]> underLayingField,
        short tagValue
    ) {
        super(ifdTagParent,
            tag,
            underLayingField,
            tagValue,
            underLayingField.getOffset());
    }

    @Override
    public int[] getDataAsLong() {
        //
        final short[] data = this.underLayingField.getData();
        int[] temp = new int[data.length];

        for (int i = 0; i < data.length; i++) {
            temp[i] = data[i] & 0xffff;
        }

        return temp;
    }

    @Override
    public String getDataAsString() {
        return StringUtils.shortArrayToString(this.underLayingField.getData(), 0, 10, true);
    }
}