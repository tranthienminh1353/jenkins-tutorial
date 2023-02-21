package com.phuong.chain_of_responsibility;

public class DirectorPPower extends PurchasePower {
    protected double getAllowable() {
        return BASE * 20;
    }

    protected String getRole() {
        return "Director";
    }
}
