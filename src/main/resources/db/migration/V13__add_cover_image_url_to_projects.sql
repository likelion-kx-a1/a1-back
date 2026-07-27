-- 프로젝트 커버 이미지: 사이드바 "표지 수정" 메뉴가 지금까지 UI만 있고 실제로 반영할 필드가
-- 없었다(docs/backend-tasks.md #3). 채팅 첨부파일과 동일한 S3 스토리지에 업로드한 뒤 그 공개 URL을
-- 저장하는 용도라 별도 참조 테이블 없이 projects에 URL 컬럼 하나만 추가한다.
ALTER TABLE projects
    ADD COLUMN cover_image_url VARCHAR(2048);
