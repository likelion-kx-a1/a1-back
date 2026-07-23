-- 낙관적 락(JPA @Version): 수동 폴링(getStatus)과 백그라운드 스케줄러(GenerationVideoPollingScheduler)가
-- 같은 GenerationJob을 동시에 완료 처리할 때 마지막에 저장한 쪽이 먼저 커밋된 결과를 조용히 덮어쓰는 것을 막는다.
ALTER TABLE generation_jobs
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
