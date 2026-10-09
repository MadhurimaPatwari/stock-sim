package com.stocksim.currency;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps the {@code currencies} reference table (V2__create_currencies.sql). Managed by migrations, read-only here. */
@Entity
@Immutable
@Table(name = "currencies")
public class Currency {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "minor_units", nullable = false)
    private short minorUnits;

    protected Currency() {
        // for JPA
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public short getMinorUnits() { return minorUnits; }
}
