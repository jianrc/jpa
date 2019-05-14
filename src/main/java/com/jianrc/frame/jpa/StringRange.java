package com.jianrc.frame.jpa;

/**
 *
 * @author jianrc
 */
public class StringRange implements Range<String> {

    private String min;

    private String max;

    private boolean inside = true;

    public StringRange() {
    }

    public StringRange(String min, String max) {
        setMin(min);
        setMax(max);
    }

    public StringRange(String min, String max, boolean inside) {
        setMin(min);
        setMax(max);
        setInside(inside);
    }

    public void copyTo(StringRange copy) {
        copy.inside = this.inside;
        copy.min = this.min;
        copy.max = this.max;
    }

    @Override
    public String toString() {
        return "StringRange[min=" + this.min + ", max=" + this.max + ", inside=" + this.inside + "]";
    }

    @Override
    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    @Override
    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    @Override
    public boolean isInside() {
        return inside;
    }

    public void setInside(boolean inside) {
        this.inside = inside;
    }
}
