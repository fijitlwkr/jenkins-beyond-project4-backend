package com.aespa.armageddon.core.domain.goal.domain;

public enum GoalStatus {
    ACTIVE,     // 진행 중 (저축/지출 공통)

    COMPLETED,  // 저축 목표 달성
    FAILED,     // 저축 목표 실패 (기간 만료)

    SUCCESS,    // 지출 목표 성공 (기간 종료 + 예산 초과 X)
    EXCEEDED,   // 지출 목표 실패 (예산 초과)

    DELETED
}