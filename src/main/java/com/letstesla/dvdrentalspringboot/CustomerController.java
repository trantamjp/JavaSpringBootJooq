package com.letstesla.dvdrentalspringboot;

import static com.letstesla.dvdrentalspringboot.db.tables.Customer.*;
import static com.letstesla.dvdrentalspringboot.db.tables.Address.*;
import static com.letstesla.dvdrentalspringboot.db.tables.City.*;
import static com.letstesla.dvdrentalspringboot.db.tables.Country.*;

import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.or;
import static org.jooq.impl.DSL.lower;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.letstesla.dvdrentalspringboot.DataTableQueryParams.Filter;
import com.letstesla.dvdrentalspringboot.DataTableQueryParams.Order;
import com.letstesla.dvdrentalspringboot.db.tables.daos.CountryDao;
import com.letstesla.dvdrentalspringboot.db.tables.daos.CustomerDao;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Address;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.City;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Country;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Customer;

import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {
    private final CustomerDao customerDao;
    private final CountryDao countryDao;

    private final DSLContext dsl;

    Logger logger = LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(DSLContext dsl, Configuration jooqConfiguration) {
        this.customerDao = new CustomerDao(jooqConfiguration);
        this.countryDao = new CountryDao(jooqConfiguration);
        this.dsl = dsl;
    }

    @GetMapping("/api/countries")
    public List<Country> countries() {
        return this.countryDao.findAll();
    }

    /**
     * Extends class {@link City} with <code>country</code> field to {@link Country}
     */
    private class CityDTRow extends City {

        private static final long serialVersionUID = 1L;
        private Country country;

        private CityDTRow(City city, Country country) {
            super(city);
            this.country = country;
        }

        @SuppressWarnings({ "unused" })
        public Country getCountry() {
            return this.country;
        }

    }

    /**
     * Extends class {@link Address} with <code>city</code> field to
     * {@link CityDTRow}
     */
    private class AddressDTRow extends Address {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private AddressDTRow(Address address, CityDTRow city) {
            super(address);
            this.city = city;
        }

        private CityDTRow city;

        @SuppressWarnings({ "unused" })
        public CityDTRow getCity() {
            return this.city;
        }

    }

    /**
     * Extends class {@link Customer} with <code>address</code> field to
     * {@link AddressDTRow}
     */
    private class CustomerDTRow extends Customer {

        private static final long serialVersionUID = -6155356548280523192L;

        private CustomerDTRow(Customer customer, AddressDTRow address) {
            super(customer);
            this.address = address;
        }

        private AddressDTRow address;

        @SuppressWarnings({ "unused" })
        public AddressDTRow getAddress() {
            return this.address;
        }

    }

    // Convert a predefined exception to an HTTP Status code
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "'limit' or 'offset' is invalid") // 400
    public class QueryLimitException extends RuntimeException {
        private static final long serialVersionUID = 3820023033506881886L;
    }

    private final String populateLikeString(String text) {
        return "%" + text.replace("\\", "\\\\").replace("_", "\\_").replace("%", "\\%") + "%";
    }

    @PostMapping("/api/datatable/customers")
    public DataTableResponse customers(@RequestBody DataTableQueryParams reqBody) {

        int limit = reqBody.limit;
        int offset = reqBody.offset;
        if (limit <= 0 || offset < 0)
            throw new QueryLimitException();

        int recordsTotal = this.dsl.fetchCount(CUSTOMER);

        logger.debug("Params: {}", reqBody.toString());

        List<Condition> conditions = new ArrayList<Condition>();
        for (Filter filter : reqBody.filters) {
            if (filter.value == null) {
                continue;
            }

            switch (filter.id) {
                case "firstName":
                    conditions.add(CUSTOMER.FIRST_NAME.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "lastName":
                    conditions.add(CUSTOMER.LAST_NAME.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "address.address":
                    final String searchValue = populateLikeString(filter.value);
                    conditions.add(or(ADDRESS.ADDRESS_.likeIgnoreCase(searchValue),
                            ADDRESS.ADDRESS2.likeIgnoreCase(searchValue)));
                    break;
                case "address.city.city":
                    conditions.add(CITY.CITY_.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "address.postalCode":
                    conditions.add(ADDRESS.POSTAL_CODE.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "address.city.country.country":
                    conditions.add(COUNTRY.COUNTRY_.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "address.phone":
                    conditions.add(ADDRESS.PHONE.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "activebool":
                    conditions.add(
                            filter.value.equals("1") ? CUSTOMER.ACTIVEBOOL.isTrue() : CUSTOMER.ACTIVEBOOL.isFalse());
                    break;
            }

        }

        int recordsFiltered = this.dsl.select(countDistinct(CUSTOMER.CUSTOMER_ID)).from(CUSTOMER).leftJoin(ADDRESS)
                .on(ADDRESS.ADDRESS_ID.eq(CUSTOMER.ADDRESS_ID.cast(SQLDataType.INTEGER))).leftJoin(CITY)
                .on(CITY.CITY_ID.eq(ADDRESS.CITY_ID.cast(SQLDataType.INTEGER))).leftJoin(COUNTRY)
                .on(COUNTRY.COUNTRY_ID.eq(CITY.COUNTRY_ID.cast(SQLDataType.INTEGER))).where(conditions)
                .fetchOneInto(int.class);

        List<SortField<String>> orderBy = new ArrayList<SortField<String>>();
        for (Order order : reqBody.orders) {
            switch (order.id) {
                case "firstName":
                    orderBy.add(order.desc ? lower(CUSTOMER.FIRST_NAME).desc() : lower(CUSTOMER.FIRST_NAME).asc());
                    break;
                case "lastName":
                    orderBy.add(order.desc ? lower(CUSTOMER.LAST_NAME).desc() : lower(CUSTOMER.LAST_NAME).asc());
                    break;
                case "address.address":
                    orderBy.add(order.desc ? lower(ADDRESS.ADDRESS_).desc() : lower(ADDRESS.ADDRESS_).asc());
                    orderBy.add(order.desc ? lower(ADDRESS.ADDRESS2).desc() : lower(ADDRESS.ADDRESS2).asc());
                    break;
                case "address.city.city":
                    orderBy.add(order.desc ? lower(CITY.CITY_).desc() : lower(CITY.CITY_).asc());
                    break;
                case "address.postalCode":
                    orderBy.add(order.desc ? lower(ADDRESS.POSTAL_CODE).desc() : lower(ADDRESS.POSTAL_CODE).asc());
                    break;
                case "address.city.country.country":
                    orderBy.add(order.desc ? lower(COUNTRY.COUNTRY_).desc() : lower(COUNTRY.COUNTRY_).asc());
                    break;
                case "address.phone":
                    orderBy.add(order.desc ? lower(ADDRESS.PHONE).desc() : lower(ADDRESS.PHONE).asc());
                    break;
                case "activebool":
                    orderBy.add(order.desc ? CUSTOMER.ACTIVEBOOL.cast(SQLDataType.VARCHAR(20)).desc()
                            : CUSTOMER.ACTIVEBOOL.cast(SQLDataType.VARCHAR(20)).asc());
                    break;
            }
        }

        List<CustomerDTRow> data = this.dsl.select().from(CUSTOMER).leftJoin(ADDRESS)
                .on(ADDRESS.ADDRESS_ID.eq(CUSTOMER.ADDRESS_ID.cast(SQLDataType.INTEGER))).leftJoin(CITY)
                .on(CITY.CITY_ID.eq(ADDRESS.CITY_ID.cast(SQLDataType.INTEGER))).leftJoin(COUNTRY)
                .on(COUNTRY.COUNTRY_ID.eq(CITY.COUNTRY_ID.cast(SQLDataType.INTEGER))).where(conditions).orderBy(orderBy)
                .limit(limit).offset(offset).fetch().stream()
                .map(r -> new CustomerDTRow(r.into(Customer.class),
                        new AddressDTRow(r.into(Address.class),
                                new CityDTRow(r.into(City.class), r.into(Country.class)))))
                .collect(Collectors.toList());

        return new DataTableResponse(reqBody.fetchId, recordsFiltered, recordsTotal, data.toArray());
    }

}