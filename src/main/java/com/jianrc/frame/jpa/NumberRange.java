package com.jianrc.frame.jpa;

/**
 *
 * @author jianrc
 */
public class NumberRange implements Range<Number> {

    private Number min;

    private Number max;

    private boolean inside = true;

    public NumberRange() {
    }

    public NumberRange(Number min, Number max) {
        setMin(min);
        setMax(max);
    }

    public NumberRange(Number min, Number max, boolean inside) {
        setMin(min);
        setMax(max);
        setInside(inside);
    }

    public void copyTo(NumberRange copy) {
        copy.inside = this.inside;
        copy.min = this.min;
        copy.max = this.max;
    }

    @Override
    public String toString() {
        return "NumberRange[min=" + this.min + ", max=" + this.max + ", inside=" + this.inside + "]";
    }

    @Override
    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    @Override
    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public void setInside(boolean inside) {
        this.inside = inside;
    }

    @Override
    public boolean isInside() {
        return this.inside;
    }

    public void prepare() {
    }
    
    public void release() {
        min = null;
        max = null;
        inside = true;
    }

}
