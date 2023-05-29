package com.dto;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class Segment {

    @Id
    private String name;

    public Segment(String name) {
        this.name = name;
    }

    public Segment() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Segment segment = (Segment) o;
        return Objects.equals(name, segment.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Segment{" +
                "name='" + name + '\'' +
                '}';
    }
}


