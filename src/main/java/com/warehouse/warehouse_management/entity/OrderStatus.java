package com.warehouse.warehouse_management.entity;

public enum Status {
    CREATED,
    AWAITING_APPROVAL,
    APPROVED,
    DECLINED,
    UNDER_DELIVERY,
    FULFILLED,
    CANCELED;

    public boolean canEdit(){
        return this == CREATED || this == DECLINED;
    }
    public boolean canSubmit(){
        return this == CREATED || this == DECLINED;
    }
    public boolean canCancel(){
        return this != FULFILLED || this != UNDER_DELIVERY || this != CANCELED;
    }
}
