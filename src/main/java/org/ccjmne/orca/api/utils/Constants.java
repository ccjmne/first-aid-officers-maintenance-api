package org.ccjmne.orca.api.utils;

import static org.ccjmne.orca.jooq.classes.Tables.UPDATES;
import static org.ccjmne.orca.jooq.classes.Tables.USERS;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.ccjmne.orca.jooq.classes.tables.records.UpdatesRecord;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.RecordMapper;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

public class Constants {

	/**
	 * If you are trying to use the maximum value as some kind of flag such as
	 * "undetermined future date" to avoid a NULL, instead choose some arbitrary
	 * date far enough in the future to exceed any legitimate value but not so
	 * far as to exceed the limits of any database you are possibly going to
	 * use. Define a constant for this value in your Java code and in your
	 * database, and document thoroughly.
	 *
	 * @see <a href=
	 *      "http://stackoverflow.com/questions/41301892/insert-the-max-date-independent-from-database">
	 *      Insert the max date (independent from database)</a>
	 */
	public static final Date DATE_INFINITY = Date.valueOf(LocalDate.of(9999, Month.JANUARY, 1).with(TemporalAdjusters.lastDayOfYear()));

	// ---- API CONSTANTS
	public static final String FIELDS_ALL = "all";
	public static final String DATE_INFINITY_LITERAL = "infinity";
	private static final Integer NO_UPDATE = Integer.valueOf(-1);

	public static final String STATUS_SUCCESS = "success";
	public static final String STATUS_WARNING = "warning";
	public static final String STATUS_DANGER = "danger";
	// ----

	// ---- DATABASE CONSTANTS
	public static final String TRNG_OUTCOME_CANCELLED = "CANCELLED";
	public static final String TRNG_OUTCOME_COMPLETED = "COMPLETED";
	public static final String TRNG_OUTCOME_SCHEDULED = "SCHEDULED";
	public static final List<String> TRAINING_OUTCOMES = Arrays
			.asList(Constants.TRNG_OUTCOME_CANCELLED, Constants.TRNG_OUTCOME_COMPLETED, Constants.TRNG_OUTCOME_SCHEDULED);

	public static final String EMPL_OUTCOME_CANCELLED = "CANCELLED";
	public static final String EMPL_OUTCOME_FLUNKED = "FLUNKED";
	public static final String EMPL_OUTCOME_MISSING = "MISSING";
	public static final String EMPL_OUTCOME_PENDING = "PENDING";
	public static final String EMPL_OUTCOME_VALIDATED = "VALIDATED";

	public static final String TAGS_TYPE_STRING = "s";
	public static final String TAGS_TYPE_BOOLEAN = "b";

	public static final String USER_ROOT = "root";

	public static final String UNASSIGNED_SITE = "0";
	public static final Integer UNASSIGNED_DEPARTMENT = Integer.valueOf(0);
	public static final Integer UNASSIGNED_TRAINERPROFILE = Integer.valueOf(0);

	public static final String ROLE_USER = "user";
	public static final String ROLE_ACCESS = "access";
	public static final String ROLE_TRAINER = "trainer";
	public static final String ROLE_ADMIN = "admin";

	public static final String USERTYPE_EMPLOYEE = "employee";
	public static final String USERTYPE_SITE = "site";
	public static final String USERTYPE_DEPARTMENT = "department";

	public static final Integer ACCESS_LEVEL_TRAININGS = Integer.valueOf(4);
	public static final Integer ACCESS_LEVEL_ALL_SITES = Integer.valueOf(3);
	public static final Integer ACCESS_LEVEL_DEPARTMENT = Integer.valueOf(2);
	// ----

	// ---- SUBQUERIES AND FIELDS
	public static Field<?>[] USERS_FIELDS = new Field<?>[] { USERS.USER_ID, USERS.USER_TYPE, USERS.USER_EMPL_FK, USERS.USER_SITE_FK, USERS.USER_DEPT_FK };
	public static final Field<Integer> CURRENT_UPDATE = Constants.selectUpdate(null);

	/**
	 * Returns a select sub-query that maps the results of the provided
	 * {@code query} to the sole specified {@code field}.
	 *
	 * @param <T>
	 *            The specified field type.
	 * @param field
	 *            The field to extract.
	 * @param query
	 *            The original query to use as a data source.
	 * @return A sub-query containing the sole specified {@code field} from the
	 *         given {@code query}.
	 */
	public static <T> Select<Record1<T>> select(final Field<T> field, final SelectQuery<? extends Record> query) {
		final TableLike<? extends Record> table = query.asTable();
		return DSL.select(table.field(field)).from(table);
	}

	/* package */ static Field<Date> fieldDate(final String dateStr) {
		return dateStr != null ? DSL.date(dateStr) : DSL.currentDate();
	}

