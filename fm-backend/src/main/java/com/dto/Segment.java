package com.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
public class Segment {

    @Id
    @SequenceGenerator(
            name = "segment_id_sequence",
            sequenceName = "segment_id_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "segment_id_sequence"
    )
    private int id;

    private String name;

    public Segment(String name) {
        this.name = name;
    }

    public Segment() {

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Segment segment = (Segment) o;
        return Objects.equals(id, segment.id) && Objects.equals(name, segment.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Segment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}


