package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectWriter;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.collect.ImmutableMap;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** JsonSerializer of {@link PageView} */
public class PageViewSerializer extends JsonSerializer<PageView> {

  @Override
  public void serialize(
      final PageView pageView,
      final JsonGenerator jsonGenerator,
      final SerializerProvider serializerProvider)
      throws IOException, JsonProcessingException {
    final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
    final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    builder.putAll(getObjectMap(pageView));
    final String json = objectWriter.writeValueAsString(builder.build());
    jsonGenerator.writeRawValue(json);
  }

  protected Map<String, Object> getObjectMap(final PageView pageView) {
    final Template template = pageView.getTemplate();

    final Map<String, Object> pageViewMap = new TreeMap<>();
    pageViewMap.put("page", this.asMap(pageView.getPageInfo()));
    pageViewMap.put("containers", getContainerRawAsMap(pageView.getContainers()));

    final Map<Object, Object> templateMap = this.asMap(template);
    templateMap.put("canEdit", pageView.canEditTemplate());
    pageViewMap.put("template", templateMap);

    pageViewMap.put("site", pageView.getSite());
    pageViewMap.put("viewAs", pageView.getViewAs());
    pageViewMap.put("canCreateTemplate", pageView.canCreateTemplate());
    ;
    if (pageView.getLayout() != null) {
      pageViewMap.put("layout", pageView.getLayout());
    }
    return pageViewMap;
  }

  private Map<String, ContainerRaw> getContainerRawAsMap(
      final Collection<? extends ContainerRaw> containerRaws) {

    final Map<String, ContainerRaw> containerRawMap = new HashMap<>();

    containerRaws
        .stream()
        .forEach(
            containerRaw -> {
              if (containerRaw.getContainer() instanceof FileAssetContainer) {

                final String path =
                    FileAssetContainer.class.cast(containerRaw.getContainer()).getPath();
                containerRawMap.put(path, containerRaw);
              }

              final String identifier = containerRaw.getContainer().getIdentifier();
              containerRawMap.put(identifier, containerRaw);
            });

    return containerRawMap;
  }

  private Map<Object, Object> asMap(final Object object) {
    final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

    try {
      final String json = objectWriter.writeValueAsString(object);
      final Map map =
          JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);
      map.values().removeIf(Objects::isNull);
      return map;
    } catch (IOException e) {
      throw new DotRuntimeException(e);
    }
  }
}
