package com.likelion.a1.generation.infrastructure.client.fal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import tools.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FalGenerationAdapterImageMappingTest {

  @Test
  void referenceToVideoAlwaysUsesImageUrlsForOneOrMoreImages() {
    FalGenerationAdapter adapter =
        new FalGenerationAdapter(mock(ObjectMapper.class), "https://queue.fal.run", "key");

    assertMappedImages(adapter, "bytedance/seedance-2.0/reference-to-video", List.of("one"));
    assertMappedImages(
        adapter,
        "fal-ai/kling-video/o3/standard/reference-to-video",
        List.of("one", "two"));
  }

  @Test
  void gptImageAlwaysUsesMediumQuality() {
    FalGenerationAdapter adapter =
        new FalGenerationAdapter(mock(ObjectMapper.class), "https://queue.fal.run", "key");
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("prompt", "prompt");
    input.put("quality", "high");

    Map<String, Object> mapped =
        ReflectionTestUtils.invokeMethod(
            adapter, "mapImagesForFalPayload", "openai/gpt-image-2", input);

    assertThat(mapped).containsEntry("quality", "medium");
  }

  @Test
  void gptImageEditUsesImageUrlsAndMediumQuality() {
    FalGenerationAdapter adapter =
        new FalGenerationAdapter(mock(ObjectMapper.class), "https://queue.fal.run", "key");
    List<String> images = List.of("https://example.com/reference.png");
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("prompt", "prompt");
    input.put("images", images);

    Map<String, Object> mapped =
        ReflectionTestUtils.invokeMethod(
            adapter, "mapImagesForFalPayload", "openai/gpt-image-2/edit", input);

    assertThat(mapped)
        .containsEntry("quality", "medium")
        .containsEntry("image_urls", images)
        .doesNotContainKey("images");
  }

  @SuppressWarnings("unchecked")
  private void assertMappedImages(
      FalGenerationAdapter adapter, String modelCode, List<String> images) {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("prompt", "prompt");
    input.put("images", images);

    Map<String, Object> mapped =
        ReflectionTestUtils.invokeMethod(adapter, "mapImagesForFalPayload", modelCode, input);

    assertThat(mapped)
        .containsEntry("image_urls", images)
        .doesNotContainKeys("images", "image_url", "reference_images");
  }
}
