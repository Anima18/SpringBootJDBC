package com.example.demo.dto.base;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;

public class PagedResultDto<T> implements Serializable {
    @JsonSerialize(
            using = LongJsonSerializer.class
    )
    private long totalCount;
    private List<T> items;

    public PagedResultDto(long totalCount, List<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public long getTotalCount() {
        return this.totalCount;
    }

    public List<T> getItems() {
        return this.items;
    }

    public void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
    }

    public void setItems(final List<T> items) {
        this.items = items;
    }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PagedResultDto)) {
            return false;
        } else {
            PagedResultDto<?> other = (PagedResultDto)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getTotalCount() != other.getTotalCount()) {
                return false;
            } else {
                Object this$items = this.getItems();
                Object other$items = other.getItems();
                if (this$items == null) {
                    if (other$items != null) {
                        return false;
                    }
                } else if (!this$items.equals(other$items)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PagedResultDto;
    }

    public int hashCode() {
        int result = 1;
        long $totalCount = this.getTotalCount();
        result = result * 59 + (int)($totalCount >>> 32 ^ $totalCount);
        Object $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    public String toString() {
        long var10000 = this.getTotalCount();
        return "PagedResultDto(totalCount=" + var10000 + ", items=" + this.getItems() + ")";
    }
}
