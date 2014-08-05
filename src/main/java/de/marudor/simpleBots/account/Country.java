package de.marudor.simpleBots.account;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;

/**
 * Created by marudor on 28/07/14.
 */

@Entity
@XmlAccessorType(XmlAccessType.FIELD)
public class Country {
    @Id
    @NotNull
    @Size(min=3,max=3)
    @XmlElement
    private String countryCode;
    @NotNull
    @XmlElement
    private String name;
    @NotNull
    @XmlElement
    private String nativeName;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public Country() {
    }

    public Country(String countryCode, String name, String nativeName) {
        this.countryCode = countryCode;
        this.name = name;
        this.nativeName = nativeName;
    }

    @OneToMany(mappedBy = "country")
    private Collection<Person> persons;

    public Collection<Person> getPersons() {
        return persons;
    }

    public void setPersons(Collection<Person> persons) {
        this.persons = persons;
    }
}
