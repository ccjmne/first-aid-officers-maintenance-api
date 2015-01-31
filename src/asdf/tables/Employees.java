/**
 * This class is generated by jOOQ
 */
package asdf.tables;

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
public class Employees extends org.jooq.impl.TableImpl<asdf.tables.records.EmployeesRecord> {

	private static final long serialVersionUID = 1734881128;

	/**
	 * The reference instance of <code>public.employees</code>
	 */
	public static final asdf.tables.Employees EMPLOYEES = new asdf.tables.Employees();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<asdf.tables.records.EmployeesRecord> getRecordType() {
		return asdf.tables.records.EmployeesRecord.class;
	}

	/**
	 * The column <code>public.employees.empl_pk</code>.
	 */
	public final org.jooq.TableField<asdf.tables.records.EmployeesRecord, java.lang.String> EMPL_PK = createField("empl_pk", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>public.employees.empl_firstname</code>.
	 */
	public final org.jooq.TableField<asdf.tables.records.EmployeesRecord, java.lang.String> EMPL_FIRSTNAME = createField("empl_firstname", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>public.employees.empl_surname</code>.
	 */
	public final org.jooq.TableField<asdf.tables.records.EmployeesRecord, java.lang.String> EMPL_SURNAME = createField("empl_surname", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false), this, "");

	/**
	 * The column <code>public.employees.empl_dob</code>.
	 */
	public final org.jooq.TableField<asdf.tables.records.EmployeesRecord, java.sql.Date> EMPL_DOB = createField("empl_dob", org.jooq.impl.SQLDataType.DATE.nullable(false), this, "");

	/**
	 * The column <code>public.employees.empl_permanent</code>.
	 */
	public final org.jooq.TableField<asdf.tables.records.EmployeesRecord, java.lang.Boolean> EMPL_PERMANENT = createField("empl_permanent", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false), this, "");

	/**
	 * Create a <code>public.employees</code> table reference
	 */
	public Employees() {
		this("employees", null);
	}

	/**
	 * Create an aliased <code>public.employees</code> table reference
	 */
	public Employees(java.lang.String alias) {
		this(alias, asdf.tables.Employees.EMPLOYEES);
	}

	private Employees(java.lang.String alias, org.jooq.Table<asdf.tables.records.EmployeesRecord> aliased) {
		this(alias, aliased, null);
	}

	private Employees(java.lang.String alias, org.jooq.Table<asdf.tables.records.EmployeesRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, asdf.Public.PUBLIC, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<asdf.tables.records.EmployeesRecord> getPrimaryKey() {
		return asdf.Keys.EMPL_PK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<asdf.tables.records.EmployeesRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<asdf.tables.records.EmployeesRecord>>asList(asdf.Keys.EMPL_PK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public asdf.tables.Employees as(java.lang.String alias) {
		return new asdf.tables.Employees(alias, this);
	}

	/**
	 * Rename this table
	 */
	public asdf.tables.Employees rename(java.lang.String name) {
		return new asdf.tables.Employees(name, null);
	}
}
