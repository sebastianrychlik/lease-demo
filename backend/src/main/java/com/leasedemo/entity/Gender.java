package com.leasedemo.entity;

/**
 * Biological sex as encoded in a Polish PESEL number.
 *
 * <p>The 10th digit of a PESEL is even for females and odd for males. This
 * enum is used both as the persisted {@link Customer#gender} attribute and
 * as the value cross-checked against the digit extracted from the PESEL by
 * {@link com.leasedemo.util.PeselValidator}.
 */
public enum Gender {
    MALE,
    FEMALE
}
