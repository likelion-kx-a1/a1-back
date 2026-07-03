package com.likelion.a1.media.application.port.out;

public interface MediaStoragePort {
  String store(byte[] content, String contentType, String extension);
}
