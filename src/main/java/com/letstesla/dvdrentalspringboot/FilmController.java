package com.letstesla.dvdrentalspringboot;

import static com.letstesla.dvdrentalspringboot.db.tables.Film.*;
import static com.letstesla.dvdrentalspringboot.db.tables.FilmCategory.*;
import static com.letstesla.dvdrentalspringboot.db.tables.Category.*;
import static com.letstesla.dvdrentalspringboot.db.tables.FilmActor.*;
import static com.letstesla.dvdrentalspringboot.db.tables.Actor.*;
import static com.letstesla.dvdrentalspringboot.db.tables.Language.*;

import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.or;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.val;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.letstesla.dvdrentalspringboot.DataTableQueryParams.Filter;
import com.letstesla.dvdrentalspringboot.DataTableQueryParams.Order;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Film;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Language;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Category;
import com.letstesla.dvdrentalspringboot.db.tables.pojos.Actor;

import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SortField;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilmController {

    private final DSLContext dsl;

    Logger logger = LoggerFactory.getLogger(FilmController.class);

    public FilmController(DSLContext dsl, Configuration jooqConfiguration) {
        this.dsl = dsl;
    }

    /**
     * Extends class {@link Film} with <code>language</code>,
     * <code>categories</code>, <code>actors</code> fields
     */
    private class FilmDTRow extends Film {

        private static final long serialVersionUID = -2497377716472619660L;

        @JsonProperty
        private Language language;

        @JsonProperty
        private Category[] categories = {};

        @JsonProperty
        private Actor[] actors = {};

        public FilmDTRow(Film film, Language language) {
            super(film);
            this.language = language;
        }

        public void setCategories(List<Category> categories) {
            this.categories = categories.toArray(Category[]::new);
        }

        public void setActors(List<Actor> actors) {
            this.actors = actors.toArray(Actor[]::new);
        }

    }

    // Convert a predefined exception to an HTTP Status code
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, // HTTP 400
            reason = "'limit' or 'offset' is invalid")
    public class QueryLimitException extends RuntimeException {
        private static final long serialVersionUID = 3820023033506881886L;
    }

    private final String populateLikeString(String text) {
        return "%" + text.replace("\\", "\\\\").replace("_", "\\_").replace("%", "\\%") + "%";
    }

    @PostMapping("/api/datatable/films")
    public DataTableResponse customers(@RequestBody DataTableQueryParams reqBody) {

        int limit = reqBody.limit;
        int offset = reqBody.offset;
        if (limit <= 0 || offset < 0)
            throw new QueryLimitException();

        int recordsTotal = this.dsl.fetchCount(FILM);

        logger.debug("Params: {}", reqBody.toString());

        List<Condition> conditions = new ArrayList<Condition>();
        for (Filter filter : reqBody.filters) {
            if (filter.value == null) {
                continue;
            }

            switch (filter.id) {
                case "title":
                    conditions.add(FILM.TITLE.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "length":
                    conditions.add(
                            FILM.LENGTH.cast(SQLDataType.VARCHAR).likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "rating":
                    conditions.add(
                            FILM.RATING.cast(SQLDataType.VARCHAR).likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "rentalRate":
                    conditions.add(FILM.RENTAL_RATE.cast(SQLDataType.VARCHAR)
                            .likeIgnoreCase(populateLikeString(filter.value)));
                    break;
                case "categories.category":
                    conditions.add(FILM.FILM_ID.in(this.dsl.select(FILM_CATEGORY.FILM_ID)
                            .from(CATEGORY.join(FILM_CATEGORY)
                                    .on(CATEGORY.CATEGORY_ID.eq(FILM_CATEGORY.CATEGORY_ID.cast(SQLDataType.INTEGER))))
                            .where(CATEGORY.NAME.likeIgnoreCase(populateLikeString(filter.value))).asField()));
                    break;
                case "actors.fullName":
                    final String searchValue = populateLikeString(filter.value);
                    conditions.add(FILM.FILM_ID.in(this.dsl.select(FILM_ACTOR.FILM_ID)
                            .from(ACTOR.join(FILM_ACTOR)
                                    .on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID.cast(SQLDataType.INTEGER))))
                            .where(or(ACTOR.FIRST_NAME.likeIgnoreCase(searchValue),
                                    ACTOR.LAST_NAME.likeIgnoreCase(searchValue)))
                            .asField()));
                    break;
                case "language.name":
                    conditions.add(LANGUAGE.NAME.likeIgnoreCase(populateLikeString(filter.value)));
                    break;
            }

        }

        int recordsFiltered = this.dsl.select(countDistinct(FILM.FILM_ID)).from(FILM)
                // join language
                .leftJoin(LANGUAGE).on(LANGUAGE.LANGUAGE_ID.eq(FILM.LANGUAGE_ID.cast(SQLDataType.INTEGER)))
                // the rest...
                .where(conditions).fetchOneInto(int.class);

        List<SortField<?>> orderBy = new ArrayList<SortField<?>>();
        for (Order order : reqBody.orders) {
            switch (order.id) {
                case "title":
                    orderBy.add(order.desc ? lower(FILM.TITLE).desc() : lower(FILM.TITLE).asc());
                    break;
                case "length":
                    orderBy.add(order.desc ? FILM.LENGTH.desc() : FILM.LENGTH.asc());
                    break;
                case "rating":
                    orderBy.add(order.desc ? FILM.RATING.desc() : FILM.RATING.asc());
                    break;
                case "rentalRate":
                    orderBy.add(order.desc ? FILM.RENTAL_RATE.desc() : FILM.RENTAL_RATE.asc());
                    break;
                case "language.name":
                    orderBy.add(order.desc ? lower(LANGUAGE.NAME).desc() : lower(LANGUAGE.NAME).asc());
                    break;
            }
        }

        Map<Integer, FilmDTRow> mapIdToFilmDTRow = this.dsl.select().from(FILM)
                // join language
                .leftJoin(LANGUAGE).on(LANGUAGE.LANGUAGE_ID.eq(FILM.LANGUAGE_ID.cast(SQLDataType.INTEGER)))
                // the rest...
                .where(conditions).orderBy(orderBy).limit(limit).offset(offset)
                .fetchMap(FILM.FILM_ID, r -> new FilmDTRow(r.into(Film.class), r.into(Language.class)));

        Integer[] filmIds = mapIdToFilmDTRow.keySet().toArray(Integer[]::new);

        Map<Short, List<Category>> mapIdToCategories = this.dsl.select(CATEGORY.fields()).select(FILM_CATEGORY.FILM_ID)
                .from(CATEGORY).join(FILM_CATEGORY)
                .on(CATEGORY.CATEGORY_ID.eq(FILM_CATEGORY.CATEGORY_ID.cast(SQLDataType.INTEGER)))
                .where(FILM_CATEGORY.FILM_ID.in(Arrays.asList(filmIds))).orderBy(lower(CATEGORY.NAME).asc())
                .fetchGroups(FILM_CATEGORY.FILM_ID, r -> r.into(Category.class));

        Map<Short, List<Actor>> mapIdToActors = this.dsl.select(ACTOR.fields()).select(FILM_ACTOR.FILM_ID).from(ACTOR)
                .join(FILM_ACTOR).on(ACTOR.ACTOR_ID.eq(FILM_ACTOR.ACTOR_ID.cast(SQLDataType.INTEGER)))
                .where(FILM_ACTOR.FILM_ID.in(Arrays.asList(filmIds)))
                .orderBy(lower(ACTOR.FIRST_NAME).concat(val(" ").concat(lower(ACTOR.LAST_NAME))).asc())
                .fetchGroups(FILM_ACTOR.FILM_ID, r -> r.into(Actor.class));

        List<FilmDTRow> data = new ArrayList<>();
        for (Integer filmId : filmIds) {
            FilmDTRow row = mapIdToFilmDTRow.get(filmId);
            row.setCategories(mapIdToCategories.get(filmId.shortValue()));
            row.setActors(mapIdToActors.get(filmId.shortValue()));
            data.add(row);
        }
        return new DataTableResponse(reqBody.fetchId, recordsFiltered, recordsTotal, data.toArray());
    }

}