package top.cskit.json.testkit;

import java.util.Date;

/**
 * 共享测试实体：游客（演示日期格式化定制）。
 * <p>
 * 供 Gson 定制构造测试（日期格式）等场景复用。
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
