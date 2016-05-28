package org.ccjmne.faomaintenance.api.rest;

import static org.ccjmne.faomaintenance.jooq.classes.Tables.CERTIFICATES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.DEPARTMENTS;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.EMPLOYEES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.EMPLOYEES_ROLES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.SITES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.SITES_EMPLOYEES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.TRAININGTYPES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.TRAININGTYPES_CERTIFICATES;
import static org.ccjmne.faomaintenance.jooq.classes.Tables.UPDATES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ccjmne.faomaintenance.api.utils.SQLDateFormat;
import org.ccjmne.faomaintenance.jooq.classes.Sequences;
import org.ccjmne.faomaintenance.jooq.classes.tables.records.SitesEmployeesRecord;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.Row1;
import org.jooq.Row2;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.tools.csv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("update")
public class UpdateEndpoint {

	private static final Pattern FIRST_LETTER = Pattern.compile("\\b(\\w)");
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateEndpoint.class);
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

	private final DSLContext ctx;
	private final SQLDateFormat dateFormat;
	private final StatisticsEndpoint statistics;

	@Inject
	public UpdateEndpoint(final DSLContext ctx, final SQLDateFormat dateFormat, final StatisticsEndpoint statistics, final ResourcesEndpoint resources) {
		this.dateFormat = dateFormat;
		this.ctx = ctx;
		this.statistics = statistics;
	}

	@PUT
	@Path("departments/{dept_pk}")
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateDepartment(@PathParam("dept_pk") final Integer dept_pk, final Map<String, String> dept) {
		if (this.ctx.fetchExists(DEPARTMENTS, DEPARTMENTS.DEPT_PK.eq(dept_pk))) {
			this.ctx.update(DEPARTMENTS)
					.set(DEPARTMENTS.DEPT_NAME, dept.get(DEPARTMENTS.DEPT_NAME.getName()))
					.set(DEPARTMENTS.DEPT_ID, dept.get(DEPARTMENTS.DEPT_ID.getName()))
					.where(DEPARTMENTS.DEPT_PK.eq(dept_pk)).execute();
			return false;
		}

		this.ctx.insertInto(DEPARTMENTS, DEPARTMENTS.DEPT_PK, DEPARTMENTS.DEPT_NAME, DEPARTMENTS.DEPT_ID)
				.values(dept_pk, dept.get(DEPARTMENTS.DEPT_NAME.getName()), dept.get(DEPARTMENTS.DEPT_ID.getName())).execute();
		return true;
	}

	@PUT
	@Path("sites/{site_pk}")
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateSite(@PathParam("site_pk") final String site_pk, final Map<String, String> site) {
		if (this.ctx.fetchExists(SITES, SITES.SITE_PK.eq(site_pk))) {
			this.ctx.update(SITES)
					.set(SITES.SITE_PK, site.getOrDefault(SITES.SITE_PK.getName(), site_pk))
					.set(SITES.SITE_NAME, site.get(SITES.SITE_NAME.getName()))
					.set(SITES.SITE_DEPT_FK, Integer.valueOf(site.get(SITES.SITE_DEPT_FK.getName())))
					.set(SITES.SITE_NOTES, site.getOrDefault(SITES.SITE_NOTES.getName(), site_pk))
					.where(SITES.SITE_PK.eq(site_pk)).execute();
			return false;
		}

		this.ctx.insertInto(SITES, SITES.SITE_PK, SITES.SITE_NAME, SITES.SITE_DEPT_FK, SITES.SITE_NOTES)
				.values(
						site_pk,
						site.get(SITES.SITE_NAME.getName()),
						Integer.valueOf(site.get(SITES.SITE_DEPT_FK.getName())),
						site.get(SITES.SITE_NOTES.getName()))
				.execute();
		return true;
	}

	@POST
	@Path("certificates")
	@Consumes(MediaType.APPLICATION_JSON)
	public Integer createCert(final Map<String, String> cert) {
		final Integer cert_pk = new Integer(this.ctx.nextval(Sequences.CERTIFICATES_CERT_PK_SEQ).intValue());
		updateCert(cert_pk, cert);
		return cert_pk;
	}

	@PUT
	@Path("certificates/{cert_pk}")
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateCert(@PathParam("cert_pk") final Integer cert_pk, final Map<String, String> cert) {
		final boolean exists = this.ctx.fetchExists(CERTIFICATES, CERTIFICATES.CERT_PK.eq(cert_pk));
		this.ctx.transaction((config) -> {
			try (final DSLContext transactionCtx = DSL.using(config)) {
				if (exists) {
					transactionCtx.update(CERTIFICATES)
							.set(CERTIFICATES.CERT_NAME, cert.get(CERTIFICATES.CERT_NAME.getName()))
							.set(CERTIFICATES.CERT_SHORT, cert.get(CERTIFICATES.CERT_SHORT.getName()))
							.set(CERTIFICATES.CERT_TARGET, Integer.valueOf(cert.get(CERTIFICATES.CERT_TARGET.getName())))
							.set(CERTIFICATES.CERT_PERMANENTONLY, Boolean.valueOf(cert.get(CERTIFICATES.CERT_PERMANENTONLY.getName())))
							.where(CERTIFICATES.CERT_PK.eq(cert_pk)).execute();
				} else {
					final Integer nextOrder = Integer.valueOf(transactionCtx.selectCount().from(TRAININGTYPES).fetchOne(0, Integer.class).intValue() + 1);
					transactionCtx.insertInto(
												CERTIFICATES,
												CERTIFICATES.CERT_PK,
												CERTIFICATES.CERT_NAME,
												CERTIFICATES.CERT_SHORT,
												CERTIFICATES.CERT_TARGET,
												CERTIFICATES.CERT_PERMANENTONLY,
												CERTIFICATES.CERT_ORDER)
							.values(
									cert_pk,
									cert.get(CERTIFICATES.CERT_NAME.getName()),
									cert.get(CERTIFICATES.CERT_SHORT.getName()),
									Integer.valueOf(cert.get(CERTIFICATES.CERT_TARGET.getName())),
									Boolean.valueOf(cert.get(CERTIFICATES.CERT_PERMANENTONLY.getName())),
									nextOrder)
							.execute();
				}

				this.statistics.refreshCertificates();
				this.statistics.invalidateEmployeesStats();
				this.statistics.invalidateSitesStats();
			}
		});

		return exists;
	}

	@POST
	@Path("trainingtypes")
	@Consumes(MediaType.APPLICATION_JSON)
	public Integer createTrty(final Map<String, Object> trty) {
		final Integer trty_pk = new Integer(this.ctx.nextval(Sequences.TRAININGTYPES_TRTY_PK_SEQ).intValue());
		updateTrty(trty_pk, trty);
		return trty_pk;
	}

	@SuppressWarnings("unchecked")
	@PUT
	@Path("trainingtypes/{trty_pk}")
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateTrty(@PathParam("trty_pk") final Integer trty_pk, final Map<String, Object> trty) {
		final boolean exists = this.ctx.fetchExists(TRAININGTYPES, TRAININGTYPES.TRTY_PK.eq(trty_pk));
		this.ctx.transaction((config) -> {
			try (final DSLContext transactionCtx = DSL.using(config)) {
				if (exists) {
					transactionCtx.update(TRAININGTYPES)
							.set(TRAININGTYPES.TRTY_PK, trty_pk)
							.set(TRAININGTYPES.TRTY_NAME, trty.get(TRAININGTYPES.TRTY_NAME.getName()).toString())
							.set(TRAININGTYPES.TRTY_VALIDITY, Integer.valueOf(trty.get(TRAININGTYPES.TRTY_VALIDITY.getName()).toString()))
							.where(TRAININGTYPES.TRTY_PK.eq(trty_pk)).execute();
				} else {
					final Integer nextOrder = Integer.valueOf(transactionCtx.selectCount().from(TRAININGTYPES).fetchOne(0, Integer.class).intValue() + 1);
					transactionCtx.insertInto(
												TRAININGTYPES,
												TRAININGTYPES.TRTY_PK,
												TRAININGTYPES.TRTY_NAME,
												TRAININGTYPES.TRTY_VALIDITY,
												TRAININGTYPES.TRTY_ORDER)
							.values(
									trty_pk,
									trty.get(TRAININGTYPES.TRTY_NAME.getName()).toString(),
									(Integer) trty.get(TRAININGTYPES.TRTY_VALIDITY.getName()),
									nextOrder)
							.execute();
				}

				transactionCtx.delete(TRAININGTYPES_CERTIFICATES).where(TRAININGTYPES_CERTIFICATES.TTCE_TRTY_FK.eq(trty_pk)).execute();
				final Row1<Integer>[] certificates = ((List<Integer>) trty.get("certificates")).stream().map(DSL::row).toArray(Row1[]::new);
				if (certificates.length > 0) {
					transactionCtx.insertInto(
												TRAININGTYPES_CERTIFICATES,
												TRAININGTYPES_CERTIFICATES.TTCE_TRTY_FK,
												TRAININGTYPES_CERTIFICATES.TTCE_CERT_FK)
							.select(DSL.select(
												DSL.val(trty_pk),
												DSL.field("cert_pk", Integer.class))
									.from(DSL.values(certificates).as("unused", "cert_pk")))
							.execute();
				}

				this.statistics.refreshCertificates();
				this.statistics.invalidateEmployeesStats();
				this.statistics.invalidateSitesStats();
			}
		});

		return exists;
	}

	@POST
	@Path("certificates/reorder")
	@SuppressWarnings("unchecked")
	public void reassignCertificates(final Map<Integer, Integer> reassignmentMap) {
		if (reassignmentMap.isEmpty()) {
			return;
		}

		this.ctx.update(CERTIFICATES)
				.set(
						CERTIFICATES.CERT_ORDER,
						DSL.field("new_order", Integer.class))
				.from(DSL.values(reassignmentMap.entrySet().stream().map((entry) -> DSL.row(entry.getKey(), entry.getValue())).toArray(Row2[]::new))
						.as("unused", "pk", "new_order"))
				.where(CERTIFICATES.CERT_PK.eq(DSL.field("pk", Integer.class)))
				.execute();

		this.statistics.refreshCertificates();
	}

	@POST
	@Path("trainingtypes/reorder")
	@SuppressWarnings("unchecked")
	public void reassignTrainingTypes(final Map<Integer, Integer> reassignmentMap) {
		if (reassignmentMap.isEmpty()) {
			return;
		}

		this.ctx.update(TRAININGTYPES)
				.set(
						TRAININGTYPES.TRTY_ORDER,
						DSL.field("new_order", Integer.class))
				.from(DSL.values(reassignmentMap.entrySet().stream().map((entry) -> DSL.row(entry.getKey(), entry.getValue())).toArray(Row2[]::new))
						.as("unused", "pk", "new_order"))
				.where(TRAININGTYPES.TRTY_PK.eq(DSL.field("pk", Integer.class)))
				.execute();

		this.statistics.refreshCertificates();
	}

	@DELETE
	@Path("certificates/{cert_pk}")
	public boolean deleteCert(@PathParam("cert_pk") final Integer cert_pk) {
		return this.ctx.delete(CERTIFICATES).where(CERTIFICATES.CERT_PK.eq(cert_pk)).execute() == 1;
	}

	@DELETE
	@Path("trainingtypes/{trty_pk}")
	public boolean deleteTrty(@PathParam("trty_pk") final Integer trty_pk) {
		return this.ctx.delete(TRAININGTYPES).where(TRAININGTYPES.TRTY_PK.eq(trty_pk)).execute() == 1;
	}

	@DELETE
	@Path("departments/{dept_pk}")
	public boolean deleteDept(@PathParam("dept_pk") final Integer dept_pk) {
		return this.ctx.delete(DEPARTMENTS).where(DEPARTMENTS.DEPT_PK.eq(dept_pk)).execute() == 1;
	}

	@DELETE
	@Path("sites/{site_pk}")
	public boolean deleteSite(@PathParam("site_pk") final String site_pk) {
		final boolean exists = this.ctx.selectFrom(SITES).where(SITES.SITE_PK.equal(site_pk)).fetch().isNotEmpty();
		if (exists) {
			this.ctx.delete(SITES).where(SITES.SITE_PK.eq(site_pk)).execute();
			this.ctx.delete(SITES_EMPLOYEES).where(SITES_EMPLOYEES.SIEM_SITE_FK.eq(site_pk)).execute();
			this.statistics.invalidateSitesStats(Collections.singleton(site_pk));
		}

		return exists;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("parse")
	public Response parse(
							@QueryParam("pageNumber") final int pageNumber,
							@QueryParam("pageName") final String pageName,
							@FormDataParam("file") final InputStream file,
							@FormDataParam("file") final FormDataContentDisposition fileDisposition) {
		try {
			switch (fileDisposition.getFileName().substring(fileDisposition.getFileName().lastIndexOf(".") + 1)) {
				case "csv":
					try (final CSVReader reader = new CSVReader(new InputStreamReader(file))) {
						final List<String[]> list = reader.readAll();
						return Response.status(Status.OK).entity(list.toArray(new String[list.size()][])).build();
					}
				case "xls":
					try (final Workbook workbook = new HSSFWorkbook(file)) {
						return Response.status(Status.OK).entity(readSheet(workbook, pageNumber, pageName)).build();
					}
				case "xlsx":
					try (final Workbook workbook = new XSSFWorkbook(file)) {
						return Response.status(Status.OK).entity(readSheet(workbook, pageNumber, pageName)).build();
					}
				default:
					return Response.status(Status.BAD_REQUEST).entity("Uploaded file was neither a .xls nor a .xlsx file.").build();
			}
		} catch (final IOException e) {
			UpdateEndpoint.LOGGER.error(String.format("Could not parse file '%s'.", fileDisposition.getFileName()), e);
			return Response.status(Status.BAD_REQUEST).entity(String.format("Could not parse file '%s'.", fileDisposition.getFileName())).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response process(final List<Map<String, String>> employees) {
		try {
			this.ctx.transaction(config -> {
				try (final DSLContext transactionCtx = DSL.using(config)) {
					final Integer updt_pk = new Integer(transactionCtx.nextval(Sequences.UPDATES_UPDT_PK_SEQ).intValue());
					transactionCtx.insertInto(UPDATES).set(UPDATES.UPDT_PK, updt_pk).set(UPDATES.UPDT_DATE, new java.sql.Date(new Date().getTime())).execute();

					try (final InsertValuesStep3<SitesEmployeesRecord, Integer, String, String> query = transactionCtx
							.insertInto(SITES_EMPLOYEES, SITES_EMPLOYEES.SIEM_UPDT_FK, SITES_EMPLOYEES.SIEM_SITE_FK, SITES_EMPLOYEES.SIEM_EMPL_FK)) {
						for (final Map<String, String> employee : employees) {
							query.values(updt_pk, employee.get(SITES_EMPLOYEES.SIEM_SITE_FK.getName()), updateEmployee(employee, transactionCtx));
						}

						query.execute();
					}

					// Remove all privileges of the remaining employees
					transactionCtx
							.delete(EMPLOYEES_ROLES)
							.where(
									EMPLOYEES_ROLES.EMPL_PK.notIn(transactionCtx.select(SITES_EMPLOYEES.SIEM_EMPL_FK).from(SITES_EMPLOYEES)
											.where(SITES_EMPLOYEES.SIEM_UPDT_FK.eq(updt_pk))))
							.and(EMPLOYEES_ROLES.EMPL_PK.ne("admin"))
							.execute();

					// ... and set their site to #0 ('unassigned')
					transactionCtx.insertInto(SITES_EMPLOYEES, SITES_EMPLOYEES.SIEM_EMPL_FK, SITES_EMPLOYEES.SIEM_SITE_FK, SITES_EMPLOYEES.SIEM_UPDT_FK)
							.select(
									transactionCtx.select(
															EMPLOYEES.EMPL_PK,
															DSL.val("0"),
															DSL.val(updt_pk))
											.from(EMPLOYEES)
											.where(EMPLOYEES.EMPL_PK
													.notIn(transactionCtx.select(SITES_EMPLOYEES.SIEM_EMPL_FK).from(SITES_EMPLOYEES)
															.where(SITES_EMPLOYEES.SIEM_UPDT_FK.eq(updt_pk)))))
							.execute();
				}
			});
		} catch (final Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

		this.statistics.invalidateSitesStats();
		return Response.ok().build();
	}

	private static String capitalise(final String str) {
		final StringBuilder res = new StringBuilder(str.toLowerCase());
		final Matcher matcher = FIRST_LETTER.matcher(res);
		while (matcher.find()) {
			res.replace(matcher.start(), matcher.start() + 1, matcher.group().toUpperCase());
		}

		return res.toString();
	}

	private String updateEmployee(final Map<String, String> employee, final DSLContext context) throws ParseException {
		final String empl_pk = employee.get(EMPLOYEES.EMPL_PK.getName());
		final Map<TableField<?, ?>, Object> record = new HashMap<>();
		record.put(EMPLOYEES.EMPL_FIRSTNAME, capitalise(employee.get(EMPLOYEES.EMPL_FIRSTNAME.getName())));
		record.put(EMPLOYEES.EMPL_SURNAME, employee.get(EMPLOYEES.EMPL_SURNAME.getName()));
		record.put(EMPLOYEES.EMPL_DOB, this.dateFormat.parseSql(employee.get(EMPLOYEES.EMPL_DOB.getName())));
		record.put(EMPLOYEES.EMPL_PERMANENT, Boolean.valueOf("CDI".equalsIgnoreCase(employee.get(EMPLOYEES.EMPL_PERMANENT.getName()))));
		record.put(EMPLOYEES.EMPL_GENDER, Boolean.valueOf("Masculin".equalsIgnoreCase(employee.get(EMPLOYEES.EMPL_GENDER.getName()))));
		record.put(EMPLOYEES.EMPL_ADDR, employee.get(EMPLOYEES.EMPL_ADDR.getName()));

		if (context.fetchExists(EMPLOYEES, EMPLOYEES.EMPL_PK.eq(empl_pk))) {
			context.update(EMPLOYEES).set(record).where(EMPLOYEES.EMPL_PK.eq(empl_pk)).execute();
		} else {
			record.put(EMPLOYEES.EMPL_PK, empl_pk);
			context.insertInto(EMPLOYEES).set(record).execute();
		}

		return empl_pk;
	}

	private List<List<String>> readSheet(final Workbook workbook, final int pageNumber, final String pageName) {
		final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		final Sheet sheet = ((pageName != null) && !pageName.isEmpty()) ? workbook.getSheet(pageName) : workbook.getSheetAt(pageNumber);

		final List<List<String>> res = new ArrayList<>();
		final int lastColNum = sheet.getRow(sheet.getFirstRowNum()).getLastCellNum();
		for (final Row row : sheet) {
			final List<String> line = new ArrayList<>(lastColNum);
			for (int col = 0; col < lastColNum; col++) {
				line.add(getStringValue(row.getCell(col), evaluator));
			}

			if (!line.stream().allMatch(entry -> entry.isEmpty())) {
				res.add(line);
			}
		}

		return res;
	}

	private String getStringValue(final Cell cell, final FormulaEvaluator evaluator) {
		if (cell == null) {
			return "";
		}

		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return this.dateFormat.format(cell.getDateCellValue());
				}

				return DECIMAL_FORMAT.format(cell.getNumericCellValue());

			case Cell.CELL_TYPE_ERROR:
			case Cell.CELL_TYPE_BLANK:
				return "";

			case Cell.CELL_TYPE_FORMULA:
				return evaluator.evaluate(cell).getStringValue();

			default:
				return cell.getStringCellValue();
		}
	}
}
