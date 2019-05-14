package com.jianrc.frame.jpa;

/**
 *
 * @author jianrc
 */
public interface Range<E> extends java.io.Serializable {

    public E getMin();

    public E getMax();

    public boolean isInside();
}
