-- password_reset_tokens는 어떤 리포지토리/서비스/컨트롤러에서도 사용되지 않는 고아 테이블이다.
-- 실제 비밀번호 재설정 흐름은 email_verifications(purpose='PASSWORD_RESET')를 재사용해서
-- 구현되어 있다(PasswordResetService 참고). docs_h/보안_취약점_점검.md #7.
DROP TABLE password_reset_tokens;
