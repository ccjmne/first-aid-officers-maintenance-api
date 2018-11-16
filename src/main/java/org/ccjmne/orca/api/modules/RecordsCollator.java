package org.ccjmne.orca.api.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.http.client.utils.URLEncodedUtils;
import org.ccjmne.orca.api.utils.Constants;
import org.ccjmne.orca.api.utils.ResourcesHelper;
import org.eclipse.jdt.annotation.NonNull;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectFinalStep;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.jooq.tools.Convert;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;

/**
 * Provides filtering, sorting and pagination methods to be applied to any
 * {@link SelectQuery}.<br />
 * Its configuration is automatically obtained from the query parameters of the
 * current {@link HttpServletRequest}.
 *
 * @author ccjmne
 */
public class RecordsCollator {

	private static final String PARAMETER_NAME_PAGE_SIZE = "page-size";
	private static final String PARAMETER_NAME_PAGE_OFFSET = "page-offset";
	private static final Pattern SORT_ENTRY = Pattern.compile("^sort\\[(?<field>[^].]+)(?<path>(?:\\.[^]]+)*)\\]=(?<direction>.*)$");
	private static final Pattern FILTER_ENTRY = Pattern
			.compile("^filter\\[(?<field>[^].]+)(?<path>(?:\\.[^]]+)*)\\]=(?:(?<comparator>lt|le|eq|ge|gt|ne):)?(?<value>.*)$");

	private final List<? extends Sort> orderBy;
	private final List<? extends Filter> filterWhere;
	private final int limit;
	private final int offset;

	/**
	 * RAII constructor that parses the context's query parameters and creates
	 * the corresponding jOOQ {@link Condition}s and {@link SortField}s.<br />
	 * <br />
	 * The order in which the sorting query parameters appear is preserved using
	 * {@link URLEncodedUtils#parse(java.net.URI, String)}.
	 */
	@Inject
	public RecordsCollator(@Context final UriInfo uriInfo) {
		final String pSize = uriInfo.getQueryParameters().getFirst(PARAMETER_NAME_PAGE_SIZE);
		this.limit = pSize == null ? 0 : Integer.parseInt(pSize);
		this.offset = this.limit * Integer.parseInt(MoreObjects.firstNonNull(uriInfo.getQueryParameters().getFirst(PARAMETER_NAME_PAGE_OFFSET), "0"));
		this.orderBy = URLEncodedUtils.parse(uriInfo.getRequestUri(), "UTF-8").stream().map(p -> String.format("%s=%s", p.getName(), p.getValue()))
				.map(SORT_ENTRY::matcher)
				.filter(Matcher::matches)
				.map(m -> new Sort(m.group("field"), m.group("path"), m.group("direction")))
				.collect(Collectors.toList());
		this.filterWhere = uriInfo.getQueryParameters().entrySet().stream().flatMap(e -> e.getValue().stream().map(v -> String.format("%s=%s", e.getKey(), v)))
				.map(FILTER_ENTRY::matcher)
				.filter(Matcher::matches)
				.map(m -> new Filter(m.group("field"), m.group("path"), m.group("comparator"), m.group("value")))
				.collect(Collectors.toList());
	}

