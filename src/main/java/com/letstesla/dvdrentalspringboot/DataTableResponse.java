package com.letstesla.dvdrentalspringboot;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataTableResponse {
    public DataTableResponse(int fetchId, int recordsFiltered, int recordsTotal, Object[] data) {
        this.fetchId = fetchId;
        this.recordsFiltered = recordsFiltered;
        this.recordsTotal = recordsTotal;
        this.data = data;
    }

    @JsonProperty
    public int fetchId = 0;
    @JsonProperty
    public int recordsFiltered = 0;
    @JsonProperty
    public int recordsTotal = 0;
    @JsonProperty
    public Object[] data = {};
}