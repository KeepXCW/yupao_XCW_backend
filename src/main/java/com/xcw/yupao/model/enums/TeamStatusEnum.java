package com.xcw.yupao.model.enums;

/**
 * 队伍状态枚举类
 * 枚举类不能用@Data注解
 * @author xcw
 */
public enum TeamStatusEnum {
    /**
     * 公开
     */
    PUBLIC(0,"公开"),
    /**
     * 私有
     */
    PRIVATE(1,"私有"),
    /**
     * 加密
     */
    SECRET(2,"加密");


    /**
     * 队伍状态值
     */
    private int value;

    /**
     * 队伍状态描述
     */
    private String description;

    //判断传入的value（队伍status）是否是在0，1，2范围中
    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue()==value) {
                return teamStatusEnum;
            }
        }
        return null;
    }


    TeamStatusEnum(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
