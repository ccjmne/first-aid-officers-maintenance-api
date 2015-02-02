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
public class Trainings extends org.jooq.impl.TableImpl<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord> {

	private static final long serialVersionUID = -1966839682;

	/**
	 * The reference instance of <code>public.trainings</code>
	 */
	public static final org.ccjmne.faomaintenance.api.jooq.tables.Trainings TRAININGS = new org.ccjmne.faomaintenance.api.jooq.tables.Trainings();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord> getRecordType() {
		return org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord.class;
	}

	/**
	 * The column <code>public.trainings.trng_pk</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, java.lang.Integer> TRNG_PK = createField("trng_pk", org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>public.trainings.trng_date</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, java.sql.Date> TRNG_DATE = createField("trng_date", org.jooq.impl.SQLDataType.DATE.nullable(false), this, "");

	/**
	 * The column <code>public.trainings.trng_trty_fk</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, java.lang.Integer> TRNG_TRTY_FK = createField("trng_trty_fk", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>public.trainings.trng_outcome</code>.
	 */
	public final org.jooq.TableField<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, java.lang.String> TRNG_OUTCOME = createField("trng_outcome", org.jooq.impl.SQLDataType.VARCHAR.length(32).nullable(false).defaulted(true), this, "");

	/**
	 * Create a <code>public.trainings</code> table reference
	 */
	public Trainings() {
		this("trainings", null);
	}

	/**
	 * Create an aliased <code>public.trainings</code> table reference
	 */
	public Trainings(java.lang.String alias) {
		this(alias, org.ccjmne.faomaintenance.api.jooq.tables.Trainings.TRAININGS);
	}

	private Trainings(java.lang.String alias, org.jooq.Table<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord> aliased) {
		this(alias, aliased, null);
	}

	private Trainings(java.lang.String alias, org.jooq.Table<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, org.ccjmne.faomaintenance.api.jooq.Public.PUBLIC, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, java.lang.Integer> getIdentity() {
		return org.ccjmne.faomaintenance.api.jooq.Keys.IDENTITY_TRAININGS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord> getPrimaryKey() {
		return org.ccjmne.faomaintenance.api.jooq.Keys.TRNG_PK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord>>asList(org.ccjmne.faomaintenance.api.jooq.Keys.TRNG_PK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.ForeignKey<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, ?>> getReferences() {
		return java.util.Arrays.<org.jooq.ForeignKey<org.ccjmne.faomaintenance.api.jooq.tables.records.TrainingsRecord, ?>>asList(org.ccjmne.faomaintenance.api.jooq.Keys.TRAININGS__TRNG_TRTY_FK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.ccjmne.faomaintenance.api.jooq.tables.Trainings as(java.lang.String alias) {
		return new org.ccjmne.faomaintenance.api.jooq.tables.Trainings(alias, this);
	}

	/**
	 * Rename this table
	 */
	public org.ccjmne.faomaintenance.api.jooq.tables.Trainings rename(java.lang.String name) {
		return new org.ccjmne.faomaintenance.api.jooq.tables.Trainings(name, null);
	}
}
