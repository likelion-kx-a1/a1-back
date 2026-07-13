package com.likelion.a1.media.application.port.out;

public interface MediaStoragePort {
  String store(byte[] content, String contentType, String extension);

  /**
   * 외부 API(fal.ai 등)가 반환한 임시 URL의 미디어를 메모리에 통째로 버퍼링하지 않고 스트림으로
   * 열어 바로 저장소로 업로드한다. 대용량 비디오 파일을 다룰 때 힙 메모리 사용을 최소화한다.
   */
  String storeFromUrl(String temporaryUrl, String contentType, String extension);
}
