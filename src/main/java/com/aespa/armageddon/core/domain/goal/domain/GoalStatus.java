package com.aespa.armageddon.core.domain.goal.domain;

public enum GoalStatus {
    ACTIVE, // 진행 중
    COMPLETED, // 달성 완료
    FAILED, // 달성 실패 (기간 만료)
    DELETED // 삭제 (Soft Delete)
}