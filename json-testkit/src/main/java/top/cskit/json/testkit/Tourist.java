package top.cskit.json.testkit;

import java.util.Date;

/**
 * Shared test entity: a tourist, demonstrating date-format customization.
 *
 * <p>Reused by scenarios such as the Gson custom-constructor test (date format).
 */
public class Tourist {

    private String name;
    private Date birthday;

    public Tourist() {
    }

    public Tourist(String name, Date birthday) {
        this.name = name;
        this.birthday = birthday;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
