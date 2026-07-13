package com.likelion.a1.media.application.port.out;

public interface MediaStoragePort {
  StorageUploadResult upload(StorageUploadCommand command);
}
