package com.letstesla.dvdrentalspringboot;
/*
{
   "fetchId" : 18,
   "filters" : [
      {
         "value" : "a",
         "id" : "firstName"
      },
      {
         "id" : "lastName",
         "value" : "b"
      },
      {
         "id" : "address.address",
         "value" : "c"
      },
      {
         "id" : "address.city.city",
         "value" : "d"
      },
      {
         "id" : "address.postalCode",
         "value" : "e"
      },
      {
         "id" : "address.city.country.country",
         "value" : "f"
      },
      {
         "id" : "address.phone",
         "value" : "1"
      },
      {
         "value" : "1",
         "id" : "activebool",
         "filter" : "includes"
      }
   ],
   "orders" : [
      {
         "desc" : true,
         "id" : "firstName",
         "sort_type" : "alphanumeric"
      },
      {
         "sort_type" : "alphanumeric",
         "desc" : false,
         "id" : "lastName"
      },
      {
         "id" : "address.address",
         "desc" : false,
         "sort_type" : "alphanumeric"
      },
      {
         "desc" : true,
         "id" : "address.city.city",
         "sort_type" : "alphanumeric"
      },
      {
         "id" : "address.postalCode",
         "desc" : false,
         "sort_type" : "alphanumeric"
      },
      {
         "sort_type" : "alphanumeric",
         "desc" : false,
         "id" : "address.city.country.country"
      },
      {
         "desc" : false,
         "id" : "address.phone",
         "sort_type" : "alphanumeric"
      },
      {
         "sort_type" : "alphanumeric",
         "id" : "activebool",
         "desc" : false
      }
   ],
   "limit" : 10,
   "offset" : 0
}​

*/

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataTableQueryParams {
    public static class Filter {
        @JsonProperty
        public String id;
        @JsonProperty
        public String value = "";
    };

    public static class Order {
        @JsonProperty
        public String id;
        @JsonProperty
        public Boolean desc = false;
    };

    @JsonProperty
    public int fetchId;

    @JsonProperty
    public Filter[] filters = {};
    @JsonProperty
    public Order[] orders = {};
    @JsonProperty
    public int limit = 10;
    @JsonProperty
    public int offset = 0;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}