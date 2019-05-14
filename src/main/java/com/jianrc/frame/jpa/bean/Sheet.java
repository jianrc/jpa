package com.jianrc.frame.jpa.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public class Sheet<T> implements java.io.Serializable {

    public static final Sheet EMPTY = new EmptySheet(0, Collections.EMPTY_LIST);

    private long rowcount = -1;

    private Collection<T> data;

    public Sheet() {
        super();
    }

    public Sheet(int rowcount, Collection<? extends T> data) {
        this((long) rowcount, data);
    }

    public Sheet(long rowcount, Collection<? extends T> data) {
        this.rowcount = rowcount;
        this.data = (Collection<T>) data;
    }

    public static <E> Sheet<E> asSheet(Collection<E> data) {
        return new Sheet<E>(data.size(), data);
    }

    public void copyTo(Sheet<T> copy) {
        if (copy == null) return;
        copy.setRowcount(this.getRowcount());
        if (this.getData() != null) {
            copy.setData(new ArrayList<T>(this.getData()));
        } else {
            copy.data = null;
        }
    }

    /**
     *判断数据列表是否为空
     * @return
     */
    public boolean isEmpty() {
        return this.data == null || this.data.isEmpty();
    }

    @Override
    public String toString() {
        return "Sheet[rowcount=" + this.rowcount + ", data=" + this.data + "]";
    }

    public long getRowcount() {
        return this.rowcount;
    }

    public void setRowcount(long rowcount) {
        this.rowcount = rowcount;
    }

    public Collection<T> getData() {
        return this.data;
    }

    public List<T> list() {
        if (this.data == null) return null;
        return (this.data instanceof List) ? (List<T>) this.data : new ArrayList<T>(this.data);
    }

    public void setData(Collection<? extends T> data) {
        this.data = (Collection<T>) data;
    }
    
    public static class EmptySheet<T> extends Sheet<T>{

        public EmptySheet() {
            super();
        }

        public EmptySheet(int rowcount, Collection data) {
            super(rowcount, data);
        }

        public EmptySheet(long rowcount, Collection data) {
            super(rowcount, data);
        }

        @Override
        public void setRowcount(long rowcount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setData(Collection<? extends T> data) {
            throw new UnsupportedOperationException();
        }
    }
}
