package top.cskit.json.testkit;

/**
 * 共享测试实体：动物（动物园居民）。
 * <p>
 * 三库通用要求：public 无参构造 + getter/setter（fastjson2 依赖 getter/setter，
 * Gson 反射读字段）。供各适配器模块测试复用，避免重复定义。
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