	/**
	 * Applicable at <strong>any depth of (sub-)query</strong>.<br />
	 *
	 * @param query
	 *            The {@link SelectQuery} to which sorting should be applied
	 * @return The original query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applyFiltering(final SelectQuery<T> query) {
		return this.applyFiltering(DSL.selectFrom(query.asTable()));
	}

	/**
	 * Overload of {@link #applyFiltering(SelectQuery)}.
	 *
	 * @param select
	 *            The {@link Select} whose underlying query to collate
	 * @return The underlying query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applyFiltering(final SelectFinalStep<T> select) {
		return this.applyFilteringImpl(select.getQuery());
	}

	/**
	 * Ideally <strong>only applied onto the outermost query</strong> that
	 * actually gets returned to the client.<br />
	 * Sorting the results of a sub-query used internally serves no purpose.
	 *
	 * @param query
	 *            The {@link SelectQuery} to which sorting should be applied
	 * @return The original query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applySorting(final SelectQuery<T> query) {
		return this.applySortingImpl(DSL.selectFrom(query.asTable()).getQuery());
	}

	/**
	 * Overload of {@link #applySorting(SelectQuery)}.
	 *
	 * @param select
	 *            The {@link Select} whose underlying query to collate
	 * @return The underlying query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applySorting(final SelectFinalStep<T> select) {
		return this.applySorting(select.getQuery());
	}

	/**
	 * Should <strong>only ever be applied onto the outermost query</strong>
	 * that actually gets returned to the client.<br />
	 * Should <strong>never be used on a sub-query </strong>used internally to
	 * refine the results.
	 *
	 * @param query
	 *            The {@link SelectQuery} to which pagination should be applied
	 * @return The original query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applyPagination(final SelectQuery<T> query) {
		if (this.limit > 0) {
			query.addLimit(this.offset, this.limit);
		}

		return query;
	}

	/**
	 * Overload of {@link #applyPagination(SelectQuery)}.
	 *
	 * @param select
	 *            The {@link Select} whose underlying query to collate
	 * @return The underlying query, for method chaining purposes
	 */
	public <T extends Record> SelectQuery<T> applyPagination(final SelectFinalStep<T> select) {
		return this.applyPagination(select.getQuery());
	}

	/**
	 * Stands for <em>"apply filtering and sorting"</em>.<br />
	 * <br />
	 * Creates a <strong>new</strong> query like:
	 *
	 * <pre>
	 * SELECT * FROM (
	 *     [{@code query}]
	 * ) AS aliased
	 * WHERE aliased.[filters...]
	 * ORDER BY aliased.[sort...]
	 * </pre>
	 *
	 * Doing so is necessary, in order to be able to filter and sort on dynamic
	 * columns (typically {@code jsonb} aggregations). Referencing the
	 * {@code SELECT}ed fields from the original {@code query} argument should
	 * be done through either {@link SelectQuery#fields(Field...)} or
	 * {@link Constants#unqualify(Field...)}.<br />
	 * <br />
	 *
	 * Ideally <strong>only applied onto the outermost query</strong> that
	 * actually gets returned to the client.<br />
	 * Sorting the results of a sub-query used internally serves no purpose (see
	 * {@link #applySorting(SelectQuery)}.
	 *
	 * @param query
	 *            The {@link SelectQuery} to which filtering and sorting should
	 *            be applied
	 * @return A new, filtered and sorted {@code SelectQuery}
	 */
	public <T extends Record> SelectQuery<T> applyFAndS(final SelectQuery<T> query) {
		return this.applySortingImpl(this.applyFilteringImpl(DSL.selectFrom(query.asTable()).getQuery()));
	}

	/**
	 * Overload of {@link #applyFAndS(SelectQuery)}.
	 *
	 * @param select
	 *            The {@link Select} whose underlying query to collate
	 * @return A new, filtered and sorted {@code SelectQuery}
	 */
	public <T extends Record> SelectQuery<T> applyFAndS(final SelectFinalStep<T> select) {
		return this.applyFAndS(select.getQuery());
	}

	/**
	 * Should <strong>only ever be applied onto the outermost query</strong>
	 * that actually gets returned to the client.<br />
	 * Should <strong>never be used on a sub-query </strong>used internally to
	 * refine the results (see {@link #applyPagination(SelectQuery)}). <br />
	 * <br />
	 * Creates a <strong>new</strong> query (doing so is necessary, see
	 * {@link #applyFAndS(SelectQuery)}), like:
	 *
	 * <pre>
	 * SELECT * FROM (
	 *     [{@code query}]
	 * ) AS aliased
	 * WHERE aliased.[filters...]
	 * ORDER BY aliased.[sort...]
	 * LIMIT [page-size] OFFSET [page-offset]
	 * </pre>
	 *
	 * @param query
	 *            The {@link SelectQuery} to which filtering, sorting and
	 *            pagination should be applied
	 * @return A new, filtered, sorted and paginated {@code SelectQuery}
	 */
	public <T extends Record> SelectQuery<T> applyAll(final SelectQuery<T> query) {
		return this.applyPagination(this.applyFAndS(query));
	}

