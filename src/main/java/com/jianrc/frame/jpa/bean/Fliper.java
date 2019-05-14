package com.jianrc.frame.jpa.bean;

import java.io.Serializable;

/**
 *
 * @author jianrc
 */
public class Fliper implements Serializable {

    public static int DEFAULT_PAGESIZE = 20;

    private int pageSize = DEFAULT_PAGESIZE;

    private int pageNo = 1;

    private int rowcount;
    
    private String sortColumn = "";

    public Fliper() {
    }

    public Fliper(int pageSize) {
        this.pageSize = pageSize;
    }

    public Fliper(int pageSize, int pageNo) {
        this.setPageSize(pageSize);
        this.setPageNo(pageNo);
    }

    public Fliper(int pageSize, int pageNo, String sortColumn) {
        this.setPageSize(pageSize);
        this.setPageNo(pageNo);
        this.setSortColumn(sortColumn);
    }

    public Fliper(Integer pageNo, String sort, String dir){
        int _pageNo = (pageNo == null ? 1 : pageNo);
        
        this.setPageSize(DEFAULT_PAGESIZE);
        this.setPageNo(_pageNo);
        this.setSortColumn((sort == null || dir == null) ? null : (sort + ' ' + dir));
    }
    
    public Fliper(Integer limit, Integer start, String sort, String dir){
        int pageSize = (limit == null ? DEFAULT_PAGESIZE : limit);
        int pageNo = (start == null ? 0 : start) / pageSize + 1;
        
        this.setPageSize(pageSize);
        this.setPageNo(pageNo);
        this.setSortColumn((sort == null || dir == null) ? null : (sort + ' ' + dir));
    }
    
    public void copyTo(Fliper copy) {
        if (copy == null) return;
        copy.pageNo = this.pageNo;
        copy.pageSize = this.pageSize;
        copy.sortColumn = this.sortColumn;
    }

    public Fliper incrementPageNo() {
        this.setPageNo(this.getPageNo() + 1);
        return this;
    }

    public int getFirstIndex() {
        return (getPageNo() - 1) * getPageSize();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[pageNo=" + this.pageNo + ", pageSize=" + this.pageSize + ", sortColumn=" + this.sortColumn + "]";
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (pageSize > 0) {
            this.pageSize = pageSize;
        }
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        if (pageNo >= 0) {
            this.pageNo = pageNo;
        }
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumnIfEmpty(String sortColumn) {
        if (isEmptySortColumn()) {
            setSortColumn(sortColumn);
        }
    }

    public void setSortColumn(String sortColumn) {
        if (sortColumn != null) {
            this.sortColumn = sortColumn.trim();
        }
    }

    public boolean isEmptySortColumn() {
        return this.sortColumn == null || this.sortColumn.isEmpty();
    }

    public boolean isNotEmptySortColumn() {
        return this.sortColumn != null && !this.sortColumn.isEmpty();
    }
    
    public int getPageCount(){
        return this.getRowcount() / this.pageSize + (this.getRowcount()%this.pageSize>0?1:0);
    }

    public int getRowcount() {
        return rowcount;
    }

    public void setRowcount(int rowcount) {
        this.rowcount = rowcount;
    }
}
