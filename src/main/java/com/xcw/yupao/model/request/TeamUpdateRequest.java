package com.xcw.yupao.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *修改队伍信息请求体（信息脱敏）
 * @author xcw
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = -1368733263240073830L;

    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     * 不是设置的队伍最大人数标准，而是当前队伍人数
     */
    //private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}