	/**
	 * Overload of {@link #applyAll(SelectQuery)}.
	 *
	 * @param select
	 *            The {@link Select} whose underlying query to collate
	 * @return A new, filtered, sorted and paginated {@code SelectQuery}
	 */
	public <T extends Record> SelectQuery<T> applyAll(final SelectFinalStep<T> select) {
		return this.applyAll(select.getQuery());
	}

	/**
	 * For internal use. Attempts to {@code FILTER} directly on the
	 * <strong>supplied</strong> query.<br />
	 * <br />
	 * Combines all filter {@link Condition}s for each {@link Field} with
	 * {@link DSL#or(Condition...)}, then applies all of these in an
	 * {@link DSL#and(Condition...)} fashion.
	 */
	// TODO: Support AND-type joining
	private <T extends Record> SelectQuery<T> applyFilteringImpl(final SelectQuery<T> query) {
		query.addConditions(this.filterWhere.stream()
				.map(Filter.toCondition(Arrays.asList(query.fields())))
				.filter(Optional::isPresent).map(Optional::get)
				.collect(FieldCondition.JOIN_SAME_FIELDS_WITH_OR).values());
		return query;
	}

	/**
	 * For internal use. Attempts to {@code ORDER BY} directly on the
	 * <strong>supplied</strong> query.<br />
	 * <br />
	 * Uses the order in which the sort parameters appear in the request URI as
	 * sorting priority.
	 */
	private <T extends Record> SelectQuery<T> applySortingImpl(final SelectQuery<T> query) {
		query.addOrderBy(this.orderBy.stream()
				.map(Sort.toSortField(Arrays.asList(query.fields())))
				.filter(Optional::isPresent).map(Optional::get)
				.collect(Collectors.toList()));
		return query;
	}

	private static class Filter {

		private final String field;
		private final String value;
		private final String comparator;
		private final String[] path;

		protected Filter(final String field, final String path, final String comparator, final String value) {
			this.field = field;
			this.path = path.isEmpty() ? new String[0] : path.substring(1).split("\\.");
			this.comparator = MoreObjects.firstNonNull(comparator, "");
			this.value = value;
		}

