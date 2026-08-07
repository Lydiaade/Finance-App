package com.dto;

import jakarta.persistence.*;

import java.util.Objects;

// FM-19: represents an ongoing, user-established classification rule - "transactions paid to X
// should default to segment Y going forward" - established whenever a user edits a transaction's
// segment (TransactionService.updateTransactionSegment). Consulted by CSVHelper on import and by
// TransactionService.addManualTransaction when no explicit segment is supplied. At most one row
// per paid_to (upsert semantics enforced in the service layer, not a DB constraint - see AC doc
// Flag F5, left open rather than unilaterally added under ddl-auto: update).
@Entity
@Table(name = "payee_segment_rules")
public class PayeeSegmentRule {

    @Id
    @SequenceGenerator(
            name = "payee_segment_rule_id_sequence",
            sequenceName = "payee_segment_rule_id_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "payee_segment_rule_id_sequence"
    )
    private int id;

    private String paid_to;
    private String segment;

    public PayeeSegmentRule() {
    }

    public PayeeSegmentRule(String paid_to, String segment) {
        this.paid_to = paid_to;
        this.segment = segment;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPaid_to() {
        return paid_to;
    }

    public void setPaid_to(String paid_to) {
        this.paid_to = paid_to;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayeeSegmentRule that = (PayeeSegmentRule) o;
        return Objects.equals(id, that.id) && Objects.equals(paid_to, that.paid_to) && Objects.equals(segment, that.segment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, paid_to, segment);
    }

    @Override
    public String toString() {
        return "PayeeSegmentRule{" +
                "id=" + id +
                ", paid_to='" + paid_to + '\'' +
                ", segment='" + segment + '\'' +
                '}';
    }
}
