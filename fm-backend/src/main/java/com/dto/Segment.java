package com.dto;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
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
    private Integer id;

    private String name;

    public Segment(String name) {
        this.name = name;
    }

    public Segment() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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