		/**
		 * Computes a mapping {@link Function} that transforms {@link Filter}
		 * instances into <code>{@link Optional}<{@link FieldCondition}></code>s,
		 * using a list of typed {@link Field}s references to generate
		 * type-specific {@link Condition}s.<br />
		 * <br />
		 * For example:
		 * <ul>
		 * <li>the {@link Condition} on a
		 * <code>{@link Field}<{@link String}></code> is set up to perform a
		 * <em>case-insensitive</em> sort, which wouldn't be possible with a
		 * {@link Condition} on a <code>{@link Field}<{@link Integer}></code>
		 * </li>
		 * <li>the {@link Condition} on a
		 * <code>{@link Field}<{@link Integer}></code> is set up to accept
		 * values like <code>/(lt|le|eq|ge|gt)(\d*\.)?\d+/</code> and actually
		 * perform a <em>mathematical comparison</em>, which wouldn't be
		 * possible with a {@link Condition} on a
		 * <code>{@link Field}<{@link Integer}></code></li>
		 * </ul>
		 * <br />
		 * This method is meant to be used with{@link Stream#map(Function)}, in
		 * a stream chain as follows:
		 *
		 * <pre>
		 * stream
		 * 		.map({@link Filter#toCondition(List)})
		 * 		.filter(Optional::isPresent)
		 * 		.map(Optional::get);
		 * </pre>
		 *
		 * @param availableFields
		 *            The {@link Field}s that are available to the current
		 *            {@link SelectQuery}
		 * @return A {@link Function} to be used with
		 *         {@link Stream#map(Function)}
		 */
		public static Function<? super Filter, Optional<? extends FieldCondition<?>>> toCondition(final List<Field<?>> availableFields) {
			return self -> {
				final Optional<Field<?>> found = availableFields.stream().filter(f -> f.getName().equals(self.field)).findFirst();
				if (!found.isPresent()) {
					return Optional.empty();
				}

				if (Boolean.class.equals(found.get().getType())) {
					return Optional.of(FieldCondition.on(DSL.field(self.field, Boolean.class)).apply(f -> f.eq(Convert.convert(self.value, Boolean.class))));
				}

				if (Number.class.isAssignableFrom(found.get().getType())) {
					return Optional.of(FieldCondition.on(DSL.field(self.field, Double.class))
							.apply(f -> Filter.comparisonCondition(f, self.comparator).apply(DSL.field(self.value, Double.class))));
				}

				if (JsonNode.class.equals(found.get().getType())) {
					return Optional.of(FieldCondition.on(DSL.field("{0} #>> {1}", Object.class, DSL.field(self.field, JsonNode.class), DSL.array(self.path)))
							.apply(Constants.FILTER_VALUE_NULL.equals(self.value)	? f -> self.comparator.equals("eq") ? f.isNull() : f.isNotNull()
																					: f -> Filter.comparisonCondition(f, self.comparator)
																							.apply(DSL.val(self.value, Object.class))));
				}

				return Optional.of(FieldCondition.on(ResourcesHelper.unaccent(DSL.field(self.field, String.class)))
						.apply(f -> f.containsIgnoreCase(ResourcesHelper.unaccent(DSL.val(self.value)))));
			};
		}

		private static <T> Function<? super Field<T>, ? extends Condition> comparisonCondition(final Field<T> field, final String comparator) {
			switch (comparator) {
				case "ne":
					return field::ne;
				case "lt":
					return field::lt;
				case "le":
					return field::le;
				case "ge":
					return field::ge;
				case "gt":
					return field::gt;
				case "eq":
				default:
					return field::eq;
			}
		}
	}

	private static class Sort {

		private final String field;
		private final String[] path;
		private final Function<? super Field<?>, ? extends SortField<?>> asSortField;

		protected Sort(final String field, final String path, final String direction) {
			this.field = field;
			this.path = path.isEmpty() ? new String[0] : path.substring(1).split("\\.");
			this.asSortField = f -> (Constants.SORT_DIRECTION_DESC.equalsIgnoreCase(direction) ? f.desc() : f.asc()).nullsLast();
		}

		/**
		 * Computes a mapping {@link Function} that transforms {@link Sort}
		 * instances into <code>{@link Optional}<{@link SortField}></code>s,
		 * using a list of typed {@link Field}s references to generate
		 * type-specific {@link SortField}s.<br />
		 * <br />
		 * For example, the <code>{@link SortField}<{@link String}></code> is
		 * set up to perform a <em>case-insensitive</em> sort, which wouldn't be
		 * possible with a <code>{@link SortField}<{@link Integer}></code>.
		 * <br />
		 * <br />
		 * This method is meant to be used with{@link Stream#map(Function)}, in
		 * a stream chain as follows:
		 *
		 * <pre>
		 * stream
		 * 		.map({@link Sort#toSortField(List)})
		 * 		.filter(Optional::isPresent)
		 * 		.map(Optional::get);
		 * </pre>
		 *
		 * @param availableFields
		 *            The {@link Field}s that are available to the current
		 *            {@link SelectQuery}
		 * @return A {@link Function} to be used with
		 *         {@link Stream#map(Function)}
		 */
		@SuppressWarnings("null")
		public static Function<? super Sort, ? extends Optional<SortField<?>>> toSortField(final List<Field<?>> availableFields) {
			return self -> {
				final Optional<Field<?>> found = availableFields.stream().filter(f -> f.getName().equals(self.field)).findFirst();
				if (!found.isPresent()) {
					return Optional.empty();
				}

				if (String.class.equals(found.get().getType())) {
					return Optional.of(self.asSortField.apply(ResourcesHelper.unaccent(DSL.field(self.field, String.class))));
				}

				if (JsonNode.class.equals(found.get().getType())) {
					return Optional
							.of(self.asSortField.apply(DSL.field("{0} #> {1}", JsonNode.class, DSL.field(self.field, JsonNode.class), DSL.array(self.path))));
				}

				return Optional.of(self.asSortField.apply(DSL.field(self.field)));
			};
		}
	}

