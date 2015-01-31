/**
 * This class is generated by jOOQ
 */
package asdf.tables.records;

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
public class EmployeesRecord extends org.jooq.impl.UpdatableRecordImpl<asdf.tables.records.EmployeesRecord> implements org.jooq.Record5<java.lang.String, java.lang.String, java.lang.String, java.sql.Date, java.lang.Boolean> {

	private static final long serialVersionUID = -712526227;

	/**
	 * Setter for <code>public.employees.empl_pk</code>.
	 */
	public void setEmplPk(java.lang.String value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>public.employees.empl_pk</code>.
	 */
	public java.lang.String getEmplPk() {
		return (java.lang.String) getValue(0);
	}

	/**
	 * Setter for <code>public.employees.empl_firstname</code>.
	 */
	public void setEmplFirstname(java.lang.String value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>public.employees.empl_firstname</code>.
	 */
	public java.lang.String getEmplFirstname() {
		return (java.lang.String) getValue(1);
	}

	/**
	 * Setter for <code>public.employees.empl_surname</code>.
	 */
	public void setEmplSurname(java.lang.String value) {
		setValue(2, value);
	}

	/**
	 * Getter for <code>public.employees.empl_surname</code>.
	 */
	public java.lang.String getEmplSurname() {
		return (java.lang.String) getValue(2);
	}

	/**
	 * Setter for <code>public.employees.empl_dob</code>.
	 */
	public void setEmplDob(java.sql.Date value) {
		setValue(3, value);
	}

	/**
	 * Getter for <code>public.employees.empl_dob</code>.
	 */
	public java.sql.Date getEmplDob() {
		return (java.sql.Date) getValue(3);
	}

	/**
	 * Setter for <code>public.employees.empl_permanent</code>.
	 */
	public void setEmplPermanent(java.lang.Boolean value) {
		setValue(4, value);
	}

	/**
	 * Getter for <code>public.employees.empl_permanent</code>.
	 */
	public java.lang.Boolean getEmplPermanent() {
		return (java.lang.Boolean) getValue(4);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Record1<java.lang.String> key() {
		return (org.jooq.Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record5 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row5<java.lang.String, java.lang.String, java.lang.String, java.sql.Date, java.lang.Boolean> fieldsRow() {
		return (org.jooq.Row5) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Row5<java.lang.String, java.lang.String, java.lang.String, java.sql.Date, java.lang.Boolean> valuesRow() {
		return (org.jooq.Row5) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field1() {
		return asdf.tables.Employees.EMPLOYEES.EMPL_PK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field2() {
		return asdf.tables.Employees.EMPLOYEES.EMPL_FIRSTNAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.String> field3() {
		return asdf.tables.Employees.EMPLOYEES.EMPL_SURNAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.sql.Date> field4() {
		return asdf.tables.Employees.EMPLOYEES.EMPL_DOB;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Field<java.lang.Boolean> field5() {
		return asdf.tables.Employees.EMPLOYEES.EMPL_PERMANENT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value1() {
		return getEmplPk();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value2() {
		return getEmplFirstname();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.String value3() {
		return getEmplSurname();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.sql.Date value4() {
		return getEmplDob();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.lang.Boolean value5() {
		return getEmplPermanent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord value1(java.lang.String value) {
		setEmplPk(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord value2(java.lang.String value) {
		setEmplFirstname(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord value3(java.lang.String value) {
		setEmplSurname(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord value4(java.sql.Date value) {
		setEmplDob(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord value5(java.lang.Boolean value) {
		setEmplPermanent(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EmployeesRecord values(java.lang.String value1, java.lang.String value2, java.lang.String value3, java.sql.Date value4, java.lang.Boolean value5) {
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached EmployeesRecord
	 */
	public EmployeesRecord() {
		super(asdf.tables.Employees.EMPLOYEES);
	}

	/**
	 * Create a detached, initialised EmployeesRecord
	 */
	public EmployeesRecord(java.lang.String emplPk, java.lang.String emplFirstname, java.lang.String emplSurname, java.sql.Date emplDob, java.lang.Boolean emplPermanent) {
		super(asdf.tables.Employees.EMPLOYEES);

		setValue(0, emplPk);
		setValue(1, emplFirstname);
		setValue(2, emplSurname);
		setValue(3, emplDob);
		setValue(4, emplPermanent);
	}
}
