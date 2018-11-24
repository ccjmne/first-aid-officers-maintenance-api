package org.ccjmne.orca.api.rest.fetch;

import static org.ccjmne.orca.jooq.classes.Tables.CERTIFICATES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES;
import static org.ccjmne.orca.jooq.classes.Tables.SITES_TAGS;
import static org.ccjmne.orca.jooq.classes.Tables.TAGS;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGTYPES;
import static org.ccjmne.orca.jooq.classes.Tables.TRAININGTYPES_CERTIFICATES;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.ccjmne.orca.api.utils.Constants;
import org.ccjmne.orca.api.utils.ResourcesHelper;
import org.ccjmne.orca.api.utils.ResourcesSelection;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Serves the resources whose access isn't restricted.<br />
 * Presents resources into {@link Map}s keyed by their unique identifier.
 *
 * @author ccjmne
 */
@Path("sub-resources")
public class SubResources {

  private final DSLContext         ctx;
  private final ResourcesSelection resourcesSelection;

  @Inject
  private SubResources(final DSLContext ctx, final ResourcesSelection resourcesSelection) {
    this.ctx = ctx;
    this.resourcesSelection = resourcesSelection;
  }

  @GET
  @Path("certificates")
  public Map<Integer, ? extends Record> getCertificatesMap() {
    return this.ctx.selectFrom(CERTIFICATES).fetchMap(CERTIFICATES.CERT_PK);
  }

  @GET
  @Path("session-types")
  public Map<Integer, ? extends Record> getSessionTypesMap() {
    return this.ctx
        .select(TRAININGTYPES.fields())
        .select(ResourcesHelper.jsonbObjectAggNullSafe(TRAININGTYPES_CERTIFICATES.TTCE_CERT_FK, TRAININGTYPES_CERTIFICATES.TTCE_DURATION).as("certificates"))
        .from(TRAININGTYPES)
        .join(TRAININGTYPES_CERTIFICATES).on(TRAININGTYPES_CERTIFICATES.TTCE_TRTY_FK.eq(TRAININGTYPES.TRTY_PK))
        .groupBy(TRAININGTYPES.fields())
        .fetchMap(TRAININGTYPES.TRTY_PK);
  }

  @GET
  @Path("tags")
  public Map<Integer, ? extends Record> getTagsMap() {
    final Table<Record3<Integer, String, Integer>> stats = DSL
        .select(SITES_TAGS.SITA_TAGS_FK, SITES_TAGS.SITA_VALUE, DSL.count(SITES_TAGS.SITA_VALUE))
        .from(SITES_TAGS)
        .where(SITES_TAGS.SITA_SITE_FK.in(Constants.select(SITES.SITE_PK, this.resourcesSelection.selectSites())))
        .groupBy(SITES_TAGS.SITA_TAGS_FK, SITES_TAGS.SITA_VALUE)
        .asTable();
    return this.ctx
        .select(TAGS.fields())
        .select(ResourcesHelper.jsonbObjectAggNullSafe(stats.field(1), stats.field(2)).as("tags_values_counts"))
        .select(ResourcesHelper.jsonbArrayAggOmitNull(ResourcesHelper.TAG_VALUE_COERCED).as("tags_values"))
        .from(TAGS)
        .leftOuterJoin(stats).on(stats.field(SITES_TAGS.SITA_TAGS_FK).eq(TAGS.TAGS_PK))
        .groupBy(TAGS.fields())
        .fetchMap(TAGS.TAGS_PK);
  }
}
