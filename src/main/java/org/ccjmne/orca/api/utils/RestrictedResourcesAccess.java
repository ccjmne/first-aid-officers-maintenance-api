package org.ccjmne.orca.api.utils;

import static org.ccjmne.orca.jooq.classes.Tables.DEPARTMENTS;
import static org.ccjmne.orca.jooq.classes.Tables.EMPLOYEES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES_EMPLOYEES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES_TAGS;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGS_EMPLOYEES;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.ccjmne.orca.api.modules.Restrictions;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

/**
 * Used to select resources whose access should be restricted.<br />
 * <strong>All access to these resources shall be done through this
 * class!</strong><br />
 *
 * @author ccjmne
 *
 */
public class RestrictedResourcesAccess {

	private final Restrictions restrictions;

	@Inject
	public RestrictedResourcesAccess(final Restrictions restrictions) {
		this.restrictions = restrictions;
	}

	/**
	 * Use
	 * {@link RestrictedResourcesAccess#accessUnassignedEmployees(String, String, Integer, Map)}
	 * instead.
	 */
	@Deprecated
	public boolean accessUnassignedEmployeesv1(final String empl_pk, final String site_pk, final Integer dept_pk, final Integer trng_pk) {
		if ((empl_pk != null) && this.restrictions.canAccessTrainings()) {
			return true;
		}

		return (dept_pk == null) && (site_pk == null) && (trng_pk != null);
	}

	/**
	 * Unassigned employees should only ever be accessed through their training
	 * sessions, since they only need to keep existing there for history
	 * purposes.<br />
	 * Thus, employees that aren't assigned to any site can be accessed if and
	 * only if:
	 * <ul>
	 * <li><code>tagFilters</code> is <code>empty</code>, and</li>
	 * <li><code>site_pk</code> is <code>null</code>, and</li>
	 * <li><code>trng_pk</code> is <strong>defined</strong></li>
	 * </ul>
	 * Or:
	 * <ul>
	 * <li><code>empl_pk</code> is <strong>defined</strong>, and</li>
	 * <li>{@link Restrictions#canAccessTrainings()} is <code>true</code></li>
	 * </ul>
	 */
	public boolean accessUnassignedEmployees(final String empl_pk, final String site_pk, final Integer trng_pk, final Map<Integer, List<String>> tagFilters) {
		if ((empl_pk != null) && this.restrictions.canAccessTrainings()) {
			return true;
		}

		return tagFilters.isEmpty() && (site_pk == null) && (trng_pk != null);
	}

	/**
	 * Use
	 * {@link RestrictedResourcesAccess#selectEmployeesByTags(String, String, Integer, String, Map)}
	 * instead.
	 */
	@Deprecated
	public SelectQuery<Record> selectEmployees(
												final String empl_pk,
												final String site_pk,
												final Integer dept_pk,
												final Integer trng_pk,
												final String dateStr) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(EMPLOYEES);
		query.addConditions(EMPLOYEES.EMPL_PK.ne(Constants.USER_ROOT));
		query.addJoin(
						SITES_EMPLOYEES,
						accessUnassignedEmployeesv1(empl_pk, site_pk, dept_pk, trng_pk) ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
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

	// TODO: rename to selectEmployees
	public SelectQuery<Record> selectEmployeesByTags(
														final String empl_pk,
														final String site_pk,
														final Integer trng_pk,
														final String dateStr,
														final Map<Integer, List<String>> tagFilters) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(EMPLOYEES);
		query.addConditions(EMPLOYEES.EMPL_PK.ne(Constants.USER_ROOT));
		query.addJoin(
						SITES_EMPLOYEES,
						accessUnassignedEmployees(empl_pk, site_pk, trng_pk, tagFilters) ? JoinType.LEFT_OUTER_JOIN : JoinType.JOIN,
						SITES_EMPLOYEES.SIEM_EMPL_FK.eq(EMPLOYEES.EMPL_PK),
						SITES_EMPLOYEES.SIEM_UPDT_FK.eq(Constants.selectUpdate(dateStr)),
						SITES_EMPLOYEES.SIEM_SITE_FK.in(Constants.select(SITES.SITE_PK, selectSitesByTags(site_pk, tagFilters))));

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

	/**
	 * Departments are on the verge of being deprecated.<br />
	 * Use {@link RestrictedResourcesAccess#selectSitesByTag} instead.
	 */
	@Deprecated
	// TODO: delete
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

	/**
	 * Selects sites for which:
	 * <ul>
	 * <li><code>site_pk</code> (if specified) uniquely identifies it,
	 * <em>and</em></li>
	 * <li><em>for each</em> filter <code>{ k: [v1, v2, ..., vN] }</code>, the
	 * site has a tag <code>{ type, value }</code> where:
	 * <ul>
	 * <li></code>type == k</code>, <em>and</em></li>
	 * <li><code>[v1, v2, ..., vN].contains(value)</code></li>
	 * </ul>
	 * </li>
	 * </ul>
	 *
	 * @param site_pk
	 *            Optional. The identifier of the only site to select
	 * @param filters
	 *            Non-null. Map of tag types and values to satisfy for the sites
	 *            to be selected
	 */
	// TODO: rename to selectSites
	public SelectQuery<Record> selectSitesByTags(final String site_pk, final Map<Integer, List<String>> filters) {
		final SelectQuery<Record> query = DSL.select().getQuery();
		query.addFrom(SITES);
		query.addConditions(SITES.SITE_PK.ne(Constants.UNASSIGNED_SITE));
		if ((site_pk == null) && !this.restrictions.canAccessAllSites()) {
			if (this.restrictions.getAccessibleSites().isEmpty()) {
				throw new ForbiddenException();
			}

			query.addConditions(SITES.SITE_PK.in(this.restrictions.getAccessibleSites()));
		}

		if (!filters.isEmpty()) {
			if (!this.restrictions.canAccessSitesWith(filters)) {
				throw new ForbiddenException();
			}

			// this Optional can safely be get() since !filters.isEmpty()
			query.addConditions(DSL.exists(filters.entrySet().stream().<Select<? extends Record>> map(e -> DSL
					.selectZero().from(SITES_TAGS)
					.where(SITES_TAGS.SITA_SITE_FK.eq(SITES.SITE_PK))
					.and(SITES_TAGS.SITA_TAGS_FK.eq(e.getKey()))
					.and(SITES_TAGS.SITA_VALUE.in(e.getValue())))
					.reduce(Select::intersect).get()));
		}

		if (site_pk != null) {
			if (!this.restrictions.canAccessSite(site_pk)) {
				throw new ForbiddenException();
			}

			query.addConditions(SITES.SITE_PK.eq(site_pk));
		}

		return query;
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
}
