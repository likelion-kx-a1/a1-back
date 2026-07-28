package com.likelion.a1.project.application.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.likelion.a1.chat.domain.model.Chat;
import com.likelion.a1.chat.domain.repository.ChatRepository;
import com.likelion.a1.library.application.service.MyLibraryService;
import com.likelion.a1.project.domain.model.Project;
import com.likelion.a1.project.domain.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
  private static final Long USER_ID = 7L;
  private static final Long PROJECT_ID = 112L;

  @Mock private ProjectRepository projectRepository;
  @Mock private ChatRepository chatRepository;
  @Mock private MyLibraryService myLibraryService;
  @Mock private Project project;
  @Mock private Chat firstChat;
  @Mock private Chat secondChat;

  private ProjectService projectService;

  @BeforeEach
  void setUp() {
    projectService = new ProjectService(projectRepository, chatRepository, myLibraryService);
    when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    when(project.isDeleted()).thenReturn(false);
    when(project.isOwnedBy(USER_ID)).thenReturn(true);
    when(chatRepository.findActiveByUserIdAndProjectId(USER_ID, PROJECT_ID))
        .thenReturn(List.of(firstChat, secondChat));
  }

  @Test
  void 채팅만_삭제하면_프로젝트와_연결_보관함을_유지한다() {
    projectService.delete(USER_ID, PROJECT_ID);

    verify(firstChat).delete();
    verify(secondChat).delete();
    verify(chatRepository).save(firstChat);
    verify(chatRepository).save(secondChat);
    verify(project, never()).delete();
    verify(projectRepository, never()).save(project);
    verifyNoInteractions(myLibraryService);
  }

  @Test
  void 미디어_포함_삭제하면_채팅과_보관함과_프로젝트를_삭제한다() {
    projectService.deleteWithLibrary(USER_ID, PROJECT_ID);

    verify(firstChat).delete();
    verify(secondChat).delete();
    verify(myLibraryService).deleteLinkedLibraryProject(USER_ID, PROJECT_ID);
    verify(project).delete();
    verify(projectRepository).save(project);
  }
}
