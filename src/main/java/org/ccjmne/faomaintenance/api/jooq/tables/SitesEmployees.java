/**
 * This class is generated by jOOQ
 */
package org.ccjmne.faomaintenance.api.jooq.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.1"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SitesEmployees extends org.jooq.impl.TableImpl<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord> {

	private static final long serialVersionUID = -1211771583;

	/**
	 * The reference instance of <code>public.sites_employees</code>
	 */
	public static final org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees SITES_EMPLOYEES = new org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord> getRecordType() {
		return org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord.class;
	}

	/**
	 * The column <code>public.sites_employees.siem_empl_fk</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord, java.lang.String> SIEM_EMPL_FK = createField("siem_empl_fk", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>public.sites_employees.siem_site_fk</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord, java.lang.String> SIEM_SITE_FK = createField("siem_site_fk", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>public.sites_employees.siem_updt_fk</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord, java.lang.Integer> SIEM_UPDT_FK = createField("siem_updt_fk", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * Create a <code>public.sites_employees</code> table reference
	 */
	public SitesEmployees() {
		this("sites_employees", null);
	}

	/**
	 * Create an aliased <code>public.sites_employees</code> table reference
	 */
	public SitesEmployees(java.lang.String alias) {
		this(alias, org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees.SITES_EMPLOYEES);
	}

	private SitesEmployees(java.lang.String alias, org.jooq.Table<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord> aliased) {
		this(alias, aliased, null);
	}

	private SitesEmployees(java.lang.String alias, org.jooq.Table<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, org.ccjmne.faomaintenance.api.jooq.Public.PUBLIC, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.ForeignKey<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord, ?>> getReferences() {
		return java.util.Arrays.<org.jooq.ForeignKey<org.ccjmne.faomaintenance.api.jooq.tables.records.SitesEmployeesRecord, ?>>asList(org.ccjmne.faomaintenance.api.jooq.Keys.SITES_EMPLOYEES__SIEM_EMPL_FK, org.ccjmne.faomaintenance.api.jooq.Keys.SITES_EMPLOYEES__SIEM_SITE_FK, org.ccjmne.faomaintenance.api.jooq.Keys.SITES_EMPLOYEES__SIEM_UPDT_FK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees as(java.lang.String alias) {
		return new org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees(alias, this);
	}

	/**
	 * Rename this table
	 */
	public org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees rename(java.lang.String name) {
		return new org.ccjmne.faomaintenance.api.jooq.tables.SitesEmployees(name, null);
	}
}