	// TODO: Document a little bit more, maybe
	private static class FieldCondition<T> {

		protected static final Collector<FieldCondition<?>, ?, Map<Field<?>, Condition>> JOIN_SAME_FIELDS_WITH_OR = Collectors
				.groupingBy(FieldCondition::getField,
							Collector.<FieldCondition<?>, FieldCondition<?>, Condition> of(	FieldCondition::new, FieldCondition::consume,
																							FieldCondition::operator,
																							FieldCondition::joinOr));

		private final Optional<Field<T>> field;
		private final List<Condition> conditions;

		/**
		 * Instantiate a new {@code FieldCondition} as follows:
		 *
		 * <pre>
		 * final {@code Field<Integer>} field = DSL.field("my_int", Integer.class);
		 * final {@code FieldCondition<Integer>} fieldCondition = FieldCondition
		 *     .on(field)
		 *     .apply(f -> f.isNotNull());
		 * </pre>
		 */
		protected static <T> Function<Function<? super Field<T>, ? extends Condition>, FieldCondition<T>> on(@NonNull final Field<T> field) {
			return func -> new FieldCondition<>(field, func.apply(field));
		}

		private FieldCondition(final Field<T> field, final Condition condition) {
			this.field = Optional.ofNullable(field);
			this.conditions = Collections.singletonList(condition);
		}

		/**
		 * Used as an accumulator to collect multiple {@code FieldConditions} and join
		 * them with {@link DSL#or(Condition...)}
		 */
		private FieldCondition() {
			this.field = Optional.empty();
			this.conditions = new ArrayList<>();
		}

		/**
		 * A {@link BinaryOperator}{@code <FieldCondition>}}.
		 * Combines two {@code FieldCondition}s within {@code this}.
		 *
		 * @param other
		 *            The other {@code FieldCondition} to combine with {@code this}
		 * @return {@code this} {@code FieldCondition}, mutated appropriately.
		 */
		private FieldCondition<?> operator(final FieldCondition<?> other) {
			this.conditions.addAll(other.conditions);
			return this;
		}

		/**
		 * A {@link BiConsumer}{@code<FieldCondition, FieldCondition>}.
		 * Add the {@code other} {@code FieldCondition} to {@code acc}.
		 *
		 * @param acc
		 *            The accumulating {@code FieldCondition}.
		 * @param other
		 *            The other {@code FieldCondition} to combine with {@code acc}
		 */
		private static void consume(final FieldCondition<?> acc, final FieldCondition<?> other) {
			acc.conditions.addAll(other.conditions);
		}

		private Field<T> getField() {
			return this.field.get();
		}

		/**
		 * @return A new {@link Condition} combining the accumulated ones with
		 *         {@link DSL#or(Condition...)}.
		 */
		private Condition joinOr() {
			return DSL.or(this.conditions);
		}

		/**
		 * @return A new {@link Condition} combining the accumulated ones with
		 *         {@link DSL#and(Condition...)}.
		 */
		private Condition joinAnd() {
			return DSL.and(this.conditions);
		}
	}
}
