package com.phuong.chain_of_responsibility;

public class ManagerPPower extends PurchasePower {
    protected double getAllowable() {
        return BASE * 10;
    }

    protected String getRole() {
        return "Manager";
    }
}