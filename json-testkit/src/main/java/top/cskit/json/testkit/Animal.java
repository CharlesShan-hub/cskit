package top.cskit.json.testkit;

/**
 * Shared test entity: an animal.
 *
 * <p>Requirements common to all three backends: public no-arg constructor
 * plus getters/setters (fastjson2 relies on them; Gson reads fields via
 * reflection). Reused by every adapter module's tests to avoid duplication.
 */
public class Animal {

    private String name;
    private int birthYear;

    public Animal() {
    }

    public Animal(String name, int birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }
}