	/**
	 * Returns a sub-query selecting the <strong>primary key</strong> of the
	 * {@link UpdatesRecord} that is or was relevant at a given date, or today
	 * if no date is specified.
	 *
	 * @param dateStr
	 *            The date for which to compute the relevant
	 *            {@link UpdatesRecord}, in the <code>"YYYY-MM-DD"</code>
	 *            format.
	 * @return The relevant {@link UpdatesRecord}'s primary key or
	 *         {@value Constants#NO_UPDATE} if no such update found.
	 */
	public static Field<Integer> selectUpdate(final String dateStr) {
		return DSL.coalesce(
							DSL.select(UPDATES.UPDT_PK).from(UPDATES).where(UPDATES.UPDT_DATE.eq(DSL.select(DSL.max(UPDATES.UPDT_DATE)).from(UPDATES)
									.where(UPDATES.UPDT_DATE.le(Constants.fieldDate(dateStr)))))
									.asField(),
							NO_UPDATE);
	}
	// ----

	// ---- HELPER METHODS
	/**
	 * Delegates to {@link DSL#arrayAgg(Field)} and gives the resulting
	 * aggregation the specified {@link Field}'s name.
	 *
	 * @param field
	 *            The field to aggregate
	 */
	public static <T> Field<T[]> arrayAgg(final Field<T> field) {
		return DSL.arrayAgg(field).as(field);
	}

	public static <T> RecordMapper<Record, Map<T, Object>> getZipMapper(final String key, final String... fields) {
		return getZipMapper(true, key, fields);
	}

	public static <T> RecordMapper<Record, Map<T, Object>> getZipMapper(final Field<T> key, final Field<?>... fields) {
		return getZipMapper(true, key, fields);
	}

	public static <T> RecordMapper<Record, Map<T, Object>> getZipMapper(final boolean ignoreFalsey, final Field<T> key, final Field<?>... fields) {
		return getZipMapper(ignoreFalsey, key.getName(), Arrays.asList(fields).stream().map(Field::getName).toArray(String[]::new));
	}

	public static <T> RecordMapper<Record, Map<T, Object>> getZipMapper(final boolean ignoreFalsey, final String key, final String... fields) {
		return new RecordMapper<Record, Map<T, Object>>() {

			private final boolean checkTruthy(final Object o) {
				return ignoreFalsey	? (null != o) && !Boolean.FALSE.equals(o) && !Integer.valueOf(0).equals(o) && !"".equals(o)
									: null != o;
			}

			@Override
			@SuppressWarnings("unchecked")
			public Map<T, Object> map(final Record record) {
				final Map<T, Object> res = new HashMap<>();
				final T[] keys = (T[]) record.get(key);
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] == null) {
						continue;
					}

					final RecordSlicer slicer = new RecordSlicer(record, i);
					res.put(keys[i], Arrays.asList(fields).stream()
							.filter(field -> checkTruthy(slicer.getSlice(field)))
							.collect(Collectors.toMap(field -> field, field -> slicer.getSlice(field))));
				}

				return res;
			}
		};
	}

	@SafeVarargs
	public static <K, V> RecordMapper<Record, Map<K, V>> getSelectMapper(final Field<K> key, final Field<V>... fields) {
		return getSelectMapper((r, x) -> x, key, fields);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> RecordMapper<Record, Map<K, V>> getSelectMapper(final String key, final String... fields) {
		return getSelectMapper((r, x) -> (V) x, key, fields);
	}

	@SafeVarargs
	public static <K, I, V> RecordMapper<Record, Map<K, V>> getSelectMapper(
																			final BiFunction<RecordSlicer, ? super I, ? extends V> coercer,
																			final Field<K> key,
																			final Field<I>... fields) {
		return getSelectMapper(coercer, key.getName(), Arrays.asList(fields).stream().map(Field::getName).toArray(String[]::new));
	}

	public static <K, I, V> RecordMapper<Record, Map<K, V>> getSelectMapper(
																			final BiFunction<RecordSlicer, ? super I, ? extends V> coercer,
																			final String key,
																			final String... fields) {
		return record -> {
			final Map<K, V> res = new HashMap<>();
			@SuppressWarnings("unchecked")
			final K[] keys = (K[]) record.get(key);
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] == null) {
					continue;
				}

				final RecordSlicer slicer = new RecordSlicer(record, i);
				final Optional<? extends V> value = Arrays.asList(fields).stream()
						.map(field -> slicer.<I> getSlice(field))
						.map(x -> coercer.apply(slicer, x))
						.filter(Objects::nonNull)
						.findFirst();

				if (value.isPresent()) {
					res.put(keys[i], value.get());
				}
			}

			return res;
		};
	}

	public static class RecordSlicer {

		private final Record record;
		private final int idx;

		/* package */ RecordSlicer(final Record record, final int idx) {
			this.record = record;
			this.idx = idx;
		}

		public final <T> T getSlice(final Field<T> field) {
			return this.getSlice(field.getName());
		}

		@SuppressWarnings("unchecked")
		public final <T> T getSlice(final String field) {
			try {
				return ((T[]) this.record.get(field))[this.idx];
			} catch (final IndexOutOfBoundsException e) {
				throw new IllegalArgumentException(String.format("This slicer could not operate on field: %s", field));
			}
		}
	}
}
