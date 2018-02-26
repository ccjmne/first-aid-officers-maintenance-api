package org.ccjmne.orca.api.rest;

import static org.ccjmne.orca.jooq.classes.Tables.TAGS;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.ccjmne.orca.api.modules.Restrictions;
import org.ccjmne.orca.jooq.classes.Sequences;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

@Path("tags")
public class TagsEndpoint {

	private final DSLContext ctx;

	@Inject
	public TagsEndpoint(final Restrictions restrictions, final DSLContext ctx) {
		if (!restrictions.canManageSitesAndTags()) {
			throw new ForbiddenException();
		}

		this.ctx = ctx;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Integer createTag(final Map<String, String> tagDefinition) {
		final Integer tags_pk = new Integer(this.ctx.nextval(Sequences.TAGS_TAGS_PK_SEQ).intValue());
		updateTag(tags_pk, tagDefinition);
		return tags_pk;
	}

	@PUT
	@Path("{tags_pk}")
	@Consumes(MediaType.APPLICATION_JSON)
	public boolean updateTag(@PathParam("tags_pk") final Integer tags_pk, final Map<String, String> tagDefinition) {
		final boolean exists = this.ctx.fetchExists(TAGS, TAGS.TAGS_PK.eq(tags_pk));
		this.ctx.transaction((config) -> {
			try (final DSLContext transactionCtx = DSL.using(config)) {
				if (exists) {
					transactionCtx.update(TAGS).set(tagDefinition).where(TAGS.TAGS_PK.eq(tags_pk)).execute();
				} else {
					transactionCtx.insertInto(TAGS, TAGS.TAGS_PK, TAGS.TAGS_NAME, TAGS.TAGS_SHORT, TAGS.TAGS_TYPE)
							.values(tags_pk,
									tagDefinition.get(TAGS.TAGS_NAME.getName()),
									tagDefinition.get(TAGS.TAGS_SHORT.getName()),
									tagDefinition.get(TAGS.TAGS_TYPE.getName()))
							.execute();
				}
			}
		});

		return exists;
	}

	@DELETE
	@Path("{tags_pk}")
	public void deleteTag(@PathParam("tags_pk") final Integer tags_pk) {
		this.ctx.delete(TAGS).where(TAGS.TAGS_PK.eq(tags_pk)).execute();
	}
}