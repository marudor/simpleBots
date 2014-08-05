package de.marudor.simpleBots.account;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Created by marudor on 28/07/14.
 */
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
public class Person {
    @NotNull
    @XmlElement
    private String firstName;
    @NotNull
    @XmlElement
    private String lastName;
    @ManyToOne(fetch = FetchType.LAZY)
    @XmlElement
    private Country country;

    public Person(String firstName, String lastName, Country country) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
    }

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Person() {
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Country getCountry() {
        return country;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
