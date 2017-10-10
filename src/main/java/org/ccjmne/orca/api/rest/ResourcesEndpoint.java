package org.ccjmne.orca.api.rest;

import static org.ccjmne.orca.jooq.classes.Tables.DEPARTMENTS;
import static org.ccjmne.orca.jooq.classes.Tables.EMPLOYEES;
import static org.ccjmne.orca.jooq.classes.Tables.EMPLOYEES_CERTIFICATES_OPTOUT;
import static org.ccjmne.orca.jooq.classes.Tables.SITES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES_EMPLOYEES;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGS;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGS_EMPLOYEES;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGTYPES;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGTYPES_CERTIFICATES;
import static org.ccjmne.orca.jooq.classes.Tables.UPDATES;

import java.sql.Date;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.ccjmne.orca.api.modules.ResourcesUnrestricted;
import org.ccjmne.orca.api.modules.Restrictions;
import org.ccjmne.orca.api.utils.Constants;
import org.ccjmne.orca.api.utils.SafeDateFormat;
import org.ccjmne.orca.jooq.classes.tables.records.EmployeesCertificatesOptoutRecord;
import org.ccjmne.orca.jooq.classes.tables.records.TrainingsEmployeesRecord;
import org.ccjmne.orca.jooq.classes.tables.records.UpdatesRecord;
import org.jooq.DSLContext;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Path("resources")
public class ResourcesEndpoint {

	private final DSLContext ctx;
	private final Restrictions restrictions;

	@Inject
	public ResourcesEndpoint(final DSLContext ctx, final ResourcesUnrestricted unrestrictedResources, final Restrictions restrictions) {
		this.ctx = ctx;
		this.restrictions = restrictions;
	}

	/**
	 * Unassigned employees should only ever be accessed through their training
	 * sessions, since they only need to keep existing there for history
	 * purposes.<br />
	 * Thus, employees that aren't assigned to any site can be accessed if and
	 * only if:
	 * <ul>
	 * <li><code>dept_pk</code> is <code>null</code></li>
	 * <li><code>site_pk</code> is <code>null</code></li>
	 * <li><code>trng_pk</code> is <strong>defined</strong></li>
	 * </ul>
	 * Or:
	 * <ul>
	 * <li><code>empl_pk</code> is <strong>defined</strong></li>
	 * <li>{@link Restrictions#canAccessTrainings()} is <code>true</code></li>
	 * </ul>
	 */
	private boolean accessUnassignedEmployees(final String empl_pk, final String site_pk, final Integer dept_pk, final Integer trng_pk) {
		if ((empl_pk != null) && this.restrictions.canAccessTrainings()) {
			return true;
		}

		return (dept_pk == null) && (site_pk == null) && (trng_pk != null);
	}

	public SelectQuery<Record> selectEmployees(final String empl_pk, final String site_pk, final Integer dept_pk, final Integer trng_pk, final String dateStr) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(EMPLOYEES);
		query.addConditions(EMPLOYEES.EMPL_PK.ne(Constants.USER_ROOT));
		query.addJoin(
						SITES_EMPLOYEES,
						accessUnassignedEmployees(empl_pk, site_pk, dept_pk, trng_pk) ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
						SITES_EMPLOYEES.SIEM_EMPL_FK.eq(EMPLOYEES.EMPL_PK),
						SITES_EMPLOYEES.SIEM_UPDT_FK.eq(Constants.selectUpdate(dateStr)),
						SITES_EMPLOYEES.SIEM_SITE_FK.in(Constants.select(SITES.SITE_PK, selectSites(site_pk, dept_pk))));

		if (trng_pk != null) {
			if (!this.restrictions.canAccessTrainings()) {
				throw new ForbiddenException();
			}

			query.addJoin(TRAININGS_EMPLOYEES, TRAININGS_EMPLOYEES.TREM_EMPL_FK.eq(EMPLOYEES.EMPL_PK), TRAININGS_EMPLOYEES.TREM_TRNG_FK.eq(trng_pk));
		}

		if (empl_pk != null) {
			query.addConditions(EMPLOYEES.EMPL_PK.eq(empl_pk));
		}

		return query;
	}

	@GET
	@Path("employees")
	public Result<? extends Record> listEmployees(
													@QueryParam("employee") final String empl_pk,
													@QueryParam("site") final String site_pk,
													@QueryParam("department") final Integer dept_pk,
													@QueryParam("training") final Integer trng_pk,
													@QueryParam("date") final String dateStr,
													@QueryParam("fields") final String fields) {
		try (final SelectQuery<? extends Record> query = selectEmployees(empl_pk, site_pk, dept_pk, trng_pk, dateStr)) {
			if (Constants.FIELDS_ALL.equals(fields)) {
				query.addSelect(EMPLOYEES.fields());
				query.addSelect(SITES_EMPLOYEES.fields());
			} else {
				query.addSelect(
								EMPLOYEES.EMPL_PK,
								EMPLOYEES.EMPL_FIRSTNAME,
								EMPLOYEES.EMPL_SURNAME,
								EMPLOYEES.EMPL_GENDER,
								EMPLOYEES.EMPL_PERMANENT,
								SITES_EMPLOYEES.SIEM_SITE_FK);
			}

			if (trng_pk != null) {
				query.addSelect(TRAININGS_EMPLOYEES.fields());
			}

			return this.ctx.fetch(query);
		}
	}

	/**
	 * Used in order to load all training sessions outcomes for the employees'
	 * advanced search module.
	 */
	// TODO: Restrict this method (and accordingly: the corresponding options in
	// the advanced search module) to users who can access training sessions?
	@GET
	@Path("employees/trainings")
	public Map<String, Result<Record>> listEmployeesTrainings(
																@QueryParam("employee") final String empl_pk,
																@QueryParam("site") final String site_pk,
																@QueryParam("department") final Integer dept_pk,
																@QueryParam("training") final Integer trng_pk,
																@QueryParam("date") final String dateStr) {
		return this.ctx
				.select(TRAININGS_EMPLOYEES.TREM_EMPL_FK, TRAININGS_EMPLOYEES.TREM_OUTCOME, TRAININGS.TRNG_DATE)
				.select(DSL.arrayAgg(TRAININGTYPES_CERTIFICATES.TTCE_CERT_FK).as("certificates"))
				.from(TRAININGS_EMPLOYEES)
				.join(TRAININGS).on(TRAININGS.TRNG_PK.eq(TRAININGS_EMPLOYEES.TREM_TRNG_FK))
				.join(TRAININGTYPES_CERTIFICATES).on(TRAININGTYPES_CERTIFICATES.TTCE_TRTY_FK.eq(TRAININGS.TRNG_TRTY_FK))
				.where(TRAININGS_EMPLOYEES.TREM_EMPL_FK.in(Constants.select(EMPLOYEES.EMPL_PK, selectEmployees(empl_pk, site_pk, dept_pk, trng_pk, dateStr))))
				.groupBy(TRAININGS_EMPLOYEES.TREM_EMPL_FK, TRAININGS_EMPLOYEES.TREM_OUTCOME, TRAININGS.TRNG_DATE)
				.fetchGroups(TRAININGS_EMPLOYEES.TREM_EMPL_FK);
	}

	@GET
	@Path("employees/{empl_pk}")
	public Record lookupEmployee(@PathParam("empl_pk") final String empl_pk, @QueryParam("date") final String dateStr) {
		try {
			return listEmployees(empl_pk, null, null, null, dateStr, Constants.FIELDS_ALL).get(0);
		} catch (final IndexOutOfBoundsException e) {
			throw new NotFoundException();
		}
	}

	public SelectQuery<Record> selectSites(final String site_pk, final Integer dept_pk) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(SITES);
		query.addConditions(SITES.SITE_PK.ne(Constants.UNASSIGNED_SITE));
		if ((site_pk == null) && !this.restrictions.canAccessAllSites()) {
			if (this.restrictions.getAccessibleSites().isEmpty()) {
				throw new ForbiddenException();
			}

			query.addConditions(SITES.SITE_PK.in(this.restrictions.getAccessibleSites()));
		}

		if (dept_pk != null) {
			if (!this.restrictions.canAccessDepartment(dept_pk)) {
				throw new ForbiddenException();
			}

			query.addConditions(SITES.SITE_DEPT_FK.eq(dept_pk));
		}

		if (site_pk != null) {
			if (!this.restrictions.canAccessSite(site_pk)) {
				throw new ForbiddenException();
			}

			query.addConditions(SITES.SITE_PK.eq(site_pk));
		}

		return query;
	}

	@GET
	@Path("sites")
	public Result<Record> listSites(
									@QueryParam("site") final String site_pk,
									@QueryParam("department") final Integer dept_pk,
									@QueryParam("date") final String dateStr,
									@QueryParam("unlisted") final boolean unlisted) {
		try (final SelectQuery<Record> query = selectSites(site_pk, dept_pk)) {
			query.addSelect(SITES.fields());
			query.addSelect(DSL.count(SITES_EMPLOYEES.SIEM_EMPL_FK).as("count"));
			query.addSelect(DSL.count(SITES_EMPLOYEES.SIEM_EMPL_FK).filterWhere(EMPLOYEES.EMPL_PERMANENT.eq(Boolean.TRUE)).as("permanent"));
			query.addJoin(
							SITES_EMPLOYEES.join(EMPLOYEES).on(EMPLOYEES.EMPL_PK.eq(SITES_EMPLOYEES.SIEM_EMPL_FK)),
							unlisted ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
							SITES_EMPLOYEES.SIEM_SITE_FK.eq(SITES.SITE_PK).and(SITES_EMPLOYEES.SIEM_UPDT_FK.eq(Constants.selectUpdate(dateStr))));
			query.addGroupBy(SITES.fields());
			return this.ctx.fetch(query);
		}
	}

	@GET
	@Path("sites/{site_pk}")
	public Record lookupSite(
								@PathParam("site_pk") final String site_pk,
								@QueryParam("date") final String dateStr,
								@QueryParam("unlisted") final boolean unlisted) {
		try {
			return listSites(site_pk, null, dateStr, unlisted).get(0);
		} catch (final IndexOutOfBoundsException e) {
			throw new NotFoundException();
		}
	}

	public SelectQuery<Record> selectDepartments(final Integer dept_pk) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(DEPARTMENTS);
		query.addConditions(DEPARTMENTS.DEPT_PK.ne(Constants.UNASSIGNED_DEPARTMENT));
		if ((dept_pk == null) && !this.restrictions.canAccessAllSites()) {
			if (this.restrictions.getAccessibleDepartment() == null) {
				throw new ForbiddenException();
			}

			query.addConditions(DEPARTMENTS.DEPT_PK.eq(this.restrictions.getAccessibleDepartment()));
		}

		if (dept_pk != null) {
			if (!this.restrictions.canAccessDepartment(dept_pk)) {
				throw new ForbiddenException();
			}

			query.addConditions(DEPARTMENTS.DEPT_PK.eq(dept_pk));
		}

		return query;
	}

	@GET
	@Path("departments")
	public Result<? extends Record> listDepartments(
													@QueryParam("department") final Integer dept_pk,
													@QueryParam("date") final String dateStr,
													@QueryParam("unlisted") final boolean unlisted) {
		final Table<Record> counts = DSL.select(SITES_EMPLOYEES.SIEM_SITE_FK)
				.select(DSL.count(SITES_EMPLOYEES.SIEM_EMPL_FK).as("count"))
				.select(DSL.count(SITES_EMPLOYEES.SIEM_EMPL_FK).filterWhere(EMPLOYEES.EMPL_PERMANENT.eq(Boolean.TRUE)).as("permanent"))
				.from(SITES_EMPLOYEES.join(EMPLOYEES).on(EMPLOYEES.EMPL_PK.eq(SITES_EMPLOYEES.SIEM_EMPL_FK)))
				.where(SITES_EMPLOYEES.SIEM_SITE_FK.in(Constants.select(SITES.SITE_PK, selectSites(null, dept_pk))))
				.and(SITES_EMPLOYEES.SIEM_UPDT_FK.eq(Constants.selectUpdate(dateStr)))
				.groupBy(SITES_EMPLOYEES.SIEM_SITE_FK).asTable();
		try (final SelectQuery<Record> query = selectDepartments(dept_pk)) {
			query.addSelect(DEPARTMENTS.fields());
			query.addSelect(DSL.sum(counts.field("count", Integer.class)).as("count"));
			query.addSelect(DSL.sum(counts.field("permanent", Integer.class)).as("permanent"));
			query.addSelect(DSL.count(SITES.SITE_PK).as("sites_count"));
			query.addJoin(
							SITES,
							unlisted ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
							SITES.SITE_DEPT_FK.eq(DEPARTMENTS.DEPT_PK));
			query.addJoin(
							counts,
							unlisted ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
							counts.field(SITES_EMPLOYEES.SIEM_SITE_FK).eq(SITES.SITE_PK));
			query.addGroupBy(DEPARTMENTS.fields());
			return this.ctx.fetch(query);
		}
	}

	@GET
	@Path("departments/{dept_pk}")
	public Record lookupDepartment(
									@PathParam("dept_pk") final Integer dept_pk,
									@QueryParam("date") final String dateStr,
									@QueryParam("unlisted") final boolean unlisted) {
		try {
			return listDepartments(dept_pk, dateStr, unlisted).get(0);
		} catch (final IndexOutOfBoundsException e) {
			throw new NotFoundException();
		}
	}

	// TODO: REWRITE EVERYTHING BELOW

	@GET
	@Path("trainings")
	public Result<Record> listTrainings(
										@QueryParam("employee") final String empl_pk,
										@QueryParam("type") final List<Integer> types,
										@QueryParam("date") final String dateStr,
										@QueryParam("from") final String fromStr,
										@QueryParam("to") final String toStr,
										@QueryParam("completed") final Boolean completedOnly)
			throws ParseException {
		if (!this.restrictions.canAccessTrainings()) {
			throw new ForbiddenException();
		}

		final Date date = dateStr == null ? null : SafeDateFormat.parseAsSql(dateStr);
		final Date from = fromStr == null ? null : SafeDateFormat.parseAsSql(fromStr);
		final Date to = toStr == null ? null : SafeDateFormat.parseAsSql(toStr);

		try (final SelectQuery<Record> query = this.ctx.selectQuery()) {
			query.addSelect(TRAININGS.fields());
			query.addFrom(TRAININGS);
			query.addGroupBy(TRAININGS.fields());
			query.addJoin(TRAININGS_EMPLOYEES, JoinType.LEFT_OUTER_JOIN, TRAININGS_EMPLOYEES.TREM_TRNG_FK.eq(TRAININGS.TRNG_PK));

			query.addSelect(Constants.TRAINING_REGISTERED);
			query.addSelect(Constants.TRAINING_VALIDATED);
			query.addSelect(Constants.TRAINING_FLUNKED);
			query.addSelect(Constants.TRAINING_TRAINERS);

			if (empl_pk != null) {
				final Table<TrainingsEmployeesRecord> employeeOutcomes = DSL.selectFrom(TRAININGS_EMPLOYEES).where(TRAININGS_EMPLOYEES.TREM_EMPL_FK.eq(empl_pk))
						.asTable();
				query.addJoin(employeeOutcomes, employeeOutcomes.field(TRAININGS_EMPLOYEES.TREM_TRNG_FK).eq(TRAININGS_EMPLOYEES.TREM_TRNG_FK));
				query.addSelect(employeeOutcomes.fields());
				query.addGroupBy(employeeOutcomes.fields());
			}

			if (!types.isEmpty()) {
				query.addJoin(TRAININGTYPES, TRAININGS.TRNG_TRTY_FK.eq(TRAININGTYPES.TRTY_PK).and(TRAININGTYPES.TRTY_PK.in(types)));
			}

			if (date != null) {
				query.addConditions(TRAININGS.TRNG_START.isNotNull()
						.and(TRAININGS.TRNG_START.le(date).and(TRAININGS.TRNG_DATE.ge(date)))
						.or(TRAININGS.TRNG_DATE.eq(date)));
			}

			if (from != null) {
				query.addConditions(TRAININGS.TRNG_DATE.ge(from).or(TRAININGS.TRNG_START.isNotNull().and(TRAININGS.TRNG_START.ge(from))));
			}

			if (to != null) {
				query.addConditions(TRAININGS.TRNG_DATE.le(to).or(TRAININGS.TRNG_START.isNotNull().and(TRAININGS.TRNG_START.le(to))));
			}

			if ((completedOnly != null) && completedOnly.booleanValue()) {
				query.addConditions(TRAININGS.TRNG_OUTCOME.eq(Constants.TRNG_OUTCOME_COMPLETED));
			}

			query.addOrderBy(TRAININGS.TRNG_DATE);
			return query.fetch();
		}
	}

	@GET
	@Path("trainings/{trng_pk}")
	public Record lookupTraining(@PathParam("trng_pk") final Integer trng_pk) {
		if (!this.restrictions.canAccessTrainings()) {
			throw new ForbiddenException();
		}

		try (final SelectQuery<Record> query = this.ctx.selectQuery()) {
			query.addSelect(TRAININGS.fields());
			query.addFrom(TRAININGS);
			query.addJoin(TRAININGS_EMPLOYEES, JoinType.LEFT_OUTER_JOIN, TRAININGS_EMPLOYEES.TREM_TRNG_FK.eq(TRAININGS.TRNG_PK));
			query.addSelect(Constants.TRAINING_REGISTERED);
			query.addSelect(Constants.TRAINING_VALIDATED);
			query.addSelect(Constants.TRAINING_FLUNKED);
			query.addSelect(Constants.TRAINING_TRAINERS);
			query.addConditions(TRAININGS.TRNG_PK.eq(trng_pk));
			query.addGroupBy(TRAININGS.fields());
			return query.fetchOne();
		}
	}

	@GET
	@Path("updates")
	// TODO: move to UpdateEndpoint?
	public Result<UpdatesRecord> listUpdates() {
		return this.ctx.selectFrom(UPDATES).orderBy(UPDATES.UPDT_DATE.desc()).fetch();
	}

	@GET
	@Path("updates/{date}")
	// TODO: move to UpdateEndpoint?
	public Record lookupUpdate(@PathParam("date") final String dateStr) {
		return this.ctx.selectFrom(UPDATES).where(UPDATES.UPDT_PK.eq(Constants.selectUpdate(dateStr))).fetchAny();
	}
}